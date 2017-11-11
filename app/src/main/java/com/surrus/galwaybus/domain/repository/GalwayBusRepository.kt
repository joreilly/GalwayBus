package com.surrus.galwaybus.domain.repository

import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Completable
import io.reactivex.Flowable

interface GalwayBusRepository {

    fun clearBusRoutes(): Completable

    fun saveBusRoutes(bufferoos: List<BusRoute>): Completable

    fun getBusRoutes(): Flowable<List<BusRoute>>
}