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
        GlobalScope.launch (Dispatchers.IO) {
            getBusStops()
        }
    }


    override suspend fun getBusRoutes(): List<BusRoute> = coroutineScope {
        val isCached = factory.retrieveCacheDataStore().isCached()
        val busRoutes = factory.retrieveDataStore(isCached).getBusRoutes()

        saveBusRoutes(busRoutes)
        busRoutes
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>) {
        return factory.retrieveCacheDataStore().saveBusRoutes(busRoutes)
    }

    override suspend fun clearBusRoutes() {
        return factory.retrieveCacheDataStore().clearBusRoutes()
    }

    private suspend fun getBusStops() = coroutineScope {

        val busStops = factory.retrieveRemoteDataStore().getBusStops()
        saveBusStops(busStops)
    }

    override suspend fun saveBusStops(busStops: List<BusStop>) {
        return factory.retrieveCacheDataStore().saveBusStops(busStops)
    }

    override suspend fun clearBusStops()  {
        return factory.retrieveCacheDataStore().clearBusStops()
    }


    override suspend fun getNearestBusStops(location: Location): Result<List<BusStop>> {
        return factory.retrieveRemoteDataStore().getNearestBusStops(location)
    }

    override suspend fun getBusStops(routeId: String): List<List<BusStop>> {
        return factory.retrieveRemoteDataStore().getBusStops(routeId)
    }

    override suspend fun getBusStopsByName(name: String) : List<BusStop> {
        return factory.retrieveCacheDataStore().getBusStopsByName(name)
    }

    override suspend fun getDepartures(stopRef: String): List<Departure> {
        return factory.retrieveRemoteDataStore().getDepartures(stopRef)
    }

    override suspend fun getSchedules(): Map<String, RouteSchedule> {
        return factory.retrieveRemoteDataStore().getSchedules()
    }

}
