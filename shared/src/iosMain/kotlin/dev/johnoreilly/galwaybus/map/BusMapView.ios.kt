package dev.johnoreilly.galwaybus.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import dev.johnoreilly.galwaybus.localizedName
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKFeatureDisplayPriorityDefaultLow
import platform.MapKit.MKFeatureDisplayPriorityRequired
import platform.MapKit.MKFeatureVisibility
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKUserLocation
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIFont
import platform.darwin.NSObject
import kotlin.math.PI
import kotlin.math.abs

private const val GALWAY_LAT = 53.2743
private const val GALWAY_LON = -9.0488

private val BusLocation.markerKey: String get() = vehicle_id ?: trip_duid

// Route palette shared with the other renderers so colours are consistent across platforms.
private val palette = listOf(
    0xE53935, 0x1E88E5, 0x43A047, 0xFB8C00, 0x8E24AA, 0x00ACC1, 0xC0CA33, 0xF4511E,
    0xD81B60, 0x3949AB, 0x00897B, 0xFDD835, 0x5E35B1, 0x039BE5, 0x7CB342, 0xFFB300,
    0x6D4C41, 0x546E7A
).map { rgb -> Color(0xFF000000 or rgb.toLong()) }

/**
 * iOS map backed by Apple Maps (MapKit / [MKMapView]) embedded via [UIKitView]. Buses and stops
 * are [MKPointAnnotation]s rendered as coloured [MKMarkerAnnotationView]s; the device location is
 * the native blue dot. A [BusMapController] reconciles annotations across recompositions and owns
 * the map delegate (which MKMapView holds only weakly).
 *
 * Tapping a bus surfaces its route/headsign/vehicle in a Compose [BusInfoCard] overlaid on the map
 * rather than a native MKMarkerAnnotationView callout — the callout bubble doesn't render reliably
 * over the Compose-hosted map (it leaks as unreadable label text), so we draw our own card.
 */
@OptIn(ExperimentalForeignApi::class)
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
    val controller = remember { BusMapController() }
    // markerKey of the bus whose info card is showing (null = none). Cleared when the map
    // deselects the annotation (a tap on empty map or another marker).
    var selectedBusKey by remember { mutableStateOf<String?>(null) }

    // Colour per route; direction (sorted-headsign index) chooses the marker glyph shape.
    val busColors = positions.map { it.timetable_id ?: "" }.distinct().sorted()
        .associateWith { route -> palette[abs(route.hashCode()) % palette.size] }

    Box(modifier) {
        UIKitView(
            factory = {
                MKMapView().apply {
                    setDelegate(controller.delegate)
                    showsUserLocation = userLocation != null
                    setRegion(
                        MKCoordinateRegionMakeWithDistance(
                            CLLocationCoordinate2DMake(GALWAY_LAT, GALWAY_LON), 6000.0, 6000.0
                        ),
                        animated = false
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                controller.onStopClick = onStopClick
                controller.onBusClick = { bus -> selectedBusKey = bus.markerKey }
                controller.onBusDeselect = { selectedBusKey = null }
                mapView.showsUserLocation = userLocation != null
                controller.sync(mapView, positions, stops, busColors, trackedTripId, trackedStopRef, userLocation)
            }
        )

        // Resolve against the latest positions so the card tracks live data and disappears if the
        // bus drops out of the feed.
        val selectedBus = selectedBusKey?.let { key -> positions.firstOrNull { it.markerKey == key } }
        selectedBus?.let { bus ->
            BusInfoCard(
                bus = bus,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
            )
        }
    }
}

/** Floating card with the tapped bus's route, destination and vehicle id. */
@Composable
private fun BusInfoCard(bus: BusLocation, modifier: Modifier = Modifier) {
    val route = bus.timetable_id?.takeIf { it.isNotEmpty() }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (route != null) "Route $route" else "Bus",
                style = MaterialTheme.typography.titleMedium
            )
            bus.headsign?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            bus.vehicle_id?.let {
                Text(
                    "Vehicle $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** MKPointAnnotation carrying the bus (for the info card) and its display colour/glyph. */
private class BusAnnotation(val color: UIColor, val glyph: String?, var busLocation: BusLocation) : MKPointAnnotation()

/** Tag identifying the heading-nose subview, so a recycled annotation view can drop the old one. */
private const val HEADING_NOSE_TAG = 0x8B5

/** How far the marker balloon's centre sits above the annotation view's own vertical midpoint. */
private const val BALLOON_CENTRE_LIFT = 10.0

/**
 * Points a small triangle the way the vehicle is travelling. MapKit has no equivalent of the other
 * renderers' drawn nose, so it rides as a subview that orbits the marker: the container is centred
 * on the view and rotated, carrying the glyph at its top edge around with it. NTA quantises bearing
 * to 45-degree steps, so this is an 8-point indicator.
 */
@OptIn(ExperimentalForeignApi::class)
private fun MKAnnotationView.applyHeadingNose(bearing: Float?, color: UIColor) {
    viewWithTag(HEADING_NOSE_TAG.toLong())?.removeFromSuperview()
    if (bearing == null) return
    val orbit = 42.0 // diameter; the nose then sits just off the balloon's edge, not floating
    // MapKit may not have laid the view out yet, in which case its bounds are empty — fall back to
    // MKMarkerAnnotationView's own default size so the nose still orbits the balloon.
    val hostWidth = CGRectGetWidth(bounds).takeIf { it > 0.0 } ?: 40.0
    val hostHeight = CGRectGetHeight(bounds).takeIf { it > 0.0 } ?: 40.0
    val container = UIView(frame = CGRectMake(0.0, 0.0, orbit, orbit)).apply {
        tag = HEADING_NOSE_TAG.toLong()
        userInteractionEnabled = false
        // The view's vertical midpoint sits below the balloon, because the bounds also cover the
        // pin tip; lift the orbit so the nose circles the balloon rather than the whole pin.
        setCenter(CGPointMake(hostWidth / 2.0, hostHeight / 2.0 - BALLOON_CENTRE_LIFT))
    }
    val glyphSize = 16.0
    UILabel(frame = CGRectMake((orbit - glyphSize) / 2.0, 0.0, glyphSize, glyphSize)).apply {
        text = "\u25B2" // black up-pointing triangle
        font = UIFont.boldSystemFontOfSize(13.0)
        textColor = color
        textAlignment = NSTextAlignmentCenter
        container.addSubview(this)
    }
    container.transform = CGAffineTransformMakeRotation(bearing * PI / 180.0)
    addSubview(container)
    sendSubviewToBack(container)
}

/** MKPointAnnotation carrying the stop it represents (for tap handling) and tracked state. */
private class StopAnnotation(val stop: Stop, val tracked: Boolean) : MKPointAnnotation()

@OptIn(ExperimentalForeignApi::class)
private fun Color.toUIColor(): UIColor =
    UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = 1.0)

private val STOP_COLOR = UIColor(red = 0.216, green = 0.278, blue = 0.310, alpha = 1.0) // #37474F
private val TRACKED_STOP_COLOR = UIColor(red = 0.31, green = 0.0, blue = 0.0, alpha = 1.0)

/**
 * Owns the annotations currently on the map and reconciles them against the latest data on each
 * [sync], so unchanged markers stay put (no per-poll flicker). Also drives camera centring.
 */
@OptIn(ExperimentalForeignApi::class)
private class BusMapController {
    var onStopClick: ((Stop) -> Unit)? = null
    var onBusClick: ((BusLocation) -> Unit)? = null
    var onBusDeselect: (() -> Unit)? = null

    private val busAnnotations = HashMap<String, BusAnnotation>()
    private val stopAnnotations = HashMap<String, StopAnnotation>()
    private var hasCentered = false

    val delegate: MKMapViewDelegateProtocol = object : NSObject(), MKMapViewDelegateProtocol {
        override fun mapView(
            mapView: MKMapView,
            viewForAnnotation: platform.MapKit.MKAnnotationProtocol
        ): MKAnnotationView? {
            if (viewForAnnotation is MKUserLocation) return null // keep the native blue dot
            val reuseId = "busStopMarker"
            val view = (mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId) as? MKMarkerAnnotationView)
                ?: MKMarkerAnnotationView(annotation = viewForAnnotation, reuseIdentifier = reuseId)
            view.annotation = viewForAnnotation
            when (viewForAnnotation) {
                is BusAnnotation -> {
                    view.markerTintColor = viewForAnnotation.color
                    view.glyphText = viewForAnnotation.glyph
                    view.displayPriority = MKFeatureDisplayPriorityRequired
                    // Bus info is shown in the Compose BusInfoCard on tap, so suppress MapKit's own
                    // callout and title label (the label leaks as unreadable text over the map).
                    view.canShowCallout = false
                    view.titleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    view.subtitleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    view.transform = CGAffineTransformIdentity.readValue()
                    view.applyHeadingNose(viewForAnnotation.busLocation.bearing, viewForAnnotation.color)
                }
                is StopAnnotation -> {
                    // Bus and stop annotations share a reuse pool, so drop any nose the recycled
                    // view was carrying from its last life as a bus.
                    view.applyHeadingNose(null, STOP_COLOR)
                    view.markerTintColor = if (viewForAnnotation.tracked) TRACKED_STOP_COLOR else STOP_COLOR
                    view.glyphText = null
                    view.displayPriority = MKFeatureDisplayPriorityDefaultLow
                    view.canShowCallout = true // stop name in the callout; tap also opens departures
                    view.titleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    view.subtitleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    // Stops are secondary to buses and there are many of them, so keep the markers small.
                    val scale = if (viewForAnnotation.tracked) 0.9 else 0.6
                    view.transform = CGAffineTransformMakeScale(scale, scale)
                }
            }
            return view
        }

        @ObjCSignatureOverride
        override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
            when (val annotation = didSelectAnnotationView.annotation) {
                is StopAnnotation -> onStopClick?.invoke(annotation.stop)
                is BusAnnotation -> onBusClick?.invoke(annotation.busLocation)
            }
        }

        @ObjCSignatureOverride
        override fun mapView(mapView: MKMapView, didDeselectAnnotationView: MKAnnotationView) {
            if (didDeselectAnnotationView.annotation is BusAnnotation) onBusDeselect?.invoke()
        }
    }

    fun sync(
        mapView: MKMapView,
        positions: List<BusLocation>,
        stops: List<Stop>,
        busColors: Map<String, Color>,
        trackedTripId: String?,
        trackedStopRef: String?,
        userLocation: UserLocation?
    ) {
        // Distinct headsigns per route → a stable direction index; direction 1 is glyphed "»".
        val routeHeadsigns = positions.groupBy { it.timetable_id ?: "" }
            .mapValues { (_, buses) -> buses.mapNotNull { it.headsign }.distinct().sorted() }

        // --- Buses: add new, move existing, drop gone ---
        val busKeys = positions.map { it.markerKey }.toSet()
        (busAnnotations.keys - busKeys).forEach { key ->
            busAnnotations.remove(key)?.let { mapView.removeAnnotation(it) }
        }
        positions.forEach { bus ->
            val color = (busColors[bus.timetable_id ?: ""] ?: palette[0]).toUIColor()
            val route = bus.timetable_id?.takeIf { it.isNotEmpty() }
            val dirIndex = routeHeadsigns[bus.timetable_id ?: ""]?.indexOf(bus.headsign) ?: -1
            val glyph = route ?: if (dirIndex == 1) "»" else "•"
            val existing = busAnnotations[bus.markerKey]
            if (existing == null) {
                val annotation = BusAnnotation(color, glyph, bus).apply {
                    setCoordinate(CLLocationCoordinate2DMake(bus.latitude, bus.longitude))
                    setTitle(if (route != null) "Route $route: ${bus.headsign ?: ""}" else (bus.headsign ?: "Bus"))
                    setSubtitle(bus.vehicle_id?.let { "Vehicle $it" })
                }
                busAnnotations[bus.markerKey] = annotation
                mapView.addAnnotation(annotation)
            } else {
                val turned = existing.busLocation.bearing != bus.bearing
                existing.busLocation = bus
                existing.setCoordinate(CLLocationCoordinate2DMake(bus.latitude, bus.longitude))
                if (turned) {
                    mapView.viewForAnnotation(existing)?.applyHeadingNose(bus.bearing, existing.color)
                }
            }
        }

        // --- Stops: keyed by ref + tracked state (re-add if tracking changed to restyle) ---
        val stopKeys = stops.associate { it.stop_ref to (it.stop_ref == trackedStopRef) }
        stopAnnotations.entries.filter { (ref, ann) -> stopKeys[ref] != ann.tracked }
            .toList()
            .forEach { (ref, ann) ->
                stopAnnotations.remove(ref)
                mapView.removeAnnotation(ann)
            }
        stops.forEach { stop ->
            if (stopAnnotations[stop.stop_ref] == null) {
                val annotation = StopAnnotation(stop, stop.stop_ref == trackedStopRef).apply {
                    setCoordinate(CLLocationCoordinate2DMake(stop.latitude, stop.longitude))
                    setTitle(stop.localizedName())
                    setSubtitle("Stop ${stop.stop_id}")
                }
                stopAnnotations[stop.stop_ref] = annotation
                mapView.addAnnotation(annotation)
            }
        }

        // --- Camera: follow a tracked bus; otherwise centre once ---
        val target: Triple<Double, Double, Double>? = when {
            trackedTripId != null ->
                positions.find { it.trip_duid == trackedTripId }
                    ?.let { Triple(it.latitude, it.longitude, 2000.0) }
            userLocation != null && !hasCentered ->
                Triple(userLocation.lat, userLocation.lon, 3000.0)
            trackedStopRef != null && !hasCentered ->
                stops.find { it.stop_ref == trackedStopRef }
                    ?.let { Triple(it.latitude, it.longitude, 2000.0) }
            positions.isNotEmpty() && !hasCentered ->
                Triple(positions.map { it.latitude }.average(), positions.map { it.longitude }.average(), 6000.0)
            else -> null
        }
        target?.let { (lat, lon, meters) ->
            hasCentered = true
            mapView.setRegion(
                MKCoordinateRegionMakeWithDistance(CLLocationCoordinate2DMake(lat, lon), meters, meters),
                animated = true
            )
        }
    }
}
