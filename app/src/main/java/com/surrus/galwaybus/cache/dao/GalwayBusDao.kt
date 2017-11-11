package com.surrus.galwaybus.cache.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.surrus.galwaybus.cache.db.GalwayBusDatabaseConstants
import com.surrus.galwaybus.model.BusRoute

@Dao
abstract class GalwayBusDao {

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_ROUTES)
    abstract fun getBusRoutes(): List<BusRoute>

    @Query(GalwayBusDatabaseConstants.DELETE_ALL_BUS_ROUTES)
    abstract fun clearBusRoutes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBusRoute(busRoute: BusRoute)

}