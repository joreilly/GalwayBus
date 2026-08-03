import dev.johnoreilly.galwaybus.GalwayBusRepository
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// The current shared module has no DI container; the repository builds its own Ktor client and
// points at the Cloud Run backend, so we instantiate it directly (matching how the apps do it).
private val galwayBusRepository = GalwayBusRepository()

// Galway city centre — default anchor for "nearest stops" when no coordinates are supplied.
private const val DEFAULT_LAT = 53.2743394
private const val DEFAULT_LON = -9.0514163

fun configureServer(): Server {
    val server = Server(
        Implementation(name = "GalwayBus MCP Server", version = "1.0.0"),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    server.addTool(
        name = "get-bus-routes",
        description = "List all Galway bus routes"
    ) {
        runCatching { galwayBusRepository.getRoutes() }.fold(
            onSuccess = { routes ->
                CallToolResult(
                    content = routes.values
                        .sortedBy { it.short_name }
                        .map { TextContent("${it.short_name} — ${it.long_name}") }
                )
            },
            onFailure = { CallToolResult(content = listOf(TextContent("Error getting bus routes: ${it.message}"))) }
        )
    }

    server.addTool(
        name = "get-nearest-stops",
        description = "List the bus stops nearest to a location (defaults to Galway city centre)",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("latitude") { put("type", "number") }
                putJsonObject("longitude") { put("type", "number") }
            }
        )
    ) { request ->
        val lat = request.arguments?.get("latitude")?.jsonPrimitive?.content?.toDoubleOrNull() ?: DEFAULT_LAT
        val lon = request.arguments?.get("longitude")?.jsonPrimitive?.content?.toDoubleOrNull() ?: DEFAULT_LON
        runCatching { galwayBusRepository.getStops() }.fold(
            onSuccess = { stops ->
                val nearest = stops
                    .sortedBy { haversineMeters(lat, lon, it.latitude, it.longitude) }
                    .take(10)
                CallToolResult(
                    content = nearest.map {
                        val dist = haversineMeters(lat, lon, it.latitude, it.longitude).toInt()
                        TextContent("${it.short_name} (stop ${it.stop_id}) — ${dist}m")
                    }
                )
            },
            onFailure = { CallToolResult(content = listOf(TextContent("Error getting nearest stops: ${it.message}"))) }
        )
    }

    server.addTool(
        name = "get-bus-departures",
        description = "List upcoming departures for a bus stop, given its stop id",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("stopId") { put("type", "string") }
            },
            required = listOf("stopId")
        )
    ) { request ->
        val stopId = request.arguments?.get("stopId")?.jsonPrimitive?.content
            ?: return@addTool CallToolResult(content = listOf(TextContent("The 'stopId' parameter is required.")))
        runCatching { galwayBusRepository.getStopDepartures(stopId) }.fold(
            onSuccess = { departures ->
                CallToolResult(
                    content = if (departures.isEmpty()) {
                        listOf(TextContent("No upcoming departures for stop $stopId."))
                    } else {
                        departures.map { d ->
                            val time = d.depart_timestamp ?: "scheduled"
                            TextContent("${d.timetable_id} → ${d.display_name} at $time")
                        }
                    }
                )
            },
            onFailure = { CallToolResult(content = listOf(TextContent("Error getting bus departures: ${it.message}"))) }
        )
    }

    server.addTool(
        name = "get-route-stops",
        description = "List the stops served by a bus route, given its route id (e.g. 401)",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("routeId") { put("type", "string") }
            },
            required = listOf("routeId")
        )
    ) { request ->
        val routeId = request.arguments?.get("routeId")?.jsonPrimitive?.content
            ?: return@addTool CallToolResult(content = listOf(TextContent("The 'routeId' parameter is required.")))
        runCatching { galwayBusRepository.getStopsForRoute(routeId) }.fold(
            onSuccess = { directions ->
                val stops = directions.flatten().distinctBy { it.stop_id }
                CallToolResult(
                    content = if (stops.isEmpty()) {
                        listOf(TextContent("No stops found for route $routeId."))
                    } else {
                        stops.map { TextContent("${it.short_name} (stop ${it.stop_id})") }
                    }
                )
            },
            onFailure = { CallToolResult(content = listOf(TextContent("Error getting route stops: ${it.message}"))) }
        )
    }

    return server
}

/** Great-circle distance in metres between two lat/lon points. */
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0

/**
 * Runs an MCP server over standard input/output — the transport used when a desktop MCP client
 * (e.g. Claude Desktop) launches this jar directly.
 */
fun runMcpServerUsingStdio() {
    val server = configureServer()
    val transport = StdioServerTransport(
        input = System.`in`.asSource().buffered(),
        output = System.out.asSink().buffered()
    ) {}

    runBlocking {
        // createSession connects (and starts) the transport; it returns once the client disconnects.
        val session = server.createSession(transport)
        val done = Job()
        session.onClose { done.complete() }
        done.join()
    }
}

/**
 * Launches an SSE (Server-Sent Events) MCP server on [port], letting clients connect over HTTP.
 */
fun runSseMcpServer(port: Int): Unit = runBlocking {
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp { configureServer() }
    }.start(wait = true)
}
