package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface GalwayBusCache {
    fun clearBusRoutes(): Completable
    fun saveBusRoutes(busRoutes: List<BusRoute>): Completable
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun isCached(): Single<Boolean>
    fun setLastCacheTime(lastCache: Long)
    fun isExpired(): Boolean
}