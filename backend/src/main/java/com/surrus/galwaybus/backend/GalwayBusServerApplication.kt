package com.surrus.galwaybus.backend

import com.surrus.galwaybus.common.remote.GalwayBusApi
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.PartialContent
import io.ktor.features.StatusPages
import io.ktor.freemarker.FreeMarker
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.error
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title
import org.example.kotlin.multiplatform.backend.serializable

fun Application.main() {

    install(DefaultHeaders)
    install(CallLogging)
//    install(ConditionalHeaders)
//    install(PartialContent)
//    install(Compression)
//    //install(Locations)
//    install(StatusPages) {
//        exception<Throwable> { cause ->
//            environment.log.error(cause)
//            call.respond(HttpStatusCode.InternalServerError)
//        }
//    }

    install(ContentNegotiation) {
        //serializable { }
        gson {
            setPrettyPrinting()
        }
    }

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(javaClass.classLoader, "")
    }

    install(Routing) {
        index()

        get("/stops.json") {
            val galwayBusApi = GalwayBusApi()
            val busStops = galwayBusApi.fetchBusStops()
            call.respond(busStops)
        }
    }

}
