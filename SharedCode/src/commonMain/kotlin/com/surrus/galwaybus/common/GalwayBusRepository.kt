package com.surrus.galwaybus.common

import com.surrus.galwaybus.common.model.BusRoute
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalwayBusRepository {

    private val galwayBusApi = GalwayBusApi()

    suspend fun fetchBusStops(): List<BusStop> {
        return galwayBusApi.fetchBusStops()
    }

    fun fetchBusStops(success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
            success(fetchBusStops())
        }
    }


    suspend fun fetchBusRoutes(): List<BusRoute> {
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
            success(fetchBusRoutes())
        }
    }

    suspend fun getNearestStops(latitude: Double, longitude: Double): List<BusStop> {
        return galwayBusApi.getNearestStops(latitude, longitude)
    }

    fun getNearestStops(latitude: Double, longitude: Double, success: (List<BusStop>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
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