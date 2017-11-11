package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Flowable

interface GalwayBusRemote {

    fun getBusRoutes(): Flowable<List<BusRoute>>

}