package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*
import io.reactivex.Flowable

interface GalwayBusRemote {
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>
    fun getNearestBusStops(location: Location): Flowable<List<BusStop>>
    fun getDepartures(stopRef: String): Flowable<List<Departure>>
    fun getSchedules(): Flowable<Map<String, RouteSchedule>>
}