package com.surrus.galwaybus.cache

import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

class GalwayBusCacheImpl @Inject constructor(val galwayBusDatabase: GalwayBusDatabase,
                                             private val preferencesHelper: PreferencesHelper) : GalwayBusCache {

    private val EXPIRATION_TIME = (60 * 10 * 1000).toLong()


    /**
     * Retrieve an instance from the database, used for tests.
     */
    internal fun getDatabase(): GalwayBusDatabase {
        return galwayBusDatabase
    }


    override fun saveBusRoutes(busRoutes: List<BusRoute>): Completable {
        return Completable.defer {
            busRoutes.forEach {
                galwayBusDatabase.galwayBusDao().insertBusRoute(it)
            }
            Completable.complete()
        }
    }

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return Flowable.defer {
            Flowable.just(galwayBusDatabase.galwayBusDao().getBusRoutes())
        }
    }

    override fun clearBusRoutes(): Completable {
        return Completable.defer {
            galwayBusDatabase.galwayBusDao().clearBusRoutes()
            Completable.complete()
        }
    }


    override fun isCached(): Single<Boolean> {
        return Single.defer {
            Single.just(galwayBusDatabase.galwayBusDao().getBusRoutes().isNotEmpty())
        }
    }

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