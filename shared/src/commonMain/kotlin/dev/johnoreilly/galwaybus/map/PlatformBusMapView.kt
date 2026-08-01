package dev.johnoreilly.galwaybus.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop

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
 */
@Composable
expect fun BusMapView(
    positions: List<BusLocation>,
    modifier: Modifier = Modifier,
    stops: List<Stop> = emptyList(),
    trackedTripId: String? = null,
    trackedStopRef: String? = null,
    onStopClick: ((Stop) -> Unit)? = null,
    userLocation: UserLocation? = null
)
