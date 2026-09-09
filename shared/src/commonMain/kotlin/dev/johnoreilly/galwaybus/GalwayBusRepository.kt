package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GalwayBusRepository(
    injectedHttpClient: HttpClient? = null
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = injectedHttpClient ?: HttpClient {
        install(ContentNegotiation) { json(json) }
        expectSuccess = true
    }
    
    private val backendUrl = "https://galwaybus-68012425471.europe-west1.run.app"
    private val dublinTz = TimeZone.of("Europe/Dublin")
    private val cacheTtlMs = 30_000L

    // Static data (routes, stops, route detail) changes about weekly and is small, so it is fetched
    // once per launch and kept. It used to come from a GTFS snapshot bundled in the app; that copy
    // had no refresh path and drifted months out of date, so the backend is the source now.
    // Separate locks rather than one: each is held across its own network call, so a single mutex
    // meant a slow /shapes fetch blocked the stop list behind it (and vice versa).
    private val staticMutex = Mutex()
    private val routeDetailsMutex = Mutex()
    private val shapeMutex = Mutex()
    private var routesCache: Map<String, Route>? = null
    private var stopsCache: List<Stop>? = null
    private val routeDetailsCache = mutableMapOf<String, ApiRouteDetails>()
    private val shapeCache = mutableMapOf<String, List<List<Double>>>()

    // RT caches: epochMs timestamp paired with data
    private val vehiclesMutex = Mutex()
    private var vehiclesCache: Pair<Long, BusPositions>? = null

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun getRoutes(): Map<String, Route> = staticMutex.withLock {
        routesCache?.let { return it }
        retrying { httpClient.get("$backendUrl/routes.json").body<Map<String, Route>>() }
            .also { routesCache = it }
    }

    suspend fun getStops(): List<Stop> = staticMutex.withLock {
        stopsCache?.let { return it }
        retrying { httpClient.get("$backendUrl/stops.json").body<List<ApiStop>>() }
            .map { it.toStop() }
            .also { stopsCache = it }
    }

    // ── Favourites ─────────────────────────────────────────────────────────────

    /** Currently persisted favourite stops (empty if none / unreadable). */
    fun getFavouriteStops(): List<FavouriteStop> {
        val raw = readPref(PREF_FAVOURITES) ?: return emptyList()
        return try {
            json.decodeFromString<List<FavouriteStop>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Persists the given favourite stops, replacing any previous value. */
    fun saveFavouriteStops(favourites: List<FavouriteStop>) {
        writePref(PREF_FAVOURITES, json.encodeToString(favourites))
    }

    // ── Last-viewed UI state (restored on next launch) ────────────────────────

    fun getLastViewedRoute(): String? = readPref(PREF_LAST_ROUTE)?.takeIf { it.isNotEmpty() }

    fun saveLastViewedRoute(routeNum: String?) = writePref(PREF_LAST_ROUTE, routeNum ?: "")

    /**
     * Live bus positions and whether the backend considers them live.
     *
     * [stale] is the backend saying it could not reach NTA, so these are its last known positions
     * (or none at all). Without it an outage and an empty timetable are the same empty map, which
     * is why the app used to guess with a two-minute timer before the backend reported this.
     */
    data class BusPositions(
        val byRoute: Map<String, List<BusLocation>>,
        val stale: Boolean = false,
        /** See [BusApiResponse.staleSeconds]. */
        val staleSeconds: Long? = null
    ) {
        val all: List<BusLocation> get() = byRoute.values.flatten()
        fun forRoute(routeNum: String): List<BusLocation> = byRoute[routeNum] ?: emptyList()
    }

    /** Departures for a stop, and whether their live delays are current. */
    data class StopDepartures(
        val times: List<DepartureTime>,
        val stale: Boolean = false,
        val staleSeconds: Long? = null
    )

    /** All Galway bus positions keyed by route number. */
    suspend fun getBusPositions(): BusPositions = fetchVehicles()

    /** Bus positions for a single route. Pass [forceRefresh] to bypass the cache. */
    suspend fun getBusPositions(routeNum: String, forceRefresh: Boolean = false): BusPositions =
        fetchVehicles(forceRefresh)

    /** Get stop departures with live delay information from the GTFS-RT feed. */
    suspend fun getStopDeparturesWithLive(stopId: String): StopDepartures {
        val livePositions = fetchVehicles().byRoute

        val liveResponse = try {
            httpClient.get("$backendUrl/stops/$stopId").body<StopDeparturesResponse>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed request is as un-live as a stale one; say so rather than passing off an
            // empty list as a stop with no departures due.
            StopDeparturesResponse(times = emptyList(), stale = true)
        }

        val nowMs = nowEpochMilliseconds()
        val result = mutableListOf<DepartureTime>()
        val usedVehicleIds = mutableSetOf<String>()

        // The backend now supplies each departure's authoritative live delay and trip id, so we take
        // them directly rather than re-deriving lateness by matching predicted times to the schedule.
        // That derivation collapsed to ~0 for a bus running roughly one headway late (its predicted
        // time lands on the next scheduled slot), which hid the "(late)" state in the list.
        // Lexicographic on the ISO string is chronological here: the backend formats every
        // timestamp as a whole-second UTC Instant, so the fields line up column for column.
        val liveTimes = liveResponse.times
            .filter { it.depart_timestamp != null }
            .sortedBy { it.depart_timestamp }

        for (live in liveTimes) {
            if (Instant.parse(live.depart_timestamp!!).toEpochMilliseconds() < nowMs - 60_000) continue

            // Attach a live vehicle only via a trustworthy link (exact trip, or the bus's own
            // next-stop prediction for this stop). No headsign guessing — see matchVehicle.
            val busesOnRoute = livePositions[live.timetable_id] ?: emptyList()
            val vehicle = matchVehicle(busesOnRoute, live.tripId, stopId, usedVehicleIds)
            val vehicleId = vehicle?.vehicle_id
            if (!vehicleId.isNullOrBlank()) usedVehicleIds.add(vehicleId)

            result.add(live.copy(vehicleId = vehicleId))
            if (result.size >= 5) break
        }

        return StopDepartures(result, liveResponse.stale, liveResponse.staleSeconds)
    }

    /**
     * Picks the live vehicle serving a departure, using only trustworthy links:
     *  1. exact trip match (the bus is running the departure's trip) AND the stop is still ahead of
     *     the bus, or
     *  2. the bus's own next-stop prediction includes this stop.
     *
     * We deliberately do NOT guess by route+headsign: frequent routes (e.g. 401) run several
     * buses in the same direction at once, so a headsign match attaches an arbitrary bus to the
     * "next due" departure and shows the wrong vehicle id. No id is better than a wrong id.
     *
     * The trip match alone is not enough: a departure can linger on a future scheduled slot after
     * the vehicle has already driven past the stop (the trip's real-time updates drop out, so the
     * backend reverts to the stale static time). Attaching the bus then shows a vehicle that's
     * visibly well beyond the stop. When we have the bus's next_stops we require the stop to be in
     * that ahead-list; only when it's absent (no position/sequence data at all) do we fall back to
     * trusting the trip match, since we then can't tell whether it has passed.
     */
    private fun matchVehicle(
        busesOnRoute: List<BusLocation>,
        tripId: String?,
        stopId: String,
        usedVehicleIds: Set<String>
    ): BusLocation? {
        fun available(bus: BusLocation): Boolean {
            val vid = bus.vehicle_id
            return !vid.isNullOrBlank() && vid !in usedVehicleIds
        }
        // True when the stop is still ahead of the bus, or we have no ahead-list to judge by.
        fun stopAhead(bus: BusLocation): Boolean {
            val ahead = bus.next_stops ?: return true
            return ahead.any { it.stop_ref == stopId }
        }

        if (tripId != null) {
            busesOnRoute.firstOrNull { it.trip_duid == tripId && available(it) && stopAhead(it) }
                ?.let { return it }
        }
        return busesOnRoute.firstOrNull { bus ->
            available(bus) && bus.next_stops?.any { it.stop_ref == stopId } == true
        }
    }

    /** Destination headsign for each direction, aligned index-for-index with [getStopsForRoute]. */
    suspend fun getDirectionHeadsigns(routeNum: String): List<String> =
        routeDetails(routeNum).direction_headsigns

    /** Ordered stop lists per direction for a route (typically 2 directions). */
    suspend fun getStopsForRoute(routeNum: String): List<List<Stop>> =
        routeDetails(routeNum).stops.map { stops -> stops.map { it.toStop() } }

    /**
     * Road geometry per direction, aligned index-for-index with [getStopsForRoute] — the path the
     * bus actually drives, rather than straight lines between stops.
     */
    suspend fun getRouteShapes(routeNum: String): List<List<List<Double>>> =
        routeDetails(routeNum).shapes

    /**
     * Retries a static fetch before giving up. The backend scales to zero, so the first request
     * after a quiet spell can fail or time out while it starts. This data used to be read from a
     * file inside the app, where it could not fail at all — so nothing upstream was written
     * expecting it to.
     */
    private suspend fun <T> retrying(attempts: Int = 3, block: suspend () -> T): T {
        var last: Exception? = null
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
                if (attempt < attempts - 1) delay(400L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException("Request failed")
    }

    /**
     * A single shape's geometry. Shapes are shared by many trips and identical across snapshots,
     * so one fetch per id serves every trip that drives it.
     */
    suspend fun getShape(shapeId: String): List<List<Double>> = shapeMutex.withLock {
        shapeCache[shapeId]?.let { return it }
        val points = try {
            retrying { httpClient.get("$backendUrl/shapes/$shapeId").body<List<List<Double>>>() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        points.also { if (it.isNotEmpty()) shapeCache[shapeId] = it }
    }

    private suspend fun routeDetails(routeNum: String): ApiRouteDetails = routeDetailsMutex.withLock {
        routeDetailsCache[routeNum]?.let { return it }
        val details = try {
            retrying { httpClient.get("$backendUrl/routes/$routeNum").body<ApiRouteDetails>() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ApiRouteDetails()
        }
        details.also { routeDetailsCache[routeNum] = it }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun fetchVehicles(force: Boolean = false): BusPositions = vehiclesMutex.withLock {
        if (!force) vehiclesCache?.let { (t, data) ->
            if (nowEpochMilliseconds() - t < cacheTtlMs) return@withLock data
        }
        val response = httpClient.get("$backendUrl/bus.json").body<BusApiResponse>()
        val result = BusPositions(
            byRoute = response.bus.mapValues { (routeId, buses) ->
                buses.map { it.copy(timetable_id = it.timetable_id ?: routeId) }
            },
            stale = response.stale,
            staleSeconds = response.staleSeconds
        )
        vehiclesCache = nowEpochMilliseconds() to result
        result
    }

    private companion object {
        const val PREF_FAVOURITES = "favourite_stops"
        const val PREF_LAST_ROUTE = "last_route"
    }
}
