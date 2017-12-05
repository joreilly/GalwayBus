package com.surrus.galwaybus.data

import com.surrus.galwaybus.data.source.GalwayBusDataStoreFactory
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.Completable
import io.reactivex.Flowable
import javax.inject.Inject

/**
 * Provides an implementation of the [GalwayBusRepository] interface for communicating to and from
 * data sources
 */
class GalwayBusDataRepository @Inject constructor(private val factory: GalwayBusDataStoreFactory): GalwayBusRepository {

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return factory.retrieveCacheDataStore().isCached()
                .flatMapPublisher {
                    factory.retrieveDataStore(it).getBusRoutes()
                }
                .flatMap {
                    saveBusRoutes(it).toSingle { it }.toFlowable()
                }
    }


    override fun getNearestBusStops(latitude: Float, longitude: Float): Flowable<List<BusStop>> {
        return factory.retrieveRemoteDataStore().getNearestBusStops(latitude, longitude)
    }

    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return factory.retrieveRemoteDataStore().getBusStops(routeId)
    }

    override fun saveBusRoutes(busRoutes: List<BusRoute>): Completable {
        return factory.retrieveCacheDataStore().saveBusRoutes(busRoutes)
    }

    override fun clearBusRoutes(): Completable {
        return factory.retrieveCacheDataStore().clearBusRoutes()
    }

}
