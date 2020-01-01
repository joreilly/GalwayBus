package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore


open class GalwayBusDataStoreFactory constructor(
        private val galwayBusCache: GalwayBusCache,
        private val galwayBusCacheDataStore: GalwayBusCacheDataStore,
        private val galwayBusRemoteDataStore: GalwayBusRemoteDataStore) {

    /**
     * Returns a DataStore based on whether or not there is content in the cache and the cache
     * has not expired
     */
    open fun retrieveDataStore(isCached: Boolean): GalwayBusDataStore {
        if (isCached && !galwayBusCache.isExpired()) {
            return retrieveCacheDataStore()
        }
        return retrieveRemoteDataStore()
    }

    /**
     * Return an instance of the Cache Data Store
     */
    open fun retrieveCacheDataStore(): GalwayBusDataStore {
        return galwayBusCacheDataStore
    }


    /**
     * Return an instance of the Remote Data Store
     */
    open fun retrieveRemoteDataStore(): GalwayBusDataStore {
        return galwayBusRemoteDataStore
    }

}