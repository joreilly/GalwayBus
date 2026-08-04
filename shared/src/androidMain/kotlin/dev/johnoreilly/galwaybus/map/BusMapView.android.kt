package dev.johnoreilly.galwaybus.map

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import dev.johnoreilly.galwaybus.displayName
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import kotlin.math.abs

private const val GALWAY_LAT = 53.2743
private const val GALWAY_LON = -9.0488
private const val STOP_MARKER_MIN_ZOOM = 13f

private val BusLocation.markerKey: String get() = vehicle_id ?: trip_duid

// Route palette shared with the OSM renderer so colours are consistent across platforms.
private val palette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFC0CA33), Color(0xFFF4511E),
    Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF00897B), Color(0xFFFDD835),
    Color(0xFF5E35B1), Color(0xFF039BE5), Color(0xFF7CB342), Color(0xFFFFB300),
    Color(0xFF6D4C41), Color(0xFF546E7A)
)

/**
 * Android map backed by the Google Maps SDK (maps-compose). Requires a Maps API key in the
 * app manifest (wired from local.properties' MAPS_API_KEY); with no key the map tiles render blank.
 */
@OptIn(MapsComposeExperimentalApi::class)
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
    val dark = isSystemInDarkTheme()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(GALWAY_LAT, GALWAY_LON), 14f)
    }

    // Colour encodes the route; direction (sorted headsign index) shapes the marker.
    val busColors = remember(positions) {
        positions.map { it.timetable_id ?: "" }.distinct().sorted()
            .associateWith { route -> palette[abs(route.hashCode()) % palette.size] }
    }
    val routeHeadsigns = remember(positions) {
        positions.groupBy { it.timetable_id ?: "" }
            .mapValues { (_, buses) -> buses.mapNotNull { it.headsign }.distinct().sorted() }
    }

    // Centre/zoom logic mirrors the OSM renderer: follow a tracked bus; otherwise centre once
    // on the user or the tracked stop. With no more specific target (e.g. the "All buses" map),
    // fit every stop into view instead of a fixed zoom — a fixed zoom level covers wildly
    // different geographic spans depending on the device's screen size/aspect ratio.
    var hasCentered by remember { mutableStateOf(false) }
    LaunchedEffect(positions, trackedTripId, trackedStopRef, userLocation) {
        val target: Pair<LatLng, Float>? = when {
            trackedTripId != null ->
                positions.find { it.trip_duid == trackedTripId }
                    ?.let { LatLng(it.latitude, it.longitude) to 16f }
            userLocation != null && !hasCentered ->
                LatLng(userLocation.lat, userLocation.lon) to 15f
            trackedStopRef != null && !hasCentered ->
                stops.find { it.stop_ref == trackedStopRef }
                    ?.let { LatLng(it.latitude, it.longitude) to 16f }
            else -> null
        }
        target?.let { (latLng, zoom) ->
            hasCentered = true
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        }
    }

    val boundsPaddingPx = with(LocalDensity.current) { 48.dp.toPx().toInt() }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapStyleOptions = if (dark) MapStyleOptions(DARK_MAP_STYLE_JSON) else null
        ),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
    ) {
        MapEffect(stops, trackedTripId, trackedStopRef, userLocation) { map ->
            if (!hasCentered && trackedTripId == null && trackedStopRef == null &&
                userLocation == null && stops.isNotEmpty()
            ) {
                val bounds = LatLngBounds.Builder().apply {
                    stops.forEach { include(LatLng(it.latitude, it.longitude)) }
                }.build()
                hasCentered = true
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, boundsPaddingPx))
                } catch (_: IllegalStateException) {
                    // Map not laid out yet (size 0) — fall back to a plain centre/zoom.
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 14f))
                }
            }
        }

        val currentZoom = cameraPositionState.position.zoom
        // Show stops once zoomed in (or when the set is small, e.g. nearby/tracking); the
        // tracked stop is always drawn.
        val showStops = stops.size <= 80 || currentZoom >= STOP_MARKER_MIN_ZOOM
        val stopDot = remember { dotDescriptor(30, 0xFF37474F.toInt(), 0xFFFFFFFF.toInt()) }
        val trackedDot = remember { dotDescriptor(40, 0xFF4F0000.toInt(), 0xFFFFFFFF.toInt()) }

        stops.forEach { stop ->
            val isTracked = stop.stop_ref == trackedStopRef
            if (isTracked || showStops) {
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(stop.latitude, stop.longitude)),
                    icon = if (isTracked) trackedDot else stopDot,
                    anchor = Offset(0.5f, 0.5f),
                    title = stop.displayName(),
                    snippet = "Stop ${stop.stop_id}",
                    onClick = {
                        onStopClick?.invoke(stop)
                        true
                    }
                )
            }
        }

        positions.forEach { bus ->
            val markerColor = busColors[bus.timetable_id ?: ""] ?: palette[0]
            val dirIndex = routeHeadsigns[bus.timetable_id ?: ""]?.indexOf(bus.headsign) ?: -1
            val isTracked = bus.trip_duid == trackedTripId
            val route = bus.timetable_id?.takeIf { it.isNotEmpty() }
            val routeLabel = route ?: ""
            val title = if (route != null) "Route $route: ${bus.headsign ?: ""}" else (bus.headsign ?: "Bus")

            MarkerComposable(
                bus.markerKey, markerColor.value, dirIndex, isTracked, routeLabel,
                state = rememberUpdatedMarkerState(position = LatLng(bus.latitude, bus.longitude)),
                title = title,
                snippet = bus.vehicle_id?.let { "Vehicle $it" }
            ) {
                BusMarkerContent(markerColor, routeLabel, dirIndex == 1, isTracked)
            }
        }

        userLocation?.let { loc ->
            MarkerComposable(
                "user-location",
                state = rememberUpdatedMarkerState(position = LatLng(loc.lat, loc.lon))
            ) {
                UserLocationContent()
            }
        }
    }
}

/** A route-coloured marker: circle for direction 0, rounded square for direction 1. */
@Composable
private fun BusMarkerContent(color: Color, routeLabel: String, isSquare: Boolean, isTracked: Boolean) {
    val contentColor = if (color.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    val shape = if (isSquare) RoundedCornerShape(6.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(if (isTracked) 44.dp else 36.dp)
            .background(
                color = if (isTracked) Color(0x4D4F0000) else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(color, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = routeLabel.ifEmpty { "•" },
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** "You are here": a blue dot inside a white ring with a translucent halo. */
@Composable
private fun UserLocationContent() {
    Box(
        modifier = Modifier.size(28.dp).background(Color(0x2E1E88E5), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(16.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(11.dp).background(Color(0xFF1E88E5), CircleShape))
        }
    }
}

/** Small filled circle used as a stop marker icon. */
private fun dotDescriptor(sizePx: Int, fill: Int, stroke: Int): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val r = sizePx / 2f
    val strokeWidth = sizePx / 10f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = fill
    canvas.drawCircle(r, r, r - strokeWidth, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokeWidth
    paint.color = stroke
    canvas.drawCircle(r, r, r - strokeWidth / 2f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// Google's "night mode" style, trimmed — keeps the map legible in the app's dark theme.
private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType":"geometry","stylers":[{"color":"#242f3e"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},
  {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},
  {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},
  {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},
  {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},
  {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},
  {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]}
]
"""
