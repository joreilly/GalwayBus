package com.surrus.galwaybus.common

import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalwayBusRepository {


    suspend fun fetchBusStops(): List<BusStop> {
        val galwayBusApi = GalwayBusApi()
        return galwayBusApi.fetchBusStops()
    }

    fun fetchBusStops(success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
            val galwayBusApi = GalwayBusApi()
            success(galwayBusApi.fetchBusStops())
        }
    }


    suspend fun fetchBusRoutes(): List<BusRoute> {
        val galwayBusApi = GalwayBusApi()
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
            val galwayBusApi = GalwayBusApi()
            val busRoutes = galwayBusApi.fetchBusRoutes()
            success(transformBusRouteMapToList(busRoutes))
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