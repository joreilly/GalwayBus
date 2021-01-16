package com.surrus.galwaybus.common.remote

import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Departure
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json


@Serializable
data class GetRouteStopsResponse(val route: BusRoute, val stops: List<List<BusStop>>)

@Serializable
data class GetBusListForRouteResponse(val bus: List<Bus>)

@Serializable
data class BusStopResponse(val stop: BusStop, val times: List<Departure> = emptyList())


class GalwayBusApi(private val client: HttpClient, val baseUrl: String = "https://api.galwaybusabu.com/v1") {
    private val nonStrictJson = Json { isLenient = true; ignoreUnknownKeys = true }

    private val busScheduleMapSerializer = MapSerializer(String.serializer(), ListSerializer(MapSerializer(String.serializer(), String.serializer())))


    suspend fun fetchBusRoutes(): Map<String, BusRoute> {
        return client.get("$baseUrl/routes.json")
    }

    suspend fun fetchAllBusStops(): List<BusStop> {
        return client.get("$baseUrl/stops.json")
    }

    suspend fun fetchBusStop(stopRef: String): BusStopResponse {
        return client.get("$baseUrl/stops/$stopRef.json")
    }

    suspend fun fetchSchedules(): Map<String, List<Map<String, String>>> {
        val jsonString = client.get<String>("$baseUrl/schedules.json")
        return nonStrictJson.decodeFromString(busScheduleMapSerializer, jsonString)
    }

    suspend fun fetchNearestStops(latitude: Double, longitude: Double): List<BusStop> {
        return client.get{
            url("$baseUrl/stops/nearby.json")
            parameter("latitude", latitude)
            parameter("longitude", longitude)
        }
    }

    suspend fun fetchRouteStops(routeId: String): List<List<BusStop>> {
        return client.get<GetRouteStopsResponse>("$baseUrl/routes/$routeId.json").stops
    }

    suspend fun fetchBusListForRoute(routeId: String): List<Bus> {
        return client.get<GetBusListForRouteResponse>("$baseUrl/bus/$routeId.json").bus
    }
}