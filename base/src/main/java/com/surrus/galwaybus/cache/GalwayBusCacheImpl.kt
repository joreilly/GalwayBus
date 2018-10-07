package com.surrus.galwaybus.cache

import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop


class GalwayBusCacheImpl constructor(val galwayBusDatabase: GalwayBusDatabase,
                                             private val preferencesHelper: PreferencesHelper) : GalwayBusCache {

    private val EXPIRATION_TIME = (60 * 10 * 1000).toLong()


    /**
     * Retrieve an instance from the database, used for tests.
     */
    internal fun getDatabase(): GalwayBusDatabase {
        return galwayBusDatabase
    }


    override fun saveBusRoutes(busRoutes: List<BusRoute>) {
        busRoutes.forEach {
            galwayBusDatabase.galwayBusDao().insertBusRoute(it)
        }
    }

    override fun getBusRoutes() = galwayBusDatabase.galwayBusDao().getBusRoutes()

    override fun clearBusRoutes() = galwayBusDatabase.galwayBusDao().clearBusRoutes()

    override fun isCached() = galwayBusDatabase.galwayBusDao().getBusRoutes().isNotEmpty()

    override fun saveBusStops(busStopList: List<BusStop>) = galwayBusDatabase.galwayBusDao().insertBusStopList(busStopList)

    override fun getBusStops() = galwayBusDatabase.galwayBusDao().qeuryBusStops()

    override fun clearBusStops() = galwayBusDatabase.galwayBusDao().clearBusStops()

    override fun isBusStopsCached() = galwayBusDatabase.galwayBusDao().getBusStops().isNotEmpty()

    override fun getNumberBusStops() = galwayBusDatabase.galwayBusDao().getNumberBusStops()

    override fun getBusStopsByName(name: String) = galwayBusDatabase.galwayBusDao().getBusStopsByName(name)


    override fun setLastCacheTime(lastCache: Long) {
        preferencesHelper.lastCacheTime = lastCache
    }

    override fun isExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastUpdateTime = this.getLastCacheUpdateTimeMillis()
        return currentTime - lastUpdateTime > EXPIRATION_TIME
    }

    /**
     * Get in millis, the last time the cache was accessed.
     */
    private fun getLastCacheUpdateTimeMillis(): Long {
        return preferencesHelper.lastCacheTime
    }

}