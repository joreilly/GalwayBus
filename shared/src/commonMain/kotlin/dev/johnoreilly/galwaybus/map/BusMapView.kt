package dev.johnoreilly.galwaybus.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.johnoreilly.galwaybus.displayName
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlin.math.*

private const val TILE_PX = 256
private const val DEFAULT_ZOOM = 14
private const val MIN_ZOOM = 10
private const val MAX_ZOOM = 18
private const val STOP_MARKER_MIN_ZOOM = 13
private const val GALWAY_LAT = 53.2743
private const val GALWAY_LON = -9.0488

private data class TileId(val z: Int, val x: Int, val y: Int)

private data class LatLon(val lat: Double, val lon: Double)

// invert + hue-rotate(180°): tiles go dark while land/water/road hues stay roughly true
private val DarkTileFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.574f, -1.430f, -0.144f, 0f, 255f,
            -0.426f, -0.430f, -0.144f, 0f, 255f,
            -0.426f, -1.430f, 0.856f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

private val BusLocation.markerKey: String get() = vehicle_id ?: trip_duid

private fun lonToTileXf(lon: Double, zoom: Int): Double =
    (lon + 180.0) / 360.0 * (1 shl zoom)

private fun latToTileYf(lat: Double, zoom: Int): Double {
    val latRad = lat * PI / 180.0
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

private fun tileXfToLon(tileXf: Double, zoom: Int): Double =
    tileXf / (1 shl zoom) * 360.0 - 180.0

private fun tileYfToLat(tileYf: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * tileYf / (1 shl zoom)
    return 180.0 / PI * atan(sinh(n))
}

/**
 * Self-contained OpenStreetMap tile renderer drawn on a Compose [Canvas]. This is the
 * cross-platform fallback used on Desktop (JVM), where there is no native map SDK. On
 * Android and iOS the [BusMapView] `expect` resolves to a native map (Google / Apple) instead.
 */
@Composable
internal fun OsmBusMapView(
    positions: List<BusLocation>,
    modifier: Modifier = Modifier,
    stops: List<Stop> = emptyList(),
    trackedTripId: String? = null,
    trackedStopRef: String? = null,
    onStopClick: ((Stop) -> Unit)? = null,
    userLocation: UserLocation? = null
) {
    val tileClient = remember { HttpClient() }
    val tileImages = remember { mutableStateMapOf<TileId, ImageBitmap>() }
    val scope = rememberCoroutineScope()
    val fetching = remember { mutableSetOf<TileId>() }

    var zoom by remember { mutableStateOf(DEFAULT_ZOOM) }
    var centerLat by remember { mutableStateOf(GALWAY_LAT) }
    var centerLon by remember { mutableStateOf(GALWAY_LON) }

    var hasCentered by remember { mutableStateOf(false) }
    var hoveredBus by remember { mutableStateOf<BusLocation?>(null) }
    var hoveredStop by remember { mutableStateOf<Stop?>(null) }
    var hoverPos by remember { mutableStateOf(Offset.Zero) }

    // Positions arrive in ~30s polls; glide each marker to its new fix instead of jumping.
    val animatedPositions = remember { mutableStateMapOf<String, LatLon>() }
    LaunchedEffect(positions) {
        val targets = positions.associateBy { it.markerKey }
        animatedPositions.keys.retainAll(targets.keys)
        targets.forEach { (id, bus) ->
            val from = animatedPositions[id]
            val to = LatLon(bus.latitude, bus.longitude)
            when {
                from == null -> animatedPositions[id] = to
                from != to -> launch {
                    animate(0f, 1f, animationSpec = tween(1500, easing = LinearEasing)) { t, _ ->
                        animatedPositions[id] = LatLon(
                            from.lat + (to.lat - from.lat) * t,
                            from.lon + (to.lon - from.lon) * t
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(positions, trackedTripId, trackedStopRef, userLocation) {
        if (trackedTripId != null) {
            positions.find { it.trip_duid == trackedTripId }?.let { bus ->
                centerLat = bus.latitude
                centerLon = bus.longitude
                zoom = MAX_ZOOM - 1
                hasCentered = true
            }
        } else if (userLocation != null && !hasCentered) {
            centerLat = userLocation.lat
            centerLon = userLocation.lon
            zoom = STOP_MARKER_MIN_ZOOM + 2
            hasCentered = true
        } else if (trackedStopRef != null && !hasCentered) {
            stops.find { it.stop_ref == trackedStopRef }?.let { stop ->
                centerLat = stop.latitude
                centerLon = stop.longitude
                zoom = MAX_ZOOM - 1
                hasCentered = true
            }
        } else if (positions.isNotEmpty() && !hasCentered) {
            centerLat = positions.map { it.latitude }.average()
            centerLon = positions.map { it.longitude }.average()
            hasCentered = true
        }
    }

    val palette = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
        Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFC0CA33), Color(0xFFF4511E),
        Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF00897B), Color(0xFFFDD835),
        Color(0xFF5E35B1), Color(0xFF039BE5), Color(0xFF7CB342), Color(0xFFFFB300),
        Color(0xFF6D4C41), Color(0xFF546E7A)
    )

    // Colour encodes the route only; direction is shown by the marker's shape.
    val busColors = remember(positions) {
        positions.map { it.timetable_id ?: "" }
            .distinct()
            .sorted()
            .associateWith { route -> palette[abs(route.hashCode()) % palette.size] }
    }

    // Each route's distinct headsigns, sorted → a stable direction index per bus.
    // Direction 0 is drawn as a circle, direction 1 as a rounded square.
    val routeHeadsigns = remember(positions) {
        positions.groupBy { it.timetable_id ?: "" }
            .mapValues { (_, buses) -> buses.mapNotNull { it.headsign }.distinct().sorted() }
    }

    val busIcon = remember {
        ImageVector.Builder(
            name = "Bus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(4f, 16f)
            curveTo(4f, 16.88f, 4.39f, 17.67f, 5f, 18.22f)
            verticalLineTo(20f)
            curveTo(5f, 20.55f, 5.45f, 21f, 6f, 21f)
            horizontalLineTo(7f)
            curveTo(7.55f, 21f, 8f, 20.55f, 8f, 20f)
            verticalLineTo(19f)
            horizontalLineTo(16f)
            verticalLineTo(20f)
            curveTo(16f, 20.55f, 16.45f, 21f, 17f, 21f)
            horizontalLineTo(18f)
            curveTo(18.55f, 21f, 19f, 20.55f, 19f, 20f)
            verticalLineTo(18.22f)
            curveTo(19.61f, 17.67f, 20f, 16.88f, 20f, 16f)
            verticalLineTo(6f)
            curveTo(20f, 2.5f, 16.42f, 2f, 12f, 2f)
            reflectiveCurveTo(4f, 2.5f, 4f, 6f)
            verticalLineTo(16f)
            close()
            moveTo(7.5f, 17f)
            curveTo(6.67f, 17f, 6f, 16.33f, 6f, 15.5f)
            reflectiveCurveTo(6.67f, 14f, 7.5f, 14f)
            reflectiveCurveTo(9f, 14.67f, 9f, 15.5f)
            reflectiveCurveTo(8.33f, 17f, 7.5f, 17f)
            close()
            moveTo(16.5f, 17f)
            curveTo(15.67f, 17f, 15f, 16.33f, 15f, 15.5f)
            reflectiveCurveTo(15.67f, 14f, 16.5f, 14f)
            reflectiveCurveTo(18f, 14.67f, 18f, 15.5f)
            reflectiveCurveTo(17.33f, 17f, 16.5f, 17f)
            close()
            moveTo(18f, 11f)
            horizontalLineTo(6f)
            verticalLineTo(6f)
            horizontalLineTo(18f)
            verticalLineTo(11f)
            close()
        }.build()
    }
    val busPainter = rememberVectorPainter(busIcon)
    val tileColorFilter = if (isSystemInDarkTheme()) DarkTileFilter else null

    // Route number drawn inside each marker (falls back to the bus glyph when unknown).
    // Marker geometry is in raw px, so the label font is derived from px, not sp.
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = with(LocalDensity.current) {
        TextStyle(fontSize = 15f.toSp(), fontWeight = FontWeight.Bold)
    }
    val routeLabelLayouts = remember(positions, textMeasurer, labelStyle) {
        positions.mapNotNull { it.timetable_id }.filter { it.isNotEmpty() }.distinct()
            .associateWith { route -> textMeasurer.measure(AnnotatedString(route), labelStyle) }
    }

    Box(modifier.clipToBounds()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            // Zoom to newZoom keeping the geo point under `pivot` fixed on screen
            val zoomAt: (Offset, Int) -> Unit = { pivot, requestedZoom ->
                val newZoom = requestedZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
                if (newZoom != zoom) {
                    val pivotXf = lonToTileXf(centerLon, zoom) + (pivot.x - widthPx / 2.0) / TILE_PX
                    val pivotYf = latToTileYf(centerLat, zoom) + (pivot.y - heightPx / 2.0) / TILE_PX
                    val pivotLon = tileXfToLon(pivotXf, zoom)
                    val pivotLat = tileYfToLat(pivotYf, zoom)
                    val newCenterXf = lonToTileXf(pivotLon, newZoom) - (pivot.x - widthPx / 2.0) / TILE_PX
                    val newCenterYf = latToTileYf(pivotLat, newZoom) - (pivot.y - heightPx / 2.0) / TILE_PX
                    centerLon = tileXfToLon(newCenterXf, newZoom)
                    centerLat = tileYfToLat(newCenterYf, newZoom)
                    zoom = newZoom
                }
            }

            val centerTileXf = lonToTileXf(centerLon, zoom)
            val centerTileYf = latToTileYf(centerLat, zoom)
            val originTileXf = centerTileXf - widthPx / 2.0 / TILE_PX
            val originTileYf = centerTileYf - heightPx / 2.0 / TILE_PX

            val x0 = originTileXf.toInt()
            val y0 = originTileYf.toInt()
            val x1 = (originTileXf + widthPx / TILE_PX).toInt() + 1
            val y1 = (originTileYf + heightPx / TILE_PX).toInt() + 1

            LaunchedEffect(x0, y0, x1, y1, zoom) {
                for (tx in x0..x1) for (ty in y0..y1) {
                    val id = TileId(zoom, tx, ty)
                    if (id !in tileImages && id !in fetching) {
                        fetching.add(id)
                        scope.launch {
                            try {
                                val bytes = tileClient
                                    .get("https://tile.openstreetmap.org/${id.z}/${id.x}/${id.y}.png") {
                                        header("User-Agent", "GalwayBusApp/1.0")
                                    }.body<ByteArray>()
                                tileImages[id] = bytes.toImageBitmap()
                            } catch (_: Exception) {
                            } finally {
                                fetching.remove(id)
                            }
                        }
                    }
                }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        var pinchAccum = 1f
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            if (pan != Offset.Zero) {
                                val newXf = lonToTileXf(centerLon, zoom) - pan.x / TILE_PX
                                val newYf = latToTileYf(centerLat, zoom) - pan.y / TILE_PX
                                centerLon = tileXfToLon(newXf, zoom)
                                centerLat = tileYfToLat(newYf, zoom)
                            }
                            if (gestureZoom != 1f) {
                                pinchAccum *= gestureZoom
                                if (pinchAccum > 1.4f) {
                                    zoomAt(centroid, zoom + 1)
                                    pinchAccum = 1f
                                } else if (pinchAccum < 0.71f) {
                                    zoomAt(centroid, zoom - 1)
                                    pinchAccum = 1f
                                }
                            }
                        }
                    }
                    .pointerInput(positions, stops, trackedStopRef, onStopClick, widthPx, heightPx) {
                        detectTapGestures(
                            onDoubleTap = { pos -> zoomAt(pos, zoom + 1) },
                            onTap = tap@{ pos ->
                                if (onStopClick == null) return@tap
                                val originXf = lonToTileXf(centerLon, zoom) - (widthPx / 2.0 / TILE_PX)
                                val originYf = latToTileYf(centerLat, zoom) - (heightPx / 2.0 / TILE_PX)
                                // Nearest stop marker within touch range (generous slop for fingers)
                                val hit = stops
                                    .filter { zoom >= STOP_MARKER_MIN_ZOOM || it.stop_ref == trackedStopRef }
                                    .map { stop ->
                                        val sx = ((lonToTileXf(stop.longitude, zoom) - originXf) * TILE_PX).toFloat()
                                        val sy = ((latToTileYf(stop.latitude, zoom) - originYf) * TILE_PX).toFloat()
                                        val dx = pos.x - sx
                                        val dy = pos.y - sy
                                        stop to (dx * dx + dy * dy)
                                    }
                                    .filter { (_, d2) -> d2 < 28f * 28f }
                                    .minByOrNull { (_, d2) -> d2 }
                                hit?.let { (stop, _) -> onStopClick(stop) }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // Mouse scroll-wheel zoom (desktop)
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val change = event.changes.firstOrNull()
                                    val dy = change?.scrollDelta?.y ?: 0f
                                    if (change != null && dy != 0f) {
                                        zoomAt(change.position, zoom - dy.sign.toInt())
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(positions, stops, zoom, centerLat, centerLon, widthPx, heightPx) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                                    val position = event.changes.first().position
                                    hoverPos = position

                                    val centerTileXf = lonToTileXf(centerLon, zoom)
                                    val centerTileYf = latToTileYf(centerLat, zoom)
                                    val originTileXf = centerTileXf - (widthPx / 2.0 / TILE_PX)
                                    val originTileYf = centerTileYf - (heightPx / 2.0 / TILE_PX)

                                    var busFound: BusLocation? = null
                                    for (bus in positions) {
                                        // Hit-test where the marker is drawn (mid-animation), not its final fix
                                        val busPos = animatedPositions[bus.markerKey] ?: LatLon(bus.latitude, bus.longitude)
                                        val bx = ((lonToTileXf(busPos.lon, zoom) - originTileXf) * TILE_PX).toFloat()
                                        val by = ((latToTileYf(busPos.lat, zoom) - originTileYf) * TILE_PX).toFloat()
                                        val dx = position.x - bx
                                        val dy = position.y - by
                                        if (dx * dx + dy * dy < 30 * 30) {
                                            busFound = bus
                                            break
                                        }
                                    }
                                    hoveredBus = busFound

                                    if (busFound == null) {
                                        var stopFound: Stop? = null
                                        for (stop in stops) {
                                            val isTracked = stop.stop_ref == trackedStopRef
                                            if (zoom >= STOP_MARKER_MIN_ZOOM || isTracked) {
                                                val sx = ((lonToTileXf(stop.longitude, zoom) - originTileXf) * TILE_PX).toFloat()
                                                val sy = ((latToTileYf(stop.latitude, zoom) - originTileYf) * TILE_PX).toFloat()
                                                val dx = position.x - sx
                                                val dy = position.y - sy
                                                val radius = if (isTracked) 16f else 11f
                                                if (dx * dx + dy * dy < (radius + 8) * (radius + 8)) {
                                                    stopFound = stop
                                                    break
                                                }
                                            }
                                        }
                                        hoveredStop = stopFound
                                    } else {
                                        hoveredStop = null
                                    }
                                } else if (event.type == PointerEventType.Exit) {
                                    hoveredBus = null
                                    hoveredStop = null
                                }
                            }
                        }
                    }
            ) {
                for (tx in x0..x1) for (ty in y0..y1) {
                    val bitmap = tileImages[TileId(zoom, tx, ty)] ?: continue
                    val px = ((tx - originTileXf) * TILE_PX).toFloat()
                    val py = ((ty - originTileYf) * TILE_PX).toFloat()
                    drawImage(bitmap, topLeft = Offset(px, py), colorFilter = tileColorFilter)
                }

                stops.forEach { stop ->
                    val isTracked = stop.stop_ref == trackedStopRef
                    if (zoom >= STOP_MARKER_MIN_ZOOM || isTracked) {
                        val sx = ((lonToTileXf(stop.longitude, zoom) - originTileXf) * TILE_PX).toFloat()
                        val sy = ((latToTileYf(stop.latitude, zoom) - originTileYf) * TILE_PX).toFloat()
                        if (sx in -16f..size.width + 16f && sy in -16f..size.height + 16f) {
                            val radius = if (isTracked) 16f else 11f
                            val color = if (isTracked) Color(0xFF4f0000) else Color(0xFF37474F)
                            drawCircle(color, radius = radius, center = Offset(sx, sy))
                            drawCircle(
                                Color.White, radius = radius,
                                center = Offset(sx, sy), style = Stroke(width = 2.5f)
                            )
                        }
                    }
                }

                positions.forEach { bus ->
                    val drawPos = animatedPositions[bus.markerKey] ?: LatLon(bus.latitude, bus.longitude)
                    val bx = ((lonToTileXf(drawPos.lon, zoom) - originTileXf) * TILE_PX).toFloat()
                    val by = ((latToTileYf(drawPos.lat, zoom) - originTileYf) * TILE_PX).toFloat()
                    val isTracked = bus.trip_duid == trackedTripId
                    val markerColor = busColors[bus.timetable_id ?: ""] ?: palette[0]
                    // Direction index within the route (by sorted headsign); shapes the marker.
                    val dirIndex = routeHeadsigns[bus.timetable_id ?: ""]?.indexOf(bus.headsign) ?: -1

                    if (isTracked) {
                        drawCircle(Color(0xFF4f0000).copy(alpha = 0.3f), radius = 30f, center = Offset(bx, by))
                    }

                    val r = 20f
                    if (dirIndex == 1) {
                        // Second direction: rounded square
                        val side = Size(r * 2, r * 2)
                        val corner = CornerRadius(6f, 6f)
                        drawRoundRect(Color(0x99000000), topLeft = Offset(bx - r + 2f, by - r + 2f), size = side, cornerRadius = corner)
                        drawRoundRect(markerColor, topLeft = Offset(bx - r, by - r), size = side, cornerRadius = corner)
                    } else {
                        // First direction (or unknown headsign): circle
                        drawCircle(Color(0x99000000), radius = r + 2f, center = Offset(bx + 2f, by + 2f))
                        drawCircle(markerColor, radius = r, center = Offset(bx, by))
                    }

                    // Tint the marker content for contrast: dark on light markers (e.g. the
                    // yellow of route 401), white on dark ones. The tracked ring signals tracking.
                    val iconTint = if (markerColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
                    val label = routeLabelLayouts[bus.timetable_id]
                    if (label != null) {
                        drawText(
                            label,
                            color = iconTint,
                            topLeft = Offset(bx - label.size.width / 2f, by - label.size.height / 2f)
                        )
                    } else {
                        val iconSize = 24f
                        translate(bx - iconSize / 2, by - iconSize / 2) {
                            with(busPainter) {
                                draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(iconTint))
                            }
                        }
                    }
                }

                // "You are here" marker: a translucent halo behind a white-ringed blue dot.
                userLocation?.let { loc ->
                    val ux = ((lonToTileXf(loc.lon, zoom) - originTileXf) * TILE_PX).toFloat()
                    val uy = ((latToTileYf(loc.lat, zoom) - originTileYf) * TILE_PX).toFloat()
                    if (ux in -20f..size.width + 20f && uy in -20f..size.height + 20f) {
                        val accent = Color(0xFF1E88E5)
                        drawCircle(accent.copy(alpha = 0.18f), radius = 26f, center = Offset(ux, uy))
                        drawCircle(Color.White, radius = 10f, center = Offset(ux, uy))
                        drawCircle(accent, radius = 7f, center = Offset(ux, uy))
                    }
                }
            }

            // Hover info overlay
            (hoveredBus ?: hoveredStop)?.let { item ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (hoverPos.x + 16).toInt().coerceAtMost((widthPx - 160).toInt()),
                                (hoverPos.y + 16).toInt().coerceAtMost((heightPx - 80).toInt())
                            )
                        }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        if (item is BusLocation) {
                            val route = item.timetable_id ?: ""
                            val title = if (route.isNotEmpty()) "Route $route: ${item.headsign}" else (item.headsign ?: "Bus")
                            Text(title, style = MaterialTheme.typography.labelLarge)
                            Text("Vehicle: ${item.vehicle_id}", style = MaterialTheme.typography.bodySmall)
                        } else if (item is Stop) {
                            Text(item.displayName(), style = MaterialTheme.typography.labelLarge)
                            Text("Stop ID: ${item.stop_id}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Route legend – one row per route, shown only when there are 2+ routes.
        // Skip buses that report no route (their entry would be a blank labelled dot).
        val legendEntries = busColors.filterKeys { it.isNotEmpty() }
        if (legendEntries.size >= 2) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .widthIn(max = 220.dp)
                    .heightIn(max = 480.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                legendEntries.forEach { (route, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Route $route",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallFloatingActionButton(onClick = { if (zoom < MAX_ZOOM) zoom++ }) {
                Text("+", fontSize = 18.sp)
            }
            SmallFloatingActionButton(onClick = { if (zoom > MIN_ZOOM) zoom-- }) {
                Text("−", fontSize = 18.sp)
            }
        }

    }
}
