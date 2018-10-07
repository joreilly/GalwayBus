package com.surrus.galwaybus.data

import com.surrus.galwaybus.data.source.GalwayBusDataStoreFactory
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.*


/**
 * Provides an implementation of the [GalwayBusRepository] interface for communicating to and from
 * data sources
 */
class GalwayBusDataRepository constructor(private val factory: GalwayBusDataStoreFactory): GalwayBusRepository {

    init {
        // Preload Bus Stop info
        launch {
            getBusStops().await()
        }
    }


    override suspend fun getBusRoutes(): Deferred<List<BusRoute>> = coroutineScope {
        async(Dispatchers.IO) {
            val isCached = factory.retrieveCacheDataStore().isCached().await()
            val busRoutes = factory.retrieveDataStore(isCached).getBusRoutes().await()

            saveBusRoutes(busRoutes).await()
            busRoutes
        }
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>) : Deferred<Unit> {
        return factory.retrieveCacheDataStore().saveBusRoutes(busRoutes)
    }

    override suspend fun clearBusRoutes(): Deferred<Unit> {
        return factory.retrieveCacheDataStore().clearBusRoutes()
    }

    private suspend fun getBusStops() = coroutineScope {

        async(Dispatchers.IO) {

            val busStops = factory.retrieveRemoteDataStore().getBusStops().await()
            saveBusStops(busStops)
        }
    }

    override suspend fun saveBusStops(busStops: List<BusStop>) : Deferred<Unit> {
        return factory.retrieveCacheDataStore().saveBusStops(busStops)
    }

    override suspend fun clearBusStops() : Deferred<Unit> {
        return factory.retrieveCacheDataStore().clearBusStops()
    }


    override suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>> {
        return factory.retrieveRemoteDataStore().getNearestBusStops(location)
    }

    override suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>> {
        return factory.retrieveRemoteDataStore().getBusStops(routeId)
    }

    override suspend fun getBusStopsByName(name: String) : Deferred<List<BusStop>> {
        return factory.retrieveCacheDataStore().getBusStopsByName(name)
    }

    override suspend fun getDepartures(stopRef: String): Deferred<List<Departure>> {
        return factory.retrieveRemoteDataStore().getDepartures(stopRef)
    }

    override suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>> {
        return factory.retrieveRemoteDataStore().getSchedules()
    }

}
