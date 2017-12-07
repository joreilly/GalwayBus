package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import com.surrus.galwaybus.model.Location
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

open class GalwayBusCacheDataStore @Inject constructor(private val galwayBusCache: GalwayBusCache) : GalwayBusDataStore {

    override fun clearBusRoutes(): Completable {
        return galwayBusCache.clearBusRoutes()
    }

    override fun saveBusRoutes(busRoutes: List<BusRoute>): Completable {
        return galwayBusCache.saveBusRoutes(busRoutes)
                .doOnComplete {
                    galwayBusCache.setLastCacheTime(System.currentTimeMillis())
                }
    }

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return galwayBusCache.getBusRoutes()
    }

    override fun getNearestBusStops(location: Location): Flowable<List<BusStop>> {
        return Flowable.just(emptyList<BusStop>())
    }

    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return Flowable.just(emptyList<List<BusStop>>())
    }

    override fun getDepartures(stopRef: String): Flowable<List<Departure>> {
        return Flowable.just(emptyList<Departure>())
    }

    override fun isCached(): Single<Boolean> {
        return galwayBusCache.isCached()
    }

}