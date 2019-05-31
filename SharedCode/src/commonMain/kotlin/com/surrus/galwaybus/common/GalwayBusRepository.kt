package com.surrus.galwaybus.common

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
            val stops = galwayBusApi.fetchBusStops()
            success(stops)
        }
    }

}