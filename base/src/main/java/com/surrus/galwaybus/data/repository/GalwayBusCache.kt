package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop


interface GalwayBusCache {
    fun clearBusRoutes()
    fun saveBusRoutes(busRoutes: List<BusRoute>)
    fun getBusRoutes(): List<BusRoute>
    fun isCached(): Boolean

    fun clearBusStops()
    fun saveBusStops(busStopList: List<BusStop>)
    fun getBusStops(): List<BusStop>
    fun isBusStopsCached(): Boolean
    fun getNumberBusStops(): Int

    fun getBusStopsByName(name: String) : List<BusStop>

    fun setLastCacheTime(lastCache: Long)
    fun isExpired(): Boolean
}