package com.surrus.galwaybus.domain.repository

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import com.surrus.galwaybus.model.Location
import io.reactivex.Completable
import io.reactivex.Flowable

interface GalwayBusRepository {

    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun saveBusRoutes(bufferoos: List<BusRoute>): Completable
    fun clearBusRoutes(): Completable

    fun getNearestBusStops(location: Location): Flowable<List<BusStop>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>
    fun getDepartures(stopRef: String): Flowable<List<Departure>>
}