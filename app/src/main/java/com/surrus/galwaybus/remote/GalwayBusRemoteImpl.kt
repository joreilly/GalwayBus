package com.surrus.galwaybus.remote

import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Flowable
import javax.inject.Inject

class GalwayBusRemoteImpl  @Inject constructor(private val galwayBusService: GalwayBusService) : GalwayBusRemote {

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return galwayBusService.getBusRoutes()
                .map {
                    val busRouteList = mutableListOf<BusRoute>()
                    it.values.forEach {
                        busRouteList.add(it)
                    }
                    busRouteList
                }
    }
}