package com.surrus.galwaybus.domain.repository

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.Completable
import io.reactivex.Flowable

interface GalwayBusRepository {

    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun saveBusRoutes(bufferoos: List<BusRoute>): Completable
    fun clearBusRoutes(): Completable

    fun getNearestBusStops(latitude: Float, longitude: Float): Flowable<List<BusStop>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>
}