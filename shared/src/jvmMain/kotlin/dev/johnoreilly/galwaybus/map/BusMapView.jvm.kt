package dev.johnoreilly.galwaybus.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop

/** Desktop has no native map SDK, so it keeps the shared OpenStreetMap tile renderer. */
@Composable
actual fun BusMapView(
    positions: List<BusLocation>,
    modifier: Modifier,
    stops: List<Stop>,
    trackedTripId: String?,
    trackedStopRef: String?,
    onStopClick: ((Stop) -> Unit)?,
    userLocation: UserLocation?
) {
    OsmBusMapView(
        positions = positions,
        modifier = modifier,
        stops = stops,
        trackedTripId = trackedTripId,
        trackedStopRef = trackedStopRef,
        onStopClick = onStopClick,
        userLocation = userLocation
    )
}
