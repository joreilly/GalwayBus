package com.surrus.galwaybus.data

import com.orhanobut.logger.Logger
import com.surrus.galwaybus.data.source.GalwayBusDataStoreFactory
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers


/**
 * Provides an implementation of the [GalwayBusRepository] interface for communicating to and from
 * data sources
 */
class GalwayBusDataRepository constructor(private val factory: GalwayBusDataStoreFactory): GalwayBusRepository {


    init {
        // Preload Bus Stop info
        getBusStops().subscribeOn(Schedulers.io()).subscribe {
            Logger.d("GalwayBusDataRepository: loaded bus stop info")
        }
    }


    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return factory.retrieveCacheDataStore().isCached()
                .flatMapPublisher {
                    factory.retrieveDataStore(it).getBusRoutes()
                }
                .flatMap {
                    saveBusRoutes(it).toSingle { it }.toFlowable()
                }
    }

    override fun saveBusRoutes(busRoutes: List<BusRoute>): Completable {
        return factory.retrieveCacheDataStore().saveBusRoutes(busRoutes)
    }

    override fun clearBusRoutes(): Completable {
        return factory.retrieveCacheDataStore().clearBusRoutes()
    }

    override fun getBusStops() : Flowable<List<BusStop>> {
        factory.retrieveRemoteDataStore().getBusStops()
                .subscribeOn(Schedulers.io())
                .flatMap { saveBusStops(it).toSingle { it }.toFlowable() }
                .subscribe({
            Logger.d("Loaded bus stop info")
        }, {
            Logger.e(it.message)
        })

        return factory.retrieveCacheDataStore().getBusStops()
    }

    override fun saveBusStops(busStops: List<BusStop>): Completable {
        return factory.retrieveCacheDataStore().saveBusStops(busStops)
    }

    override fun clearBusStops(): Completable {
        return factory.retrieveCacheDataStore().clearBusStops()
    }





    override fun getNearestBusStops(location: Location): Flowable<List<BusStop>> {
        return factory.retrieveRemoteDataStore().getNearestBusStops(location)
    }

    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return factory.retrieveRemoteDataStore().getBusStops(routeId)
    }

    override fun getBusStopsByName(name: String) : Flowable<List<BusStop>> {
        return factory.retrieveCacheDataStore().getBusStopsByName(name)
    }

    override fun getDepartures(stopRef: String): Flowable<List<Departure>> {
        return factory.retrieveRemoteDataStore().getDepartures(stopRef)
    }

    override fun getSchedules(): Flowable<Map<String, RouteSchedule>> {
        return factory.retrieveRemoteDataStore().getSchedules()
    }

}
