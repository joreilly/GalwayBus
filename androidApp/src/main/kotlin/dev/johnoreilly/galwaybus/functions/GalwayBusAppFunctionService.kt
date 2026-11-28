package dev.johnoreilly.galwaybus.functions

import android.location.Address
import android.location.Geocoder
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionNotSupportedException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dev.johnoreilly.galwaybus.GalwayBusPrefs
import dev.johnoreilly.galwaybus.GalwayBusRepository
import dev.johnoreilly.galwaybus.location.distanceMeters
import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * AppFunctions for GalwayBus. KSP generates the concrete [GalwayBusAppFunctionService] registered
 * in the manifest. They're what the system offers assistants, and the tools of the in-app
 * "Ask GalwayBus" agent.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "GalwayBusAppFunctionService",
    appFunctionXmlFileName = "galwaybus_app_function_service",
)
abstract class BaseGalwayBusAppFunctionService : AppFunctionService() {

    private val repository by lazy {
        // The service can start without the activity (an assistant calling in), and saved stops
        // are read from preferences, which need a context.
        GalwayBusPrefs.appContext = applicationContext
        GalwayBusRepository()
    }

    /**
     * Lists Galway's city bus routes.
     *
     * @return Every route, with its number and the places it runs between.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listRoutes(): List<RouteResponse> = withContext(Dispatchers.IO) {
        load("the list of routes") { repository.getRoutes() }
            .values
            .sortedBy { it.short_name }
            .map { RouteResponse(number = it.short_name, name = it.long_name) }
    }

    /**
     * Finds Galway bus stops by name or stop number. Most names cover several stops, one for each
     * direction, so check [StopResponse.towards].
     *
     * @param query Name of the stop ("Eyre Square", "Salthill"), or the number shown on the stop.
     *   Stop names are often shortened, such as "Hospl" for hospital and "Pk" for park.
     * @param routeNumber Only include stops served by this route, such as "405".
     * @param limit Maximum number of stops to return, from 1 to 20. Defaults to 10.
     * @return The matching stops.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun findStops(
        query: String,
        routeNumber: String? = null,
        limit: Int = 0,
    ): List<StopResponse> = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw AppFunctionInvalidArgumentException("Give a stop name or number to search for.")
        stops()
            .filter { it.matchesName(query) && it.servesRoute(routeNumber) }
            .take(limit.inRange(DEFAULT_STOP_LIMIT))
            .map { it.toResponse(distanceMetres = null) }
    }

    /**
     * Finds the Galway bus stops closest to a place, nearest first.
     *
     * @param place Name or address of a place in Galway, such as a landmark, street or area
     *   ("Galway Cathedral", "Shop Street"). The app looks up where it is, so use this for named
     *   places instead of guessing coordinates.
     * @param latitude Latitude to search around, in degrees. Only when already known.
     * @param longitude Longitude to search around, in degrees. Only when already known.
     * @param routeNumber Only include stops served by this route, such as "405".
     * @param limit Maximum number of stops to return, from 1 to 20. Defaults to 5.
     * @return The nearest stops, with their distance from the place.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun findNearbyStops(
        place: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        routeNumber: String? = null,
        limit: Int = 0,
    ): List<StopResponse> = withContext(Dispatchers.IO) {
        val (lat, lon) = when {
            !place.isNullOrBlank() -> locate(place)
            latitude != null && longitude != null -> latitude to longitude
            else -> throw AppFunctionInvalidArgumentException("Give a place name, or both latitude and longitude.")
        }
        stops()
            .filter { it.servesRoute(routeNumber) }
            .map { it to distanceMeters(lat, lon, it.latitude, it.longitude).roundToInt() }
            .sortedBy { it.second }
            .take(limit.inRange(DEFAULT_NEARBY_LIMIT))
            .map { (stop, distance) -> stop.toResponse(distance) }
    }

    /**
     * The next buses leaving a stop, soonest first, with live delays where buses are being tracked.
     *
     * @param stop The stop: its name ("Eyre Square"), the number shown on it, or a place near it
     *   ("Galway Cathedral"). A name covers every stop of that name, in both directions, unless
     *   [routeNumber] or [towards] narrow it.
     * @param routeNumber Only include buses on this route, such as "405". Only when the user
     *   names a route; omit it to include every route at the stop.
     * @param towards Only include buses that go to this place: where they end ("Rahoon") or a
     *   stop or place on the way ("Salthill"). Only when the user says where they're going.
     * @param limit Maximum number of departures to return, from 1 to 20. Defaults to 8.
     * @return The departures, with the stop each one leaves from.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getDepartures(
        stop: String,
        routeNumber: String? = null,
        towards: String? = null,
        limit: Int = 0,
    ): List<DepartureResponse> = withContext(Dispatchers.IO) {
        val candidates = resolveStops(stop).filter { it.servesRoute(routeNumber) }
        if (candidates.isEmpty()) {
            throw AppFunctionElementNotFoundException(
                if (routeNumber.isNullOrBlank()) "No stop called '$stop'. Try findStops or findNearbyStops."
                else "No stop called '$stop' is served by route $routeNumber."
            )
        }
        // Where "towards" is, as stops: a bus goes there if its route reaches one of them after the
        // stop it leaves from. Matching the destination alone missed places on the way: no route
        // ends at Salthill, so "Eyre Square to Salthill" found nothing, though the 401 goes there.
        val destinations = towards?.takeIf { it.isNotBlank() }?.let { place ->
            resolveStops(place).mapTo(mutableSetOf()) { it.stop_ref }
        }.orEmpty()
        val now = Instant.now()
        val departures = coroutineScope {
            candidates.take(MAX_STOPS_PER_DEPARTURES).map { s ->
                async { s to load("departures for ${s.long_name}") { repository.getStopDeparturesWithLive(s.stop_ref) } }
            }.awaitAll()
        }
            .flatMap { (s, departures) -> departures.times.map { s to it } }
            .filter { (_, d) -> routeNumber.isNullOrBlank() || d.timetable_id.equals(routeNumber.trim(), ignoreCase = true) }
        val routes = if (destinations.isEmpty()) emptyMap() else coroutineScope {
            departures.map { it.second.timetable_id }.distinct().map { route ->
                // Route details are cached, and come back empty rather than failing.
                async {
                    route to RouteStops(
                        repository.getDirectionHeadsigns(route),
                        repository.getStopsForRoute(route).map { direction -> direction.map { it.stop_ref } },
                    )
                }
            }.awaitAll().toMap()
        }
        departures
            .filter { (s, d) ->
                towards.isNullOrBlank() || d.display_name.matchesText(towards) ||
                    routes[d.timetable_id]?.reaches(s.stop_ref, d.display_name, destinations) == true
            }
            .mapNotNull { (s, d) -> d.toResponse(s, now) }
            .sortedBy { it.minutesAway }
            .take(limit.inRange(DEFAULT_DEPARTURE_LIMIT))
    }

    /**
     * Lists the stops the user has saved as favourites in the app.
     *
     * @return The saved stops, in the order the user keeps them. Empty if they haven't saved any.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listSavedStops(): List<StopResponse> = withContext(Dispatchers.IO) {
        val saved = repository.getFavouriteStops()
        if (saved.isEmpty()) return@withContext emptyList()
        val byRef = stops().associateBy { it.stop_ref }
        saved.mapNotNull { byRef[it.stopRef]?.toResponse(distanceMetres = null) }
    }

    /**
     * The stops [stop] could mean: a stop number, then a stop name, then (when no stop has that
     * name) the stops near a place of that name. The last is what makes "the next bus from the
     * cathedral" one call: models otherwise guess a stop name, or coordinates.
     */
    private suspend fun resolveStops(stop: String): List<Stop> {
        val text = stop.trim()
        if (text.isEmpty()) throw AppFunctionInvalidArgumentException("Give a stop name, number or nearby place.")
        val all = stops()
        all.firstOrNull { it.stop_id == text || it.stop_ref.equals(text, ignoreCase = true) }?.let { return listOf(it) }
        all.filter { it.matchesName(text) }.takeIf { it.isNotEmpty() }?.let { return it }
        val (lat, lon) = try {
            locate(text)
        } catch (e: AppFunctionElementNotFoundException) {
            return emptyList()
        } catch (e: AppFunctionNotSupportedException) {
            return emptyList()
        }
        // The nearest stop, and the others of its name close by (the far side of the road). A
        // plain radius took in neighbouring stops too, and listed the same bus at each of them.
        val nearest = all.minByOrNull { distanceMeters(lat, lon, it.latitude, it.longitude) } ?: return emptyList()
        if (distanceMeters(lat, lon, nearest.latitude, nearest.longitude) > NEAR_PLACE_METRES) return emptyList()
        return all.filter {
            it.long_name == nearest.long_name &&
                distanceMeters(nearest.latitude, nearest.longitude, it.latitude, it.longitude) <= SAME_STOP_METRES
        }
    }

    /**
     * Where [place] is, looked up with [Geocoder] within Galway first, so "the cathedral" is
     * Galway's. Gemini Nano, given only names, guesses coordinates; the BikeShare agent put
     * "near Tower Bridge" 3.5km out.
     */
    private suspend fun locate(place: String): Pair<Double, Double> {
        if (!Geocoder.isPresent()) {
            throw AppFunctionNotSupportedException(
                "Looking up places isn't available on this device. Use findStops with a stop name instead."
            )
        }
        val geocoder = Geocoder(applicationContext)
        // A place Galway doesn't have ("Tower Bridge") comes back as Galway itself, the city's
        // centre, which would answer with Eyre Square's buses. Only a match more precise than
        // the city counts.
        val address = geocoder.find(place, GALWAY_SOUTH, GALWAY_WEST, GALWAY_NORTH, GALWAY_EAST)
            ?.takeUnless { it.isJustTheCity() }
            ?: geocoder.find("$place, Galway")?.takeUnless { it.isJustTheCity() }
            ?: throw AppFunctionElementNotFoundException("Couldn't find a place called '$place' in Galway.")
        if (distanceMeters(GALWAY_LAT, GALWAY_LON, address.latitude, address.longitude) > MAX_PLACE_DISTANCE_METRES) {
            throw AppFunctionElementNotFoundException("'$place' was found, but it isn't in Galway.")
        }
        return address.latitude to address.longitude
    }

    private fun Address.isJustTheCity(): Boolean =
        thoroughfare == null && premises == null && subLocality == null &&
            (featureName == null || featureName.equals(locality, ignoreCase = true))

    private suspend fun Geocoder.find(name: String, vararg box: Double): Address? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) =
                    continuation.resume(addresses.firstOrNull())
                override fun onError(errorMessage: String?) = continuation.resume(null)
            }
            if (box.size == 4) {
                getFromLocationName(name, 1, box[0], box[1], box[2], box[3], listener)
            } else {
                getFromLocationName(name, 1, listener)
            }
        }

    private suspend fun stops(): List<Stop> = load("the list of stops") { repository.getStops() }

    /**
     * Runs [block], turning a failure to reach the GalwayBus backend into an error the agent can
     * relay, rather than an unexplained failure or an empty result.
     */
    private suspend fun <T> load(what: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("AppFunctions: loading $what failed: $e")
            throw AppFunctionAppUnknownException("Couldn't load $what. Check the connection and try again.")
        }

    private companion object {
        const val DEFAULT_STOP_LIMIT = 10
        const val DEFAULT_NEARBY_LIMIT = 5
        const val DEFAULT_DEPARTURE_LIMIT = 8
        const val MAX_LIMIT = 20
        // Eyre Square alone has seven stops; more than this and the answer is too long to use.
        const val MAX_STOPS_PER_DEPARTURES = 8
        // A place this far from its nearest stop has no stop of its own to answer for.
        const val NEAR_PLACE_METRES = 500
        // Stops of the same name this close together are one stop, either side of the road.
        const val SAME_STOP_METRES = 300

        // Galway city, roughly: the area place lookups search first and the stops cover.
        const val GALWAY_LAT = 53.2740
        const val GALWAY_LON = -9.0490
        const val GALWAY_SOUTH = 53.22
        const val GALWAY_NORTH = 53.34
        const val GALWAY_WEST = -9.20
        const val GALWAY_EAST = -8.88
        const val MAX_PLACE_DISTANCE_METRES = 15_000

        // An omitted limit arrives as 0, not the declared default.
        fun Int.inRange(default: Int) = if (this <= 0) default else coerceAtMost(MAX_LIMIT)
    }
}

/** A route's stops in order, one list per direction, and where each direction is headed. */
internal class RouteStops(private val headsigns: List<String>, private val directions: List<List<String>>) {
    /**
     * Whether a bus leaving stop [from] for [headsign] reaches any of [destinations] after it. The
     * headsign picks the direction; when it matches none (or the data is missing it), any
     * direction that reaches a destination after [from] counts.
     */
    fun reaches(from: String, headsign: String, destinations: Set<String>): Boolean {
        val chosen = headsigns.indices.filter { headsigns[it].equals(headsign, ignoreCase = true) }
            .ifEmpty { directions.indices.toList() }
        return chosen.any { i ->
            val stops = directions.getOrNull(i) ?: return@any false
            val start = stops.indexOf(from)
            start >= 0 && stops.drop(start + 1).any { it in destinations }
        }
    }
}

/** A Galway city bus route. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RouteResponse(
    /** Route number, such as "405". */
    val number: String,
    /** The places the route runs between, such as "Rahoon - Eyre Square - Ballybane". */
    val name: String,
)

/** A Galway bus stop. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class StopResponse(
    /** Name of the stop, such as "Eyre Square". Several stops can share a name. */
    val name: String,
    /** The number shown on the stop, which identifies it uniquely. */
    val stopNumber: String,
    /** Where buses from this stop are heading, such as "Rahoon", or null when unknown. */
    val towards: String?,
    /** Numbers of the routes that stop here. */
    val routes: List<String>,
    /** Latitude of the stop, in degrees. */
    val latitude: Double,
    /** Longitude of the stop, in degrees. */
    val longitude: Double,
    /** Distance from the searched place in metres, or null when no place was searched. */
    val distanceMetres: Int?,
)

/** A bus due to leave a stop. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DepartureResponse(
    /** Route number, such as "405". */
    val route: String,
    /** Where the bus is going. */
    val destination: String,
    /** Name of the stop it leaves from. */
    val stopName: String,
    /** The number shown on the stop it leaves from. */
    val stopNumber: String,
    /** Time it leaves, as 24-hour local time such as "14:05". */
    val departs: String,
    /** Minutes from now until it leaves; 0 means it's due now. */
    val minutesAway: Int,
    /**
     * Minutes behind the timetable (negative if early), when the bus is being tracked live; null
     * when this is the timetabled time only.
     */
    val minutesLate: Int?,
)

internal fun Stop.toResponse(distanceMetres: Int?) = StopResponse(
    name = long_name,
    stopNumber = stop_id,
    towards = direction,
    routes = routes.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    distanceMetres = distanceMetres,
)

private val DUBLIN = ZoneId.of("Europe/Dublin")
private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

internal fun DepartureTime.toResponse(stop: Stop, now: Instant): DepartureResponse? {
    val at = depart_timestamp?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return DepartureResponse(
        route = timetable_id,
        destination = display_name,
        stopName = stop.long_name,
        stopNumber = stop.stop_id,
        departs = CLOCK.format(at.atZone(DUBLIN)),
        // Rounded like the app's own countdown, so the two agree.
        minutesAway = (((at.epochSecond - now.epochSecond) + 30) / 60).toInt().coerceAtLeast(0),
        minutesLate = delaySeconds?.let { (it / 60.0).roundToInt() },
    )
}

internal fun Stop.servesRoute(routeNumber: String?): Boolean =
    routeNumber.isNullOrBlank() || routes.orEmpty().any { it.equals(routeNumber.trim(), ignoreCase = true) }

/**
 * Whether a stop answers to [query]: its number, or every word of the query starting a word of
 * its English or Irish name ("eyre sq" finds "Eyre Square", "salthill prom" finds "Salthill Prom").
 */
internal fun Stop.matchesName(query: String): Boolean {
    val text = query.trim()
    if (text == stop_id) return true
    return long_name.matchesText(text) || long_name_ga?.matchesText(text) == true
}

/** Every word of [query] starts a word of this text, ignoring case, accents and punctuation. */
internal fun String.matchesText(query: String): Boolean {
    val wanted = query.words()
    if (wanted.isEmpty()) return false
    val have = words()
    return wanted.all { w -> have.any { it.startsWith(w) } }
}

private fun String.words(): List<String> =
    java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() && it !in STOP_WORDS }

// Words models add that stop names don't have ("the Eyre Square stop").
private val STOP_WORDS = setOf("the", "stop", "bus", "an", "na", "galway")
