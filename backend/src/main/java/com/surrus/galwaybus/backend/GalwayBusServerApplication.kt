package com.surrus.galwaybus.backend



import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.html.*
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.*

// Entry Point of the application as defined in resources/application.conf.
// @see https://ktor.io/servers/configuration.html#hocon-file
fun Application.main() {
    // This adds Date and Server headers to each response, and allows custom additional headers
    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    //val busStop = BusStop(1, "name", "irish name")

    routing {
        // Here we use a DSL for building HTML on the route "/"
        // @see https://github.com/Kotlin/kotlinx.html
        get("/") {
            call.respondHtml {
                head {
                    title { +"Ktor on Google App Engine Standard" }
                }
                body {
                    p {
                        +"hi there! This is Ktor running on Google Appengine Standard"
                    }
                }
            }
        }
        get("/bus") {

            val galwayBusApi = GalwayBusApi()
            val busStops = galwayBusApi.fetchBusStops()
            call.respond(busStops)
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8090) { main() }.start(wait = true)
}