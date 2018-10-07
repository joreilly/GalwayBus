package com.surrus.galwaybus.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.surrus.galwaybus.cache.db.GalwayBusDatabaseConstants
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop



@Dao
abstract class GalwayBusDao {

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_ROUTES)
    abstract fun getBusRoutes(): List<BusRoute>

    @Query(GalwayBusDatabaseConstants.DELETE_ALL_BUS_ROUTES)
    abstract fun clearBusRoutes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBusRoute(busRoute: BusRoute)

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS_COUNNT)
    abstract fun getNumberBusStops(): Int

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS)
    abstract fun getBusStops(): List<BusStop>

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS)
    abstract fun qeuryBusStops(): List<BusStop>

    @Query(GalwayBusDatabaseConstants.DELETE_ALL_BUS_STOPS)
    abstract fun clearBusStops()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBusStopList(busStopList: List<BusStop>)

    @Query(GalwayBusDatabaseConstants.QUERY_BUS_STOPS_BY_NAME)
    abstract fun getBusStopsByName(longNameText: String): List<BusStop>

}