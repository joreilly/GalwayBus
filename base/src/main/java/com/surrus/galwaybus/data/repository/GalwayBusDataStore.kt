package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface GalwayBusDataStore {
    fun clearBusRoutes(): Completable
    fun saveBusRoutes(busRoutes: List<BusRoute>): Completable
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun isCached(): Single<Boolean>

    fun clearBusStops(): Completable
    fun saveBusStops(busStops: List<BusStop>): Completable
    fun getBusStops(): Flowable<List<BusStop>>
    fun getBusStopsByName(name: String) : Flowable<List<BusStop>>
    fun isBusStopsCached(): Single<Boolean>
    fun getNumberBusStops(): Single<Int>

    fun getNearestBusStops(location: Location): Flowable<List<BusStop>>
    fun getBusStops(routeId: String): Flowable<List<List<BusStop>>>

    fun getDepartures(stopRef: String): Flowable<List<Departure>>
    fun getSchedules(): Flowable<Map<String, RouteSchedule>>
}