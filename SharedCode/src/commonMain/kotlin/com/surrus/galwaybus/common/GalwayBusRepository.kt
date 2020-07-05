package com.surrus.galwaybus.common

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.surrus.galwaybus.common.model.*
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.common.remote.RTPIApi
import com.surrus.galwaybus.common.remote.RealtimeBusInformation
import com.surrus.galwaybus.common.remote.Stop
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


expect fun createDb() : MyDatabase?

// TEMP until following is resolved https://github.com/ktorio/ktor/issues/1622
expect fun ktorScope(block: suspend () -> Unit)


open class GalwayBusRepository {

    private val galwayBusApi = GalwayBusApi()
    private val galwayBusDb = createDb()
    private val galwayBusQueries = galwayBusDb?.galwayBusQueries

    init {
        ktorScope {
            fetchAndStoreBusStops()
        }
    }


    private suspend fun fetchAndStoreBusStops() {
        try {
            val busStops = galwayBusApi.fetchAllBusStops()

            busStops.forEach {
                galwayBusQueries?.insertItem(it.stop_id.toLong(), it.shortName, it.irishShortName, it.latitude, it.longitude)
            }
        } catch(e: Exception) {
            // TODO how should we handle this
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getBusStopsFlow() = galwayBusQueries?.selectAll(mapper = { stop_id, short_name, irish_short_name, latitude, longitude ->
            BusStop(stop_id.toInt(), short_name, irish_short_name, latitude = latitude, longitude = longitude)
        })?.asFlow()?.mapToList()


    suspend fun getBusStops(): List<BusStop> {
        return galwayBusQueries?.selectAll(mapper = { stop_id, short_name, irish_short_name, latitude, longitude  ->
            BusStop(stop_id.toInt(), short_name, irish_short_name, latitude = latitude, longitude = longitude)
        })?.executeAsList() ?: emptyList<BusStop>()
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

    fun fetchBusListForRoute(routeId: String, success: (List<Bus>) -> Unit) {
        ktorScope {
            val busList = galwayBusApi.fetchBusListForRoute(routeId)
            success(busList)
        }
    }


    fun getBusStops(success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            success(getBusStops())
        }
    }

    suspend fun fetchSchedules() = galwayBusApi.fetchSchedules()
        .mapValues {
            it.value[0].values.elementAt(0)
        }


    suspend fun fetchBusRoutes(): List<BusRoute> {
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        ktorScope {
            success(fetchBusRoutes())
        }
    }

    suspend fun fetchNearestStops(latitude: Double, longitude: Double): Result<List<BusStop>> {
        try {
            val busStops = galwayBusApi.fetchNearestStops(latitude, longitude)
            return Result.Success(busStops)
        } catch (e: Exception) {
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


    // RTPI based queries
    private val rtpiApi = RTPIApi()

    suspend fun getNearestStops(center: Location): Result<List<Stop>> {
        try {
            val result = rtpiApi.getBusStopInformation()
            val nearestStops = result.results.map { stop ->
                stop to center.distance((Location(stop.latitude.toDouble(), stop.longitude.toDouble())))      //poses.sortedBy { point.distance(it) }.drop(1).take(10)
            }.sortedBy { it.second }.take(20).map { it -> it.first }

            return Result.Success(nearestStops)
        } catch(e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun getRealtimeBusInformation(stopId: String): Result<List<RealtimeBusInformation>> {
        try {
            val result = rtpiApi.getRealtimeBusInformation(stopId)
            return Result.Success(result.results)
        } catch(e: Exception) {
            return Result.Error(e)
        }
    }
}