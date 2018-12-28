package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.coroutineScope


open class GalwayBusCacheDataStore constructor(private val galwayBusCache: GalwayBusCache) : GalwayBusDataStore {

    override suspend fun clearBusRoutes()  = coroutineScope {
        galwayBusCache.clearBusRoutes()
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>)  = coroutineScope {
        galwayBusCache.saveBusRoutes(busRoutes)
        galwayBusCache.setLastCacheTime(System.currentTimeMillis())
    }

    override suspend fun getBusRoutes(): List<BusRoute> = coroutineScope {
        galwayBusCache.getBusRoutes()
    }

    override suspend fun isCached(): Boolean = coroutineScope {
        galwayBusCache.isCached()
    }


    override suspend fun clearBusStops()  = coroutineScope {
        galwayBusCache.clearBusStops()
    }

    override suspend fun saveBusStops(busStopList: List<BusStop>) = coroutineScope {
        galwayBusCache.saveBusStops(busStopList)
    }

    override suspend fun getBusStops(): List<BusStop>  = coroutineScope {
        galwayBusCache.getBusStops()
    }

    override suspend fun getBusStopsByName(name: String) : List<BusStop> = coroutineScope {
        galwayBusCache.getBusStopsByName(name)
    }

    override suspend fun isBusStopsCached(): Boolean = coroutineScope {
        galwayBusCache.isBusStopsCached()
    }

    override suspend fun getNumberBusStops(): Int = coroutineScope {
        galwayBusCache.getNumberBusStops()
    }

    override suspend fun getNearestBusStops(location: Location): Result<List<BusStop>> = coroutineScope {
        Result.Success(emptyList<BusStop>())
    }

    override suspend fun getBusStops(routeId: String): List<List<BusStop>> = coroutineScope {
        emptyList<List<BusStop>>()
    }

    override suspend fun getDepartures(stopRef: String): List<Departure> = coroutineScope {
        emptyList<Departure>()
    }

    override suspend fun getSchedules(): Map<String, RouteSchedule> = coroutineScope {
        emptyMap<String, RouteSchedule>()
    }

    override suspend fun getBusListForRoute(routeId: String): Result<List<Bus>> = coroutineScope {
        Result.Success(emptyList<Bus>())
    }

}