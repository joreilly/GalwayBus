package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface GalwayBusCache {
    fun clearBusRoutes(): Completable
    fun saveBusRoutes(busRoutes: List<BusRoute>): Completable
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun isCached(): Single<Boolean>

    fun clearBusStops(): Completable
    fun saveBusStops(busStopList: List<BusStop>): Completable
    fun getBusStops(): Flowable<List<BusStop>>
    fun isBusStopsCached(): Single<Boolean>
    fun getNumberBusStops(): Single<Int>

    fun getBusStopsByName(name: String) : Flowable<List<BusStop>>

    fun setLastCacheTime(lastCache: Long)
    fun isExpired(): Boolean
}