package com.surrus.galwaybus.common.remote

import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list


class GalwayBusApi() {

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

    suspend fun fetchBusRoutes(): HashMap<String, BusRoute> {
        return client.get {
            url("$baseUrl/routes.json")
        }
    }

    suspend fun fetchBusStops(): List<BusStop> {
        val jsonArrayString = client.get<String> {
            url("$baseUrl/stops.json")
        }

        return JSON.nonstrict.parse(BusStop.serializer().list, jsonArrayString)
    }

    companion object {
        private const val baseUrl = "https://galwaybus.herokuapp.com"
    }
}