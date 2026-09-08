package dev.johnoreilly.galwaybus.model

import kotlinx.serialization.Serializable

// --- Domain models ---

@Serializable
data class Route(
    val timetable_id: Int,
    val long_name: String,
    val short_name: String
)

@Serializable
data class Stop(
    val stop_ref: String,
    val stop_id: String,
    val long_name: String,
    /** Irish (Gaeilge) name from the GTFS Translations feed, if available; null otherwise. */
    val long_name_ga: String? = null,
    val short_name: String,
    val latitude: Double,
    val longitude: Double,
    val routes: List<String>? = null,
    /** Destination this stop's buses head towards (dominant departure headsign), to tell apart
     *  opposite-direction stops that share a name/route. Null when unknown. */
    val direction: String? = null
)

@Serializable
data class FavouriteStop(
    val stopRef: String,
    val name: String,
    val stopId: String = ""
)

@Serializable
data class DepartureTime(
    val display_name: String,
    val timetable_id: String,
    val low_floor: Boolean,
    val depart_timestamp: String? = null,
    val delaySeconds: Int? = null,
    val tripId: String? = null,
    val vehicleId: String? = null
)

@Serializable
data class BusLocation(
    val latitude: Double,
    val longitude: Double,
    /** Compass heading in degrees (0 = north, clockwise) where the feed supplies one, else null.
     *  NTA quantises it to 45-degree steps, so it is 8-point rather than smooth. */
    val bearing: Float? = null,
    val modified_timestamp: String,
    val trip_duid: String,
    val vehicle_id: String? = null,
    val timetable_id: String? = null,
    val headsign: String? = null,
    val next_stop_ref: String? = null,
    val next_stops: List<StopPrediction>? = null,
    /** The trip's road geometry, fetched from the backend by id and cached; shared by many trips. */
    val shape_id: String? = null
)

@Serializable
data class StopPrediction(
    val stop_ref: String,
    val stop_sequence: Int,
    val arrival_timestamp: String? = null,
    val departure_timestamp: String? = null,
    val delay: Int? = null
)

// --- Backend response wrapper ---

@Serializable
data class BusApiResponse(
    val bus: Map<String, List<BusLocation>>,
    /**
     * The backend could not reach NTA, so this is last-known data or nothing at all — as opposed
     * to a genuinely quiet night, which looks identical in [bus] alone. Absent on older backends,
     * where it defaults to false and the app behaves as it always did.
     */
    val stale: Boolean = false,
    /** How old the served fallback is, in seconds; null when [stale] is false, or when it's true
     *  but there was nothing to fall back to at all. See [GalwayBusViewModel.isMeaningfullyStale]. */
    val staleSeconds: Long? = null
)

@Serializable
data class StopDeparturesResponse(
    val times: List<DepartureTime>,
    /** See [BusApiResponse.stale] — here it means the times carry no live delay. */
    val stale: Boolean = false,
    /** See [BusApiResponse.staleSeconds]. */
    val staleSeconds: Long? = null
)

// --- Backend static-data payloads ---
//
// These mirror what the API returns; the UI keeps its own [Stop] shape (a String stop_id it
// searches and shows, and long_name_ga) so the wire format can differ without rippling through
// the screens. Until Sept 2026 this data came from a GTFS snapshot bundled in the app, which
// silently went stale — the backend regenerates weekly and is the single source now.

@Serializable
data class ApiStop(
    val stop_ref: String,
    val stop_id: Int = 0,
    val long_name: String = "",
    val irish_long_name: String? = null,
    val short_name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val routes: List<String>? = null,
    /** Only /stops.json sets this; route stop lists leave it null. */
    val direction: String? = null
) {
    fun toStop(): Stop = Stop(
        stop_ref = stop_ref,
        stop_id = stop_id.toString(),
        long_name = long_name,
        long_name_ga = irish_long_name,
        short_name = short_name.ifEmpty { long_name },
        latitude = latitude,
        longitude = longitude,
        routes = routes,
        direction = direction
    )
}

@Serializable
data class ApiRouteDetails(
    val route: Route? = null,
    /** One list per direction. */
    val stops: List<List<ApiStop>> = emptyList(),
    /** Destination of each direction, index-for-index with [stops]. */
    val direction_headsigns: List<String> = emptyList(),
    /** Road geometry of each direction as ordered [lat, lon] pairs, index-for-index with [stops]. */
    val shapes: List<List<List<Double>>> = emptyList()
)
