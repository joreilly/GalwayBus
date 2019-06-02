package com.surrus.galwaybus.common.remote

import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.map


class GalwayBusApi() {

    private val busRouteMapSerializer: KSerializer<Map<String, BusRoute>> = (StringSerializer to BusRoute.serializer()).map

    private val client by lazy {
        HttpClient() {
            install(JsonFeature) {
                serializer = KotlinxSerializer(Json(JsonConfiguration(strictMode = false))).apply {
                    setMapper(BusRoute::class, BusRoute.serializer())
                    setMapper(BusStop::class, BusStop.serializer())
                    //setListMapper(BusStop::class, BusStop.serializer())
                }
            }
        }
    }


    suspend fun fetchBusRoutes(): Map<String, BusRoute> {
        val jsonString = client.get<String> {
            url("$baseUrl/routes.json")
        }
        return Json.nonstrict.parse(busRouteMapSerializer, jsonString)
    }

    suspend fun fetchBusStops(): List<BusStop> {
        val jsonArrayString = client.get<String> {
            url("$baseUrl/stops.json")
        }
        return Json.nonstrict.parse(BusStop.serializer().list, jsonArrayString)
    }

    suspend fun getNearestStops(latitude: Double, longitude: Double): List<BusStop> {
        val jsonArrayString = client.get<String> {
            url("$baseUrl/stops/nearby.json")
            parameter("latitude", latitude)
            parameter("longitude", longitude)
        }
        return Json.nonstrict.parse(BusStop.serializer().list, jsonArrayString)
    }


    companion object {
        private const val baseUrl = "https://galwaybus.herokuapp.com"
    }
}