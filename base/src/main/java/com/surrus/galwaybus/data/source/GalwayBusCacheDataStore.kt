package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


open class GalwayBusCacheDataStore constructor(private val galwayBusCache: GalwayBusCache) : GalwayBusDataStore {

    override suspend fun clearBusRoutes() : Deferred<Unit> = coroutineScope {
        async {
            galwayBusCache.clearBusRoutes()
        }
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>) : Deferred<Unit> = coroutineScope {
        async {
            galwayBusCache.saveBusRoutes(busRoutes)
            galwayBusCache.setLastCacheTime(System.currentTimeMillis())
        }
    }

    override suspend fun getBusRoutes(): Deferred<List<BusRoute>> = coroutineScope {
        async {
            galwayBusCache.getBusRoutes()
        }
    }

    override suspend fun isCached(): Deferred<Boolean> = coroutineScope {
        async {
            galwayBusCache.isCached()
        }
    }


    override suspend fun clearBusStops() : Deferred<Unit> = coroutineScope {
        async {
            galwayBusCache.clearBusStops()
        }
    }

    override suspend fun saveBusStops(busStopList: List<BusStop>): Deferred<Unit> = coroutineScope {
        async {
            galwayBusCache.saveBusStops(busStopList)
        }
    }

    override suspend fun getBusStops(): Deferred<List<BusStop>>  = coroutineScope {
        async {
            galwayBusCache.getBusStops()
        }
    }

    override suspend fun getBusStopsByName(name: String) : Deferred<List<BusStop>> = coroutineScope {
        async {
            galwayBusCache.getBusStopsByName(name)
        }
    }

    override suspend fun isBusStopsCached(): Deferred<Boolean> = coroutineScope {
        async {
            galwayBusCache.isBusStopsCached()
        }
    }

    override suspend fun getNumberBusStops(): Deferred<Int> = coroutineScope {
        async {
            galwayBusCache.getNumberBusStops()
        }
    }

    override suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>> = coroutineScope {
        async {
            emptyList<BusStop>()
        }
    }

    override suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>> = coroutineScope {
        async {
            emptyList<List<BusStop>>()
        }
    }

    override suspend fun getDepartures(stopRef: String): Deferred<List<Departure>> = coroutineScope {
        async {
            emptyList<Departure>()
        }
    }

    override suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>> = coroutineScope {
        async {
            emptyMap<String, RouteSchedule>()
        }
    }

}