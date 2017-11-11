package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface GalwayBusDataStore {
    fun clearBusRoutes(): Completable
    fun saveBusRoutes(bufferoos: List<BusRoute>): Completable
    fun getBusRoutes(): Flowable<List<BusRoute>>
    fun isCached(): Single<Boolean>

}