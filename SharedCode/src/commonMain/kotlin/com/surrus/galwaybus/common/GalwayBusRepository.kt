package com.surrus.galwaybus.common

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.common.remote.GalwayBusApi
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
        val busStops = galwayBusApi.fetchAllBusStops()

        busStops.forEach {
            galwayBusQueries?.insertItem(it.stop_id.toLong(), it.shortName, it.irishShortName)
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getBusStopsFlow() = galwayBusQueries?.selectAll(mapper = { stop_id, short_name, irish_short_name ->
            BusStop(stop_id.toInt(), short_name, irish_short_name)
        })?.asFlow()?.mapToList()


    suspend fun getBusStops(): List<BusStop> {
        return galwayBusQueries?.selectAll(mapper = { stop_id, short_name, irish_short_name ->
            BusStop(stop_id.toInt(), short_name, irish_short_name)
        })?.executeAsList() ?: emptyList<BusStop>()
    }


    suspend fun fetchRouteStops(routeId: String) = galwayBusApi.fetchRouteStops(routeId)

    suspend fun fetchBusListForRoute(routeId: String): Result<List<Bus>> {
        try {
            val busList = galwayBusApi.fetchBusListForRoute(routeId)
            return Result.Success(busList)
        } catch (e: Exception) {
            return Result.Error(e)
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
}