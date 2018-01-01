package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.model.*
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

    override fun isCached(): Single<Boolean> {
        return galwayBusCache.isCached()
    }


    override fun clearBusStops(): Completable {
        return galwayBusCache.clearBusStops()
    }

    override fun saveBusStops(busStopList: List<BusStop>): Completable {
        return galwayBusCache.saveBusStops(busStopList)
    }

    override fun getBusStops(): Flowable<List<BusStop>> {
        return galwayBusCache.getBusStops()
    }

    override fun getBusStopsByName(name: String) : Flowable<List<BusStop>> {
        return galwayBusCache.getBusStopsByName(name)
    }

    override fun isBusStopsCached(): Single<Boolean> {
        return galwayBusCache.isBusStopsCached()
    }

    override fun getNumberBusStops(): Single<Int> {
        return galwayBusCache.getNumberBusStops()
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

    override fun getSchedules(): Flowable<Map<String, RouteSchedule>> {
        return Flowable.just(emptyMap<String, RouteSchedule>())
    }


}