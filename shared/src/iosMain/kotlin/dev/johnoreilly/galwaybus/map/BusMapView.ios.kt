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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import dev.johnoreilly.galwaybus.localizedName
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGLineCap
import platform.CoreGraphics.CGLineJoin
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKFeatureDisplayPriorityDefaultLow
import platform.MapKit.MKFeatureDisplayPriorityRequired
import platform.MapKit.MKFeatureVisibility
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.addOverlay
import platform.MapKit.removeOverlay
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKUserLocation
import platform.UIKit.UIColor
import platform.UIKit.UIBezierPath
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
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
    onBusClick: ((BusLocation) -> Unit)?,
    userLocation: UserLocation?,
    polylines: List<List<MapPoint>>,
    stopEtas: Map<String, StopEta>
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
                // With a caller handling bus taps, the selection lives there and this renderer's
                // own info card would just duplicate it.
                controller.onBusClick = { bus ->
                    if (onBusClick != null) onBusClick(bus) else selectedBusKey = bus.markerKey
                }
                controller.onBusDeselect = { selectedBusKey = null }
                mapView.showsUserLocation = userLocation != null
                controller.sync(mapView, positions, stops, busColors, trackedTripId, trackedStopRef, userLocation, polylines, stopEtas)
            }
        )

        // Resolve against the latest positions so the card tracks live data and disappears if the
        // bus drops out of the feed.
        val selectedBus = selectedBusKey
            ?.takeIf { onBusClick == null }
            ?.let { key -> positions.firstOrNull { it.markerKey == key } }
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

/** MKPointAnnotation carrying the stop it represents (for tap handling), tracked state and ETA. */
private class StopAnnotation(
    val stop: Stop,
    val tracked: Boolean,
    val eta: StopEta? = null
) : MKPointAnnotation()

/** Tag identifying the ETA label subview, so a recycled annotation view drops the previous one. */
private const val STOP_ETA_TAG = 0x8B6

private var stopDotUpcoming: UIImage? = null
private var stopDotPassed: UIImage? = null

/** A stop as a small white-ringed dot, matching the weight the other renderers give it. */
@OptIn(ExperimentalForeignApi::class)
private fun stopDotImage(passed: Boolean): UIImage? {
    stopDotUpcoming?.takeIf { !passed }?.let { return it }
    stopDotPassed?.takeIf { passed }?.let { return it }
    val size = 14.0
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(size, size))
    val image = renderer.imageWithActions { _ ->
        val ring = UIBezierPath.bezierPathWithOvalInRect(CGRectMake(0.0, 0.0, size, size))
        UIColor.whiteColor.setFill()
        ring.fill()
        val inner = UIBezierPath.bezierPathWithOvalInRect(CGRectMake(2.0, 2.0, size - 4.0, size - 4.0))
        (if (passed) PASSED_STOP_COLOR else STOP_COLOR).setFill()
        inner.fill()
    }
    if (passed) stopDotPassed = image else stopDotUpcoming = image
    return image
}

/**
 * Writes a stop's predicted departure beside its pin. MapKit's own title is hidden (it renders as
 * unreadable text over the map), so the label rides as a subview instead.
 */
@OptIn(ExperimentalForeignApi::class)
private fun MKAnnotationView.applyStopEta(eta: StopEta?, tint: UIColor) {
    viewWithTag(STOP_ETA_TAG.toLong())?.removeFromSuperview()
    if (eta == null || eta.label.isBlank()) return  // passed stops are muted, not annotated
    val width = 36.0
    val height = 15.0
    // Centred under the marker rather than beside it. Hung off the right it ran off the edge of the
    // screen for stops near it, and collided with the map's own place names.
    // Built into a local rather than configured in an `apply`: inside that block the implicit
    // receiver is the label, so `addSubview(this)` added the label to itself — which UIKit rejects
    // outright ("Can't add self as subview"), taking the app down as the first stop was drawn.
    val label = UILabel(
        frame = CGRectMake(
            (CGRectGetWidth(bounds) - width) / 2.0,
            CGRectGetHeight(bounds) + 1.0,
            width,
            height
        )
    )
    label.tag = STOP_ETA_TAG.toLong()
    label.userInteractionEnabled = false
    label.text = eta.label
    label.font = UIFont.boldSystemFontOfSize(11.0)
    // Tinted to the route being tracked instead of an unrelated accent colour.
    label.textColor = if (eta.upcoming) tint else UIColor.grayColor
    label.backgroundColor = UIColor.whiteColor.colorWithAlphaComponent(0.92)
    label.textAlignment = NSTextAlignmentCenter
    label.layer.cornerRadius = 3.0
    label.clipsToBounds = true
    // The label hangs below the marker's own bounds, so the view must not crop its subviews.
    clipsToBounds = false
    addSubview(label)
}

@OptIn(ExperimentalForeignApi::class)
private fun Color.toUIColor(): UIColor =
    UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = 1.0)

private val STOP_COLOR = UIColor(red = 0.216, green = 0.278, blue = 0.310, alpha = 1.0) // #37474F
private val TRACKED_STOP_COLOR = UIColor(red = 0.31, green = 0.0, blue = 0.0, alpha = 1.0)
private val PASSED_STOP_COLOR = UIColor(red = 0.62, green = 0.62, blue = 0.62, alpha = 1.0)
private val ROUTE_LINE_CASING = UIColor.whiteColor.colorWithAlphaComponent(0.9)
private val ROUTE_LINE_COLOR = UIColor(red = 0.082, green = 0.396, blue = 0.753, alpha = 1.0)
/** Fallback when the tracked route has no colour yet — the app's own dark red, not an accent. */
private val DEFAULT_ETA_TINT = UIColor(red = 0.31, green = 0.0, blue = 0.0, alpha = 1.0)

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
    private var overlays: List<MKPolyline> = emptyList()
    private var casings: Set<MKPolyline> = emptySet()
    private var overlayKey: String? = null
    private var stopEtas: Map<String, StopEta> = emptyMap()
    // Times are tinted to the route being tracked, so they read as part of that line rather than
    // as an unrelated accent.
    private var etaTint: UIColor = DEFAULT_ETA_TINT
    private var hasCentered = false

    /**
     * Replaces the drawn geometry when it changes. Keyed on the lines themselves so panning and
     * refreshing don't tear the overlay down and rebuild it every few seconds.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun syncPolylines(mapView: MKMapView, polylines: List<List<MapPoint>>) {
        val key = polylines.joinToString("|") { "${it.size}:${it.firstOrNull()?.lat},${it.lastOrNull()?.lon}" }
        if (key == overlayKey) return
        overlayKey = key
        overlays.forEach { mapView.removeOverlay(it) }
        casings = emptySet()
        val drawn = polylines.filter { it.size >= 2 }.map { line ->
            memScoped {
                val coords = allocArray<CLLocationCoordinate2D>(line.size)
                line.forEachIndexed { i, p ->
                    coords[i].latitude = p.lat
                    coords[i].longitude = p.lon
                }
                // Two copies of the same geometry: a wide pale one underneath so the route reads
                // over Apple's blue water and grey roads, and the coloured line on top.
                val casing = MKPolyline.polylineWithCoordinates(coords, line.size.toULong())
                val top = MKPolyline.polylineWithCoordinates(coords, line.size.toULong())
                casing to top
            }
        }
        casings = drawn.map { it.first }.toSet()
        overlays = drawn.flatMap { listOf(it.first, it.second) }
        overlays.forEach { mapView.addOverlay(it) }
    }

    val delegate: MKMapViewDelegateProtocol = object : NSObject(), MKMapViewDelegateProtocol {
        override fun mapView(
            mapView: MKMapView,
            viewForAnnotation: platform.MapKit.MKAnnotationProtocol
        ): MKAnnotationView? {
            if (viewForAnnotation is MKUserLocation) return null // keep the native blue dot

            // An ordinary stop is a dot, not a pin. A balloon per stop buried the line and the bus
            // under a wall of teardrops; the dot is the same weight the other renderers give it.
            if (viewForAnnotation is StopAnnotation && !viewForAnnotation.tracked) {
                val dotId = "stopDot"
                val dot = mapView.dequeueReusableAnnotationViewWithIdentifier(dotId)
                    ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = dotId)
                dot.annotation = viewForAnnotation
                dot.image = stopDotImage(viewForAnnotation.eta?.upcoming == false)
                dot.canShowCallout = false
                dot.displayPriority = MKFeatureDisplayPriorityDefaultLow
                dot.applyStopEta(viewForAnnotation.eta, etaTint)
                return dot
            }

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
                    // Scaled up: the bus is why this screen is open, and it was smaller than the
                    // stops it travels between.
                    view.transform = CGAffineTransformMakeScale(1.2, 1.2)
                    view.applyHeadingNose(viewForAnnotation.busLocation.bearing, viewForAnnotation.color)
                    view.applyStopEta(null, etaTint)
                }
                // Only the tracked stop reaches here — every other stop is drawn as a dot above.
                is StopAnnotation -> {
                    // Bus and stop annotations share a reuse pool, so drop any nose the recycled
                    // view was carrying from its last life as a bus.
                    view.applyHeadingNose(null, STOP_COLOR)
                    view.markerTintColor = TRACKED_STOP_COLOR
                    view.glyphText = null
                    view.displayPriority = MKFeatureDisplayPriorityRequired
                    view.canShowCallout = true // stop name in the callout; tap also opens departures
                    view.titleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    view.subtitleVisibility = MKFeatureVisibility.MKFeatureVisibilityHidden
                    view.transform = CGAffineTransformIdentity.readValue()
                    view.applyStopEta(viewForAnnotation.eta, etaTint)
                }
            }
            return view
        }

        override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
            val renderer = MKPolylineRenderer(overlay = rendererForOverlay)
            val isCasing = casings.any { it === rendererForOverlay }
            renderer.strokeColor = if (isCasing) ROUTE_LINE_CASING else ROUTE_LINE_COLOR
            renderer.lineWidth = if (isCasing) 9.0 else 5.0
            renderer.lineCap = CGLineCap.kCGLineCapRound
            renderer.lineJoin = CGLineJoin.kCGLineJoinRound
            return renderer
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
        userLocation: UserLocation?,
        polylines: List<List<MapPoint>> = emptyList(),
        stopEtas: Map<String, StopEta> = emptyMap()
    ) {
        syncPolylines(mapView, polylines)
        this.stopEtas = stopEtas
        this.etaTint = positions.firstOrNull()?.timetable_id
            ?.let { busColors[it] }
            // Only dark route colours are readable as text on the pale label; 401's yellow all but
            // disappeared on it, so light routes fall back to the app's own dark red.
            ?.takeIf { it.luminance() < 0.5f }
            ?.toUIColor()
            ?: DEFAULT_ETA_TINT
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

        // --- Stops: keyed by ref + tracked state + ETA (re-add to restyle or relabel) ---
        val stopKeys = stops.associate { it.stop_ref to (it.stop_ref == trackedStopRef) }
        stopAnnotations.entries
            .filter { (ref, ann) -> stopKeys[ref] != ann.tracked || stopEtas[ref] != ann.eta }
            .toList()
            .forEach { (ref, ann) ->
                stopAnnotations.remove(ref)
                mapView.removeAnnotation(ann)
            }
        stops.forEach { stop ->
            if (stopAnnotations[stop.stop_ref] == null) {
                val annotation = StopAnnotation(stop, stop.stop_ref == trackedStopRef, stopEtas[stop.stop_ref]).apply {
                    setCoordinate(CLLocationCoordinate2DMake(stop.latitude, stop.longitude))
                    setTitle(stop.localizedName())
                    setSubtitle("Stop ${stop.stop_id}")
                }
                stopAnnotations[stop.stop_ref] = annotation
                mapView.addAnnotation(annotation)
            }
        }

        // --- Camera: frame the wait; otherwise centre once ---
        // When both the bus and the stop are known, show the span between them. Centring on the
        // bus in a fixed 2km window meant the thing being waited for was often off screen, and
        // half the map was whatever happened to be nearby — usually Galway Bay.
        val trackedBus = trackedTripId?.let { id -> positions.find { it.trip_duid == id } }
        val waitingAt = trackedStopRef?.let { ref -> stops.find { it.stop_ref == ref } }
        if (trackedBus != null && waitingAt != null) {
            val midLat = (trackedBus.latitude + waitingAt.latitude) / 2.0
            val midLon = (trackedBus.longitude + waitingAt.longitude) / 2.0
            // Padded so neither marker sits under the app bar or the arrival card, with a floor so
            // a bus about to pull in doesn't zoom to street level.
            val latSpan = maxOf(abs(trackedBus.latitude - waitingAt.latitude) * 2.2, 0.012)
            val lonSpan = maxOf(abs(trackedBus.longitude - waitingAt.longitude) * 2.2, 0.012)
            mapView.setRegion(
                MKCoordinateRegionMake(
                    CLLocationCoordinate2DMake(midLat, midLon),
                    MKCoordinateSpanMake(latSpan, lonSpan)
                ),
                animated = true
            )
            hasCentered = true
            return
        }

        // Resolved up front so a tracked trip with no bus in the feed yet falls through to the
        // stop below, instead of matching here and yielding no target at all.
        val followBus = trackedTripId?.let { id -> positions.find { it.trip_duid == id } }
        val target: Triple<Double, Double, Double>? = when {
            followBus != null -> Triple(followBus.latitude, followBus.longitude, 2000.0)
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
