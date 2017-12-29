package com.surrus.galwaybus.cache.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.surrus.galwaybus.cache.db.GalwayBusDatabaseConstants
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import android.arch.lifecycle.LiveData



@Dao
abstract class GalwayBusDao {

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_ROUTES)
    abstract fun getBusRoutes(): List<BusRoute>

    @Query(GalwayBusDatabaseConstants.DELETE_ALL_BUS_ROUTES)
    abstract fun clearBusRoutes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBusRoute(busRoute: BusRoute)


    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS)
    abstract fun getBusStops(): List<BusStop>

    @Query(GalwayBusDatabaseConstants.DELETE_ALL_BUS_STOPS)
    abstract fun clearBusStops()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBusStop(busStop: BusStop)


    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS_BY_NAME)
    abstract fun getBusStopsByName(longNameText: String): List<BusStop>

}