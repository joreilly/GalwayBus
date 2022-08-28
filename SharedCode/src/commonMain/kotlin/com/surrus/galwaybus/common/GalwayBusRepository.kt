package com.surrus.galwaybus.common

import co.touchlab.kermit.Logger
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.surrus.galwaybus.common.model.*
import com.surrus.galwaybus.common.remote.CityBikesApi
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.common.remote.Station
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.time.Duration


open class GalwayBusRepository : KoinComponent {
    private val galwayBusApi: GalwayBusApi = get()
    private val cityBikesApi: CityBikesApi by inject()

    private val appSettings: AppSettings by inject()
    private val galwayBusDb: MyDatabase by inject()
    private val galwayBusQueries = galwayBusDb.galwayBusQueries

    val busStops = galwayBusQueries.selectAll(mapper = { stop_id, stop_ref, short_name, long_name, latitude, longitude ->
        BusStop(stop_id, short_name, long_name, stop_ref, latitude = latitude, longitude = longitude)
    }).asFlow().mapToList()

    val favorites = appSettings.favorites
    val favoriteBusStopList = busStops.combine(favorites) { busStops, favorites ->
        Logger.i { "getBusStopsFlow().combine, favorites = $favorites, busStops size = ${busStops.size}" }
        favorites.map { favorite -> busStops.firstOrNull { it.stop_id == favorite } }.filterNotNull()
    }

    suspend fun fetchAndStoreBusStops() {
        Logger.i { "fetchAndStoreBusStops" }
        try {
            val busStops = galwayBusApi.fetchAllBusStops()
            Logger.i { "JFOR, fetchAndStoreBusStops, busStops, size = ${busStops.size}" }

            galwayBusQueries.deleteAll()
            val galwayBusStops = busStops.filter { it.distance != null && it.distance < 20000.0 }
            println(galwayBusStops)
            galwayBusStops.forEach {
                if (it.latitude != null && it.longitude != null) {
                    galwayBusQueries.insertItem(it.stop_id, it.stopRef, it.shortName, it.longName, it.latitude, it.longitude)
                }
            }
            Logger.d("JFOR, fetchAndStoreBusStops, finished storing bus stops in db, busStops = $busStops")
            busStops.forEach {
                println("JFOR, ${it.stopRef} : ${it.stop_id}")
            }
        } catch(e: Exception) {
            Logger.e { "fetchAndStoreBusStops, exception e = $e" }
            e.printStackTrace()
        }
    }

//    @ExperimentalCoroutinesApi
//    fun getBusStopsFlow() = galwayBusQueries?.selectAll(mapper = { stop_id, stop_ref, short_name, long_name, latitude, longitude ->
//            BusStop(stop_id, short_name, long_name, stop_ref, latitude = latitude, longitude = longitude)
//        })?.asFlow()?.mapToList() ?: flowOf(emptyList())
//
//
    fun getBusStops(): List<BusStop> {
        return galwayBusQueries.selectAll(mapper = { stop_id, stop_ref, short_name, long_name, latitude, longitude  ->
            BusStop(stop_id, short_name, long_name, stop_ref, latitude = latitude, longitude = longitude)
        }).executeAsList()
    }


    suspend fun fetchRouteStops(routeId: String): Result<List<List<BusStop>>> {
        try {
            val busStopLists = galwayBusApi.fetchRouteStops(routeId)
            return Result.Success(busStopLists)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun fetchBusListForRoute(routeId: String): Result<List<Bus>> {
        try {
            val busList = galwayBusApi.fetchBusListForRoute(routeId)
            return Result.Success(busList)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun fetchBusStopDepartures(stopRef: String): Result<List<GalwayBusDeparture>> {
        try {
            val busStopResponse = galwayBusApi.fetchBusStop(stopRef)

            val now = Clock.System.now()
            val departures = busStopResponse.times
                .map { departure ->
                    departure.departTimestamp?.let {
                        val departureTime = Instant.parse(departure.departTimestamp)
                        val durationUntilDeparture: Duration = departureTime - now
                        val minutesUntilDeparture = durationUntilDeparture.inWholeMinutes
                        GalwayBusDeparture(departure.timetableId, departure.displayName, departure.departTimestamp,
                            durationUntilDeparture, minutesUntilDeparture)
                    }
                }
                .filterNotNull()
                .take(5)

            return Result.Success(departures)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun fetchBusRoutes(): List<BusRoute> {
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    suspend fun fetchNearestStops(latitude: Double, longitude: Double): Result<List<BusStop>> {
        try {
            val busStops = galwayBusApi.fetchNearestStops(latitude, longitude)
            return Result.Success(busStops)
        } catch (e: Exception) {
            println(e)
            return Result.Error(e)
        }
    }

    private fun transformBusRouteMapToList(busRoutesMap: Map<String, BusRoute>): List<BusRoute> {
        val busRouteList = mutableListOf<BusRoute>()
        busRoutesMap.values.forEach {
            busRouteList.add(it)
        }
        return busRouteList
    }

    fun toggleFavorite(stopRef: String) {
        appSettings.toggleFavorite(stopRef)
    }


    suspend fun fetchBikeShareInfo(network: String) : List<Station> {
        val result = cityBikesApi.fetchBikeShareInfo(network)
        return result.network.stations
    }

}