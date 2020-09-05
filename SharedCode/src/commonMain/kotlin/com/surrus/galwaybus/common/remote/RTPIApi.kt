package com.surrus.galwaybus.common.remote

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
import kotlinx.serialization.json.Json


class RTPIApi {
    private val baseUrl = "https://data.smartdublin.ie/cgi-bin/rtpi"

    private val nonStrictJson = Json { isLenient = true; ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient() {
            install(JsonFeature) {
                serializer = KotlinxSerializer(nonStrictJson)
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }


    // https://data.smartdublin.ie/cgi-bin/rtpi/busstopinformation?operator=be
    suspend fun getBusStopInformation() : BusStopInformationResult {
        return client.get{
            url("$baseUrl/busstopinformation")
            parameter("operator", "be")
        }
    }

    suspend fun getRouteInformation(routeId: String) : RouteInformationResult {
        return client.get{
            url("$baseUrl/routeinformation")
            parameter("operator", "be")
            parameter("routeid", routeId)
        }
    }

    //https://data.smartdublin.ie/cgi-bin/rtpi//realtimebusinformation?maxresults=10&operator=be&stopid=522301
    suspend fun getRealtimeBusInformation(stopId: String) : RealtimeBusInformationResult {
        return client.get{
            url("$baseUrl/realtimebusinformation")
            parameter("operator", "be")
            parameter("maxresults", "10")
            parameter("stopid", stopId)
        }
    }

}


@Serializable
data class RouteInformation(val origin: String, val destination: String, val stops: List<Stop>)

@Serializable
data class Stop(val stopid: String, val shortname: String, val fullname: String, val latitude: String, val longitude: String)


@Serializable
data class RealtimeBusInformation(val route: String, val duetime: String, val destination: String, val departuredatetime: String)


@Serializable
data class BusStopInformationResult(val results: List<Stop>)


@Serializable
data class RouteInformationResult(val route: String, val results: List<RouteInformation>)

@Serializable
data class RealtimeBusInformationResult(val results: List<RealtimeBusInformation>)
