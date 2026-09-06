package dev.johnoreilly.galwaybus.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop

/** A point on a drawn line. The API sends geometry as [lat, lon] pairs; this names them. */
data class MapPoint(val lat: Double, val lon: Double)

/**
 * A stop's predicted departure, drawn beside it on the map.
 *
 * @param label the time to show, already formatted for display (e.g. "16:52").
 * @param upcoming false once the bus has passed the stop, so it can be drawn muted.
 */
data class StopEta(val label: String, val upcoming: Boolean)

/** Geometry as the API sends it — [lat, lon] pairs — as points. */
fun List<List<Double>>.toMapPoints(): List<MapPoint> =
    mapNotNull { p -> if (p.size >= 2) MapPoint(p[0], p[1]) else null }

/**
 * The bus map. Each platform provides a native implementation:
 *  - Android → Google Maps (maps-compose)
 *  - iOS → Apple Maps (MapKit / MKMapView)
 *  - Desktop/JVM → OpenStreetMap tiles drawn on a Compose Canvas ([OsmBusMapView])
 *
 * @param positions live bus locations to plot (route-coloured markers).
 * @param stops stops to plot as tappable markers.
 * @param trackedTripId when set, the map centres/highlights this bus.
 * @param trackedStopRef when set, the map centres/highlights this stop.
 * @param onStopClick invoked when a stop marker is tapped (opens its departures sheet).
 * @param userLocation the device's location, shown as a "you are here" marker.
 * @param polylines road geometry to trace — the path a bus actually drives, rather than straight
 *   lines between its stops. Usually one line: the tracked trip's, or the shown direction's.
 * @param stopEtas predicted departure per stop_ref, drawn beside that stop's marker.
 */
@Composable
expect fun BusMapView(
    positions: List<BusLocation>,
    modifier: Modifier = Modifier,
    stops: List<Stop> = emptyList(),
    trackedTripId: String? = null,
    trackedStopRef: String? = null,
    onStopClick: ((Stop) -> Unit)? = null,
    userLocation: UserLocation? = null,
    polylines: List<List<MapPoint>> = emptyList(),
    stopEtas: Map<String, StopEta> = emptyMap()
)
