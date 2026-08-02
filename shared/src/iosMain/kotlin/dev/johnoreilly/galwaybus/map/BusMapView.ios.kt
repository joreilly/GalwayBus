package dev.johnoreilly.galwaybus.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKFeatureDisplayPriorityDefaultLow
import platform.MapKit.MKFeatureDisplayPriorityRequired
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKUserLocation
import platform.UIKit.UIColor
import platform.darwin.NSObject
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

    // Colour per route; direction (sorted-headsign index) chooses the marker glyph shape.
    val busColors = positions.map { it.timetable_id ?: "" }.distinct().sorted()
        .associateWith { route -> palette[abs(route.hashCode()) % palette.size] }

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
        modifier = modifier,
        update = { mapView ->
            controller.onStopClick = onStopClick
            mapView.showsUserLocation = userLocation != null
            controller.sync(mapView, positions, stops, busColors, trackedTripId, trackedStopRef, userLocation)
        }
    )
}

/** MKPointAnnotation carrying the bus and its display colour/glyph. */
private class BusAnnotation(val color: UIColor, val glyph: String?) : MKPointAnnotation()

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
            view.canShowCallout = true
            when (viewForAnnotation) {
                is BusAnnotation -> {
                    view.markerTintColor = viewForAnnotation.color
                    view.glyphText = viewForAnnotation.glyph
                    view.displayPriority = MKFeatureDisplayPriorityRequired
                    view.transform = CGAffineTransformIdentity.readValue()
                }
                is StopAnnotation -> {
                    view.markerTintColor = if (viewForAnnotation.tracked) TRACKED_STOP_COLOR else STOP_COLOR
                    view.glyphText = null
                    view.displayPriority = MKFeatureDisplayPriorityDefaultLow
                    val scale = if (viewForAnnotation.tracked) 1.5 else 1.35
                    view.transform = CGAffineTransformMakeScale(scale, scale)
                }
            }
            return view
        }

        override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
            (didSelectAnnotationView.annotation as? StopAnnotation)?.let { onStopClick?.invoke(it.stop) }
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
                val annotation = BusAnnotation(color, glyph).apply {
                    setCoordinate(CLLocationCoordinate2DMake(bus.latitude, bus.longitude))
                    setTitle(if (route != null) "Route $route: ${bus.headsign ?: ""}" else (bus.headsign ?: "Bus"))
                    setSubtitle(bus.vehicle_id?.let { "Vehicle $it" })
                }
                busAnnotations[bus.markerKey] = annotation
                mapView.addAnnotation(annotation)
            } else {
                existing.setCoordinate(CLLocationCoordinate2DMake(bus.latitude, bus.longitude))
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
                    setTitle(stop.long_name)
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
