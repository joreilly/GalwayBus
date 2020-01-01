package com.surrus.galwaybus.common

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch



expect fun createDb() : MyDatabase?

class GalwayBusRepository {

    private val galwayBusApi = GalwayBusApi()
    private val galwayBusDb = createDb()
    private val galwayBusQueries = galwayBusDb?.galwayBusQueries

    init {
        GlobalScope.launch (Dispatchers.Main) {
            fetchAndStoreBusStops()
        }
    }


    private suspend fun fetchAndStoreBusStops() {
        val busStops = galwayBusApi.fetchBusStops()

        busStops.forEach {
            galwayBusQueries?.insertItem(it.stop_id.toLong(), it.short_name, it.irish_short_name)
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


    fun getBusStops(success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            success(getBusStops())
        }
    }

    suspend fun fetchBusRoutes(): List<BusRoute> {
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            success(fetchBusRoutes())
        }
    }

    suspend fun getNearestStops(latitude: Double, longitude: Double): List<BusStop> {
        return galwayBusApi.getNearestStops(latitude, longitude)
    }

    fun getNearestStops(latitude: Double, longitude: Double, success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            success(getNearestStops(latitude, longitude))
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