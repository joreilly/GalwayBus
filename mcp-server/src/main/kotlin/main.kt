/**
 * Entry point.
 * It initializes and runs the appropriate server mode based on the input arguments.
 *
 * Command-line arguments passed to the application:
 * - args[0]: Specifies the server mode. Supported values are:
 *      - "--sse-server": Runs the SSE MCP server.
 *      - "--stdio": Runs the MCP server using standard input/output.
 *      Defaults to "--sse-server" if not provided.
 * - args[1]: Specifies the port number for the server. Defaults to 3001 if not provided or invalid.
 */
fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "--sse-server"
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    when (command) {
        "--sse-server" -> runSseMcpServer(port)
        "--stdio" -> {
            // In stdio mode stdout carries the JSON-RPC protocol stream and must contain nothing
            // else. Capture the real stdout for the transport, then point System.out at stderr so
            // stray output (e.g. kotlin-logging's init banner, pulled in transitively by the MCP
            // SDK) can't corrupt the protocol. This must happen before any logger initialises.
            val protocolOut = System.out
            System.setOut(System.err)
            runMcpServerUsingStdio(protocolOut)
        }
        else -> System.err.println("Unknown command: $command")
    }
}
