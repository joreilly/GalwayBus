package dev.johnoreilly.galwaybus.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun BusMapView(
    positions: List<BusLocation>,
    modifier: Modifier = Modifier,
    stops: List<Stop> = emptyList(),
    trackedTripId: String? = null,
    trackedStopRef: String? = null
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

    LaunchedEffect(positions, trackedTripId, trackedStopRef) {
        if (trackedTripId != null) {
            positions.find { it.trip_duid == trackedTripId }?.let { bus ->
                centerLat = bus.latitude
                centerLon = bus.longitude
                zoom = MAX_ZOOM - 1
                hasCentered = true
            }
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

    val busColors = remember(positions) {
        positions.map { (it.timetable_id ?: "") to (it.headsign ?: "") }
            .distinct()
            .sortedBy { it.first + it.second }
            .associateWith { (route, headsign) ->
                val hash = (route + headsign).hashCode()
                palette[abs(hash) % palette.size]
            }
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
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { pos -> zoomAt(pos, zoom + 1) })
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
                                        val bx = ((lonToTileXf(bus.longitude, zoom) - originTileXf) * TILE_PX).toFloat()
                                        val by = ((latToTileYf(bus.latitude, zoom) - originTileYf) * TILE_PX).toFloat()
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
                                                val radius = if (isTracked) 12f else 8f
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
                    drawImage(bitmap, topLeft = Offset(px, py))
                }

                stops.forEach { stop ->
                    val isTracked = stop.stop_ref == trackedStopRef
                    if (zoom >= STOP_MARKER_MIN_ZOOM || isTracked) {
                        val sx = ((lonToTileXf(stop.longitude, zoom) - originTileXf) * TILE_PX).toFloat()
                        val sy = ((latToTileYf(stop.latitude, zoom) - originTileYf) * TILE_PX).toFloat()
                        if (sx in -16f..size.width + 16f && sy in -16f..size.height + 16f) {
                            val radius = if (isTracked) 12f else 8f
                            val color = if (isTracked) Color(0xFFA80050) else Color.White
                            drawCircle(color, radius = radius, center = Offset(sx, sy))
                            drawCircle(
                                Color(0xFF555555), radius = radius,
                                center = Offset(sx, sy), style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                positions.forEach { bus ->
                    val bx = ((lonToTileXf(bus.longitude, zoom) - originTileXf) * TILE_PX).toFloat()
                    val by = ((latToTileYf(bus.latitude, zoom) - originTileYf) * TILE_PX).toFloat()
                    val isTracked = bus.trip_duid == trackedTripId
                    val markerColor = busColors[(bus.timetable_id ?: "") to (bus.headsign ?: "")] ?: palette[0]
                    
                    if (isTracked) {
                        drawCircle(Color(0xFFA80050).copy(alpha = 0.3f), radius = 30f, center = Offset(bx, by))
                    }
                    
                    drawCircle(Color(0x99000000), radius = 22f, center = Offset(bx + 2f, by + 2f))
                    drawCircle(markerColor, radius = 20f, center = Offset(bx, by))

                    val iconSize = 24f
                    translate(bx - iconSize / 2, by - iconSize / 2) {
                        with(busPainter) {
                            draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(if (isTracked) Color.Yellow else Color.White))
                        }
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
                            Text(item.long_name, style = MaterialTheme.typography.labelLarge)
                            Text("Stop ID: ${item.stop_id}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Route/Direction legend – only shown when there are 2+ distinct combinations
        if (busColors.size >= 2) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .widthIn(max = 180.dp)
                    .heightIn(max = 240.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                busColors.forEach { (pair, color) ->
                    val (route, headsign) = pair
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (route.isNotEmpty()) "$route: $headsign" else headsign,
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
