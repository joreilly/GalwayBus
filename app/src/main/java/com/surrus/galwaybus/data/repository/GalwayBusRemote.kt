package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import io.reactivex.Flowable

interface GalwayBusRemote {
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>
    fun getNearestBusStops(latitude: Float, longitude: Float): Flowable<List<BusStop>>
    fun getDepartures(stopRef: String): Flowable<List<Departure>>
}