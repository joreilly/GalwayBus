import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.di.initKoin
import com.surrus.galwaybus.common.model.Result
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive


private val koin = initKoin(enableNetworkLogs = true).koin

fun configureServer(): Server {
    val galwayBusRepository = koin.get<GalwayBusRepository>()

    val server = Server(
        Implementation(
            name = "GalwayBus MCP Server",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )


    server.addTool(
        name = "get-bus-routes",
        description = "List of bus routes"
    ) {
        val busRoutes = galwayBusRepository.fetchBusRoutes()
        CallToolResult(
            content =
                busRoutes.map { TextContent("${it.timetableId}, ${it.longName}") }
        )
    }



    server.addTool(
        name = "get-nearest-stops",
        description = "List nearest bus stops",
    ) { request ->
        val latitude = 53.2743394
        val longitude = -9.0514163
        val routeStopsResult = galwayBusRepository.fetchNearestStops(latitude, longitude)
        if (routeStopsResult is Result.Success) {
            val routeStops = routeStopsResult.data
            CallToolResult(
                content = routeStops.map { TextContent(it.toString()) }
            )
        } else {
            CallToolResult(
                content = listOf(TextContent("Error getting route stops."))
            )
        }
    }


    server.addTool(
        name = "get-bus-departures",
        description = "List",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf("stopRef" to JsonPrimitive("string"))
            ),
            required = listOf("stopRef")
        )

    ) { request ->
        val stopRef = request.arguments["stopRef"]
        if (stopRef == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'stopRef' parameter is required."))
            )
        }

        val busDeparturesResult = galwayBusRepository.fetchBusStopDepartures(stopRef.jsonPrimitive.content)
        if (busDeparturesResult is Result.Success) {
            val routeStops = busDeparturesResult.data
            CallToolResult(
                content = routeStops.map { TextContent(it.toString()) }
            )
        } else {
            CallToolResult(
                content = listOf(TextContent("Error getting bus departures."))
            )
        }
    }


    server.addTool(
        name = "get-route-stops",
        description = "List or stops for particular bus route",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf("routeId" to JsonPrimitive("string"))
            ),
            required = listOf("routeId")
        )

    ) { request ->
        val routeId = request.arguments["routeId"]
        if (routeId == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'routeId' parameter is required."))
            )
        }

        val routeStopsResult = galwayBusRepository.fetchRouteStops(routeId.jsonPrimitive.content)
        if (routeStopsResult is Result.Success) {
            val routeStops = routeStopsResult.data
            CallToolResult(
                content = routeStops.map { TextContent(it.toString()) }
            )
        } else {
            CallToolResult(
                content = listOf(TextContent("Error getting route stops."))
            )
        }
    }

    return server
}

/**
 * Runs an MCP (Model Context Protocol) server using standard I/O for communication.
 *
 * This function initializes a server instance configured with predefined tools and capabilities.
 * It sets up a transport mechanism using standard input and output for communication.
 * Once the server starts, it listens for incoming connections, processes requests,
 * and executes the appropriate tools. The server shuts down gracefully upon receiving
 * a close event.
 */
fun `run mcp server using stdio`() {
    val server = configureServer()
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
    }
}

/**
 * Launches an SSE (Server-Sent Events) MCP (Model Context Protocol) server on the specified port.
 * This server enables clients to connect via SSE for real-time communication and provides endpoints
 * for handling specific messages.
 *
 * @param port The port number on which the SSE server should be started.
 */
fun `run sse mcp server`(port: Int): Unit = runBlocking {
    val server = configureServer()
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            server
        }
    }.start(wait = true)
}
