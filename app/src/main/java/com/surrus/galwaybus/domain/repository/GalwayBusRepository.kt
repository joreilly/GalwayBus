package com.surrus.galwaybus.domain.repository

import com.surrus.galwaybus.model.*
import io.reactivex.Completable
import io.reactivex.Flowable

interface GalwayBusRepository {

    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun saveBusRoutes(busRoutes: List<BusRoute>): Completable
    fun clearBusRoutes(): Completable

    fun getBusStops() : Flowable<List<BusStop>>
    fun saveBusStops(busStops: List<BusStop>): Completable
    fun clearBusStops(): Completable


    fun getNearestBusStops(location: Location): Flowable<List<BusStop>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>
    fun getBusStopsByName(name: String) : Flowable<List<BusStop>>

    fun getDepartures(stopRef: String): Flowable<List<Departure>>
    fun getSchedules(): Flowable<Map<String, RouteSchedule>>
}