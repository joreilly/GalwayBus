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
    val modified_timestamp: String,
    val trip_duid: String,
    val vehicle_id: String? = null,
    val timetable_id: String? = null,
    val headsign: String? = null,
    val next_stop_ref: String? = null,
    val next_stops: List<StopPrediction>? = null
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
data class BusApiResponse(val bus: Map<String, List<BusLocation>>)

@Serializable
data class StopDeparturesResponse(
    val times: List<DepartureTime>
)

// --- Galway GTFS snapshot format (loaded from composeResources/files/galway_gtfs.json) ---

@Serializable
data class GalwayGtfsSnapshot(
    val generated: String = "",
    val stops: Map<String, GtfsSnapshotStop> = emptyMap(),
    val routes: Map<String, GtfsSnapshotRoute> = emptyMap(),
    val routeStops: Map<String, List<List<String>>> = emptyMap(),
    val stopRoutes: Map<String, List<String>> = emptyMap(),
    val trips: Map<String, GtfsSnapshotTrip> = emptyMap(),
    val calendar: Map<String, GtfsSnapshotCalendar> = emptyMap(),
    val calendarDates: List<GtfsSnapshotCalendarDate> = emptyList(),
    val stopDepartures: Map<String, List<GtfsSnapshotDeparture>> = emptyMap()
)

@Serializable data class GtfsSnapshotStop(val code: String = "", val name: String = "", val lat: Double = 0.0, val lon: Double = 0.0)
@Serializable data class GtfsSnapshotRoute(val id: String = "", val longName: String = "")
@Serializable data class GtfsSnapshotTrip(val rId: String = "", val sId: String = "", @kotlinx.serialization.SerialName("head") val headsign: String = "", val dir: Int = 0)
@Serializable data class GtfsSnapshotCalendar(val days: String = "", val start: String = "", val end: String = "")
@Serializable data class GtfsSnapshotCalendarDate(val sId: String = "", val date: String = "", val type: Int = 0)
@Serializable data class GtfsSnapshotDeparture(val tId: String = "", val secs: Int = 0, val seq: Int = 0)
