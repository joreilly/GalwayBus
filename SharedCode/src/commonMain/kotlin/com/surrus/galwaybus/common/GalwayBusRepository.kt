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

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        GlobalScope.launch(ApplicationDispatcher) {
            val galwayBusApi = GalwayBusApi()
            val busRoutes = galwayBusApi.fetchBusRoutes()

            val busRouteList = mutableListOf<BusRoute>()
            busRoutes.values.forEach {
                busRouteList.add(it)
            }
            success(busRouteList)
        }
    }
}