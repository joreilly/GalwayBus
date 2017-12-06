package com.surrus.galwaybus.remote

import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import com.surrus.galwaybus.model.Location
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


    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return galwayBusService.getStops(routeId)
                .map {
                    it.stops
                }
    }

    override fun getNearestBusStops(location: Location): Flowable<List<BusStop>> {
        return galwayBusService.getNearestStops(location.latitude, location.longitude)
    }

    override fun getDepartures(stopRef: String): Flowable<List<Departure>> {
        return galwayBusService.getDepartures(stopRef)
                .map {
                    it.departureTimes
                }
    }
}