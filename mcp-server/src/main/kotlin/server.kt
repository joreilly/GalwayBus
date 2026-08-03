import dev.johnoreilly.galwaybus.GalwayBusRepository
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.OutputStream
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// The current shared module has no DI container; the repository builds its own Ktor client and
// points at the Cloud Run backend, so we instantiate it directly (matching how the apps do it).
private val repository = GalwayBusRepository()

// Galway city centre — default anchor for "nearest stops" when no coordinates are supplied.
private const val DEFAULT_LAT = 53.2743394
private const val DEFAULT_LON = -9.0514163

fun configureServer(): Server {
    val server = Server(
        Implementation(name = "GalwayBus MCP Server", version = "1.0.0"),
        ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true)))
    )

    server.addTool("get-bus-routes", "List all Galway bus routes") {
        toolResult("getting bus routes") {
            repository.getRoutes().values.sortedBy { it.short_name }
                .map { "${it.short_name} — ${it.long_name}" }
        }
    }

    server.addTool(
        "get-nearest-stops",
        "List the bus stops nearest to a location (defaults to Galway city centre)",
        schema("latitude" to "number", "longitude" to "number")
    ) { request ->
        val lat = request.arg("latitude")?.toDoubleOrNull() ?: DEFAULT_LAT
        val lon = request.arg("longitude")?.toDoubleOrNull() ?: DEFAULT_LON
        toolResult("getting nearest stops") {
            repository.getStops()
                .map { it to haversineMeters(lat, lon, it.latitude, it.longitude) }
                .sortedBy { it.second }
                .take(10)
                .map { (stop, dist) -> "${stop.short_name} (stop ${stop.stop_id}) — ${dist.roundToInt()}m" }
        }
    }

    server.addTool(
        "get-bus-departures",
        "List upcoming departures for a bus stop, given its stop id",
        schema("stopId" to "string", required = listOf("stopId"))
    ) { request ->
        val stopId = request.arg("stopId") ?: return@addTool missing("stopId")
        toolResult("getting bus departures") {
            val departures = repository.getStopDepartures(stopId)
            if (departures.isEmpty()) listOf("No upcoming departures for stop $stopId.")
            else departures.map { "${it.timetable_id} → ${it.display_name} at ${it.depart_timestamp ?: "scheduled"}" }
        }
    }

    server.addTool(
        "get-route-stops",
        "List the stops served by a bus route, given its route id (e.g. 401)",
        schema("routeId" to "string", required = listOf("routeId"))
    ) { request ->
        val routeId = request.arg("routeId") ?: return@addTool missing("routeId")
        toolResult("getting route stops") {
            val directions = repository.getStopsForRoute(routeId)
            val headsigns = repository.getDirectionHeadsigns(routeId)
            buildList {
                directions.forEachIndexed { i, stops ->
                    if (stops.isEmpty()) return@forEachIndexed
                    add("── Towards ${headsigns.getOrNull(i) ?: "Direction ${i + 1}"} (${stops.size} stops) ──")
                    stops.forEach { add("  ${it.short_name} (stop ${it.stop_id}) @ (${fmtCoord(it.latitude)}, ${fmtCoord(it.longitude)})") }
                }
            }.ifEmpty { listOf("No stops found for route $routeId.") }
        }
    }

    server.addTool(
        "get-live-buses",
        "List live bus positions, optionally filtered to a single route id (e.g. 401)",
        schema("routeId" to "string")
    ) { request ->
        val routeId = request.arg("routeId")
        toolResult("getting live buses") {
            val buses = if (routeId != null) {
                repository.getBusPositions(routeId).map { routeId to it }
            } else {
                repository.getBusPositions().flatMap { (route, list) -> list.map { route to it } }
            }
            if (buses.isEmpty()) {
                listOf(if (routeId != null) "No live buses for route $routeId." else "No live buses right now.")
            } else {
                buses.map { (route, bus) ->
                    val veh = bus.vehicle_id?.let { " veh $it" } ?: ""
                    "$route → ${bus.headsign ?: "?"} @ (${fmtCoord(bus.latitude)}, ${fmtCoord(bus.longitude)})$veh (updated ${bus.modified_timestamp})"
                }
            }
        }
    }

    server.addTool(
        "search-stops",
        "Find bus stops whose name contains the given text (e.g. 'Eyre Square')",
        schema("query" to "string", required = listOf("query"))
    ) { request ->
        val query = request.arg("query")?.trim()
        if (query.isNullOrEmpty()) return@addTool missing("query")
        toolResult("searching stops") {
            val matches = repository.getStops().filter {
                it.long_name.contains(query, ignoreCase = true) || it.short_name.contains(query, ignoreCase = true)
            }.take(20)
            if (matches.isEmpty()) listOf("No stops matching \"$query\".")
            else matches.map { "${it.short_name} (stop ${it.stop_id}) — ${it.long_name}" }
        }
    }

    server.addTool(
        "get-departures-with-live",
        "Upcoming departures for a stop, merged with live tracking (marks live buses and delays)",
        schema("stopId" to "string", required = listOf("stopId"))
    ) { request ->
        val stopId = request.arg("stopId") ?: return@addTool missing("stopId")
        toolResult("getting live departures") {
            val departures = repository.getStopDeparturesWithLive(stopId).first
            if (departures.isEmpty()) listOf("No upcoming departures for stop $stopId.")
            else departures.map {
                val live = if (it.vehicleId != null) " [live]" else ""
                val delay = it.delaySeconds?.let(::delayLabel) ?: ""
                "${it.timetable_id} → ${it.display_name} at ${it.depart_timestamp ?: "scheduled"}$live$delay"
            }
        }
    }

    return server
}

// ── Tool helpers ────────────────────────────────────────────────────────────

/** Wraps text lines as a tool result (one [TextContent] block per line). */
private fun textResult(lines: List<String>): CallToolResult =
    CallToolResult(content = lines.map { TextContent(it) })

private fun textResult(vararg lines: String): CallToolResult = textResult(lines.asList())

private fun missing(param: String): CallToolResult = textResult("The '$param' parameter is required.")

/** Reads a string argument from the request, or null if absent. */
private fun CallToolRequest.arg(name: String): String? = arguments?.get(name)?.jsonPrimitive?.content

/** Builds a tool input schema from (name -> JSON-schema type) pairs. */
private fun schema(vararg props: Pair<String, String>, required: List<String> = emptyList()): ToolSchema =
    ToolSchema(
        properties = buildJsonObject { props.forEach { (name, type) -> putJsonObject(name) { put("type", type) } } },
        required = required.ifEmpty { null }
    )

/** Runs a tool body, mapping its lines to text content and any thrown exception to an error message. */
private inline fun toolResult(context: String, block: () -> List<String>): CallToolResult =
    runCatching { block() }.fold(
        onSuccess = { textResult(it) },
        onFailure = { textResult("Error $context: ${it.message}") }
    )

private fun delayLabel(delaySeconds: Int): String = when {
    delaySeconds > 60 -> " (${delaySeconds / 60}m late)"
    delaySeconds < -60 -> " (${-delaySeconds / 60}m early)"
    else -> " (on time)"
}

/** Formats a coordinate to 5 decimal places (~1 m), always with a dot decimal separator. */
private fun fmtCoord(coord: Double): String = String.format(Locale.US, "%.5f", coord)

/** Great-circle distance in metres between two lat/lon points. */
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun Double.toRadians(): Double = this * PI / 180.0

// ── Transports ──────────────────────────────────────────────────────────────

/**
 * Runs an MCP server over standard input/output — the transport used when a desktop MCP client
 * (e.g. Claude Desktop) launches this jar directly. [protocolOut] is the real stdout captured
 * before System.out was redirected to stderr, so only JSON-RPC reaches the client.
 */
fun runMcpServerUsingStdio(protocolOut: OutputStream) {
    val server = configureServer()
    val transport = StdioServerTransport(
        input = System.`in`.asSource().buffered(),
        output = protocolOut.asSink().buffered()
    ) {}

    runBlocking {
        // createSession connects (and starts) the transport; it returns once the client disconnects.
        val session = server.createSession(transport)
        val done = Job()
        session.onClose { done.complete() }
        done.join()
    }
}

/** Launches an SSE (Server-Sent Events) MCP server on [port], letting clients connect over HTTP. */
fun runSseMcpServer(port: Int): Unit = runBlocking {
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp { configureServer() }
    }.start(wait = true)
}
