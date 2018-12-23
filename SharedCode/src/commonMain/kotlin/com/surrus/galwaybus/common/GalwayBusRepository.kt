package com.surrus.galwaybus.common

import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.remote.GalwayBusApi

class GalwayBusRepository {


    suspend fun fetchBusStops(): List<BusStop> {
        val galwayBusApi = GalwayBusApi()
        return galwayBusApi.fetchBusStops()
    }
}