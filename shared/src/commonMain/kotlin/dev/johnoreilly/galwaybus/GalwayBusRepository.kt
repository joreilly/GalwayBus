package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.*
import galwaybus.shared.generated.resources.Res
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class GalwayBusRepository(
    injectedHttpClient: HttpClient? = null,
    injectedSnapshot: GalwayGtfsSnapshot? = null
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = injectedHttpClient ?: HttpClient {
        install(ContentNegotiation) { json(json) }
        expectSuccess = true
    }
    
    private val backendUrl = "https://galwaybus-68012425471.europe-west1.run.app"
    private val dublinTz = TimeZone.of("Europe/Dublin")
    private val cacheTtlMs = 30_000L

    // Snapshot
    private val snapshotMutex = Mutex()
    private var snapshot: GalwayGtfsSnapshot? = injectedSnapshot

    // RT caches: epochMs timestamp paired with data
    private val vehiclesMutex = Mutex()
    private var vehiclesCache: Pair<Long, Map<String, List<BusLocation>>>? = null
    private var activeTripsForDay: Pair<LocalDate, Set<String>>? = null

    data class StopUpdate(
        val arrivalDelay: Int? = null,
        val departureDelay: Int? = null,
        val departureTimestamp: Long? = null
    )

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun getRoutes(): Map<String, Route> {
        val s = snapshot()
        return s.routes.mapValues { (shortName, r) ->
            Route(shortName.toIntOrNull() ?: 0, r.longName, shortName)
        }
    }

    suspend fun getStops(): List<Stop> {
        val s = snapshot()
        val directions = stopDirections(s)
        return s.stops.map { (stopId, stop) ->
            Stop(
                stop_ref = stopId,
                stop_id = stop.code,
                long_name = stop.name,
                short_name = stop.name,
                latitude = stop.lat,
                longitude = stop.lon,
                routes = s.stopRoutes[stopId],
                direction = directions[stopId]
            )
        }
    }

    // stop_ref -> destination its buses most often head towards (dominant departure headsign).
    // Lets the UI distinguish opposite-direction stops that share a name and route.
    private var stopDirectionsCache: Map<String, String>? = null

    private fun stopDirections(s: GalwayGtfsSnapshot): Map<String, String> {
        stopDirectionsCache?.let { return it }
        val result = HashMap<String, String>()
        for ((stopRef, departures) in s.stopDepartures) {
            val counts = HashMap<String, Int>()
            for (dep in departures) {
                val head = s.trips[dep.tId]?.headsign
                if (!head.isNullOrBlank()) counts[head] = (counts[head] ?: 0) + 1
            }
            counts.maxByOrNull { it.value }?.let { result[stopRef] = it.key }
        }
        return result.also { stopDirectionsCache = it }
    }

    suspend fun getStopDepartures(stopId: String, lookbackMinutes: Int = 0): List<DepartureTime> {
        val s = snapshot()
        val today = todayIn(dublinTz)
        val activeTripIds = activeTripIds(s, today)
        val nowSecs = nowLocalDateTimeIn(dublinTz).let {
            it.hour * 3600 + it.minute * 60 + it.second
        }
        val startOfDay = today.atStartOfDayIn(dublinTz)

        val departures = (s.stopDepartures[stopId] ?: emptyList())
            .filter { dep -> dep.tId in activeTripIds && dep.secs >= (nowSecs - lookbackMinutes * 60) }
            .let { if (lookbackMinutes > 0) it else it.take(5) }

        return departures.map { dep ->
            val trip = s.trips[dep.tId]
            val routeShortName = s.routes.entries.find { it.value.id == trip?.rId }?.key ?: ""
            val departureInstant = startOfDay + dep.secs.seconds
            DepartureTime(
                display_name = trip?.headsign ?: "",
                timetable_id = routeShortName,
                low_floor = false,
                depart_timestamp = departureInstant.toString(),
                tripId = dep.tId
            )
        }
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

    /** All Galway bus positions keyed by route number. */
    suspend fun getBusPositions(): Map<String, List<BusLocation>> = fetchVehicles()

    /** Bus positions for a single route. Pass [forceRefresh] to bypass the cache. */
    suspend fun getBusPositions(routeNum: String, forceRefresh: Boolean = false): List<BusLocation> =
        fetchVehicles(forceRefresh)[routeNum] ?: emptyList()

    /** Get stop departures with live delay information from the GTFS-RT feed. */
    suspend fun getStopDeparturesWithLive(stopId: String): Pair<List<DepartureTime>, Map<String, List<BusLocation>>> {
        val scheduledDepartures = getStopDepartures(stopId, lookbackMinutes = 30)
        val livePositions = fetchVehicles()

        val liveResponse = try {
            httpClient.get("$backendUrl/stops/$stopId").body<StopDeparturesResponse>()
        } catch (e: Exception) {
            StopDeparturesResponse(times = emptyList())
        }

        val s = snapshot()
        val stopLocation = s.stops[stopId]
        val nowMs = nowEpochMilliseconds()
        val liveTimes = liveResponse.times.filter { it.depart_timestamp != null }.sortedBy { it.depart_timestamp }
        val result = mutableListOf<DepartureTime>()
        val usedScheduledIndices = mutableSetOf<Int>()
        val usedVehicleIds = mutableSetOf<String>()

        for (live in liveTimes) {
            val liveInstant = Instant.parse(live.depart_timestamp!!)
            if (liveInstant.toEpochMilliseconds() < nowMs - 60_000) continue

            var bestScheduledIdx = -1
            var minScore = Long.MAX_VALUE

            for (i in scheduledDepartures.indices) {
                if (i in usedScheduledIndices) continue
                val scheduled = scheduledDepartures[i]
                if (scheduled.timetable_id != live.timetable_id) continue

                val staticInstant = Instant.parse(scheduled.depart_timestamp!!)
                val diff = (liveInstant - staticInstant).inWholeSeconds
                val absDiff = if (diff < 0) -diff else diff
                // Heuristic: Buses are much more likely to be late than early.
                // Apply a penalty to early matches to favor matching with a late previous bus.
                val matchScore = if (diff < 0) absDiff * 4 else absDiff

                if (matchScore < minScore && absDiff < 1800) {
                    minScore = matchScore
                    bestScheduledIdx = i
                }
            }

            if (bestScheduledIdx != -1) {
                usedScheduledIndices.add(bestScheduledIdx)
                val scheduled = scheduledDepartures[bestScheduledIdx]
                val staticInstant = Instant.parse(scheduled.depart_timestamp!!)
                val delay = (liveInstant - staticInstant).inWholeSeconds.toInt()

                // Find vehicle ID if possible
                val busesOnRoute = livePositions[scheduled.timetable_id] ?: emptyList()
                val vehicle = busesOnRoute
                    .find { it.trip_duid == scheduled.tripId && (it.vehicle_id == null || it.vehicle_id !in usedVehicleIds) }
                    ?: busesOnRoute.find { 
                        it.vehicle_id != null && it.vehicle_id !in usedVehicleIds && headsignMatches(it.headsign, scheduled.display_name) 
                    }

                val vehicleId = vehicle?.vehicle_id ?: live.vehicleId
                if (!vehicleId.isNullOrBlank()) usedVehicleIds.add(vehicleId)
                
                result.add(scheduled.copy(
                    delaySeconds = delay,
                    depart_timestamp = live.depart_timestamp,
                    tripId = scheduled.tripId,
                    vehicleId = vehicleId
                ))
            } else {
                // Second pass: try to match live-only departure to a vehicle from bus.json
                val busesOnRoute = livePositions[live.timetable_id] ?: emptyList()
                val bestBus = if (stopLocation != null) {
                    busesOnRoute
                        .filter { it.vehicle_id !in usedVehicleIds && headsignMatches(it.headsign, live.display_name) }
                        .minByOrNull { bus: BusLocation ->
                        val dx = bus.longitude - stopLocation.lon
                        val dy = bus.latitude - stopLocation.lat
                        dx * dx + dy * dy
                    }
                } else {
                    busesOnRoute.firstOrNull { it.vehicle_id !in usedVehicleIds && headsignMatches(it.headsign, live.display_name) }
                }
                
                val finalVehicleId = bestBus?.vehicle_id ?: live.vehicleId
                if (!finalVehicleId.isNullOrBlank()) usedVehicleIds.add(finalVehicleId)

                result.add(if (bestBus != null) live.copy(tripId = bestBus.trip_duid, vehicleId = finalVehicleId) else live)
            }
            if (result.size >= 5) break
        }

        if (result.size < 5) {
            for (i in scheduledDepartures.indices) {
                if (i in usedScheduledIndices) continue
                val scheduled = scheduledDepartures[i]
                val staticInstant = Instant.parse(scheduled.depart_timestamp!!)
                if (staticInstant.toEpochMilliseconds() >= nowMs) {
                    val busesOnRoute = livePositions[scheduled.timetable_id] ?: emptyList()
                    val vehicle = busesOnRoute.find { it.trip_duid == scheduled.tripId && (it.vehicle_id == null || it.vehicle_id !in usedVehicleIds) }
                    val vehicleId = vehicle?.vehicle_id
                    if (!vehicleId.isNullOrBlank()) usedVehicleIds.add(vehicleId)
                    
                    result.add(scheduled.copy(vehicleId = vehicleId))
                    if (result.size >= 5) break
                }
            }
        }

        return result.sortedBy { it.depart_timestamp } to livePositions
    }

    private fun normalizeHeadsignForMatch(headsign: String?): String {
        if (headsign.isNullOrBlank()) return ""

        var normalized = headsign
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

        normalized = normalized.removePrefix("towards ")
            .removePrefix("to ")

        // Strip common suffixes like "X (via ...)".
        normalized = normalized.substringBefore(" (")
        normalized = normalized.substringBefore(" via ")

        return normalized.trim()
    }

    private fun headsignMatches(busHeadsign: String?, departureHeadsign: String?): Boolean {
        val a = normalizeHeadsignForMatch(busHeadsign)
        val b = normalizeHeadsignForMatch(departureHeadsign)
        return a.isNotEmpty() && a == b
    }

    /**
     * Destination headsign for each direction, aligned index-for-index with [getStopsForRoute].
     *
     * The snapshot's per-direction stop lists ([GalwayGtfsSnapshot.routeStops]) are not ordered by
     * GTFS direction index, so we can't just label list N with the headsign of `dir == N` — that
     * puts the labels on the wrong stop lists. Instead we infer each list's direction from the trips
     * actually serving its stops (dominant `dir` across the list) and label it with that direction's
     * headsign, which is the destination the riders on those stops are heading to.
     */
    suspend fun getDirectionHeadsigns(routeNum: String): List<String> {
        val s = snapshot()
        val routeId = s.routes[routeNum]?.id ?: return emptyList()

        // Representative (first non-blank) headsign per GTFS direction.
        val headsignByDir = mutableMapOf<Int, String>()
        for (trip in s.trips.values) {
            if (trip.rId == routeId && trip.headsign.isNotBlank())
                headsignByDir.getOrPut(trip.dir) { trip.headsign }
        }

        val stopLists = s.routeStops[routeNum] ?: return emptyList()
        return stopLists.mapIndexed { i, stopIds ->
            val dirVotes = mutableMapOf<Int, Int>()
            for (stopId in stopIds) {
                for (dep in s.stopDepartures[stopId] ?: emptyList()) {
                    val trip = s.trips[dep.tId] ?: continue
                    if (trip.rId == routeId) dirVotes[trip.dir] = (dirVotes[trip.dir] ?: 0) + 1
                }
            }
            dirVotes.maxByOrNull { it.value }?.key?.let { headsignByDir[it] }
                ?: "Direction ${i + 1}"
        }
    }

    /** Ordered stop lists per direction for a route (typically 2 directions). */
    suspend fun getStopsForRoute(routeNum: String): List<List<Stop>> {
        val s = snapshot()
        return (s.routeStops[routeNum] ?: emptyList()).map { stopIds ->
            stopIds.mapNotNull { stopId ->
                s.stops[stopId]?.let { stop ->
                    Stop(
                        stop_ref = stopId,
                        stop_id = stop.code,
                        long_name = stop.name,
                        short_name = stop.name,
                        latitude = stop.lat,
                        longitude = stop.lon,
                        routes = s.stopRoutes[stopId]
                    )
                }
            }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun snapshot(): GalwayGtfsSnapshot {
        snapshot?.let { return it }
        return snapshotMutex.withLock {
            snapshot?.let { return it }
            val text = Res.readBytes("files/galway_gtfs.json").decodeToString()
            json.decodeFromString<GalwayGtfsSnapshot>(text).also { snapshot = it }
        }
    }

    private suspend fun activeTripIds(s: GalwayGtfsSnapshot, today: LocalDate): Set<String> {
        activeTripsForDay?.let { (date, ids) -> if (date == today) return ids }
        val active = computeActiveServices(s, today)
        val ids = s.trips.filterValues { it.sId in active }.keys
        activeTripsForDay = today to ids
        return ids
    }

    private fun computeActiveServices(s: GalwayGtfsSnapshot, today: LocalDate): Set<String> {
        val todayStr = today.toGtfsDate()
        val dayIdx = today.dayOfWeek.ordinal   // 0=Monday … 6=Sunday
        val active = mutableSetOf<String>()
        s.calendar.forEach { (serviceId, cal) ->
            if (cal.days.getOrNull(dayIdx) == '1' && todayStr >= cal.start && todayStr <= cal.end)
                active.add(serviceId)
        }
        s.calendarDates.forEach { cd ->
            if (cd.date == todayStr) when (cd.type) {
                1 -> active.add(cd.sId)
                2 -> active.remove(cd.sId)
            }
        }
        return active
    }

    private suspend fun fetchVehicles(force: Boolean = false): Map<String, List<BusLocation>> = vehiclesMutex.withLock {
        if (!force) vehiclesCache?.let { (t, data) ->
            if (nowEpochMilliseconds() - t < cacheTtlMs) return@withLock data
        }
        val response = httpClient.get("$backendUrl/bus.json").body<BusApiResponse>()
        val result = response.bus.mapValues { (routeId, buses) ->
            buses.map { it.copy(timetable_id = it.timetable_id ?: routeId) }
        }
        vehiclesCache = nowEpochMilliseconds() to result
        result
    }

    private fun LocalDate.toGtfsDate(): String =
        "${year}${month.number.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"

    private companion object {
        const val PREF_FAVOURITES = "favourite_stops"
        const val PREF_LAST_ROUTE = "last_route"
    }
}
