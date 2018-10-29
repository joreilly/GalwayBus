package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*


interface GalwayBusDataStore {
    suspend fun clearBusRoutes()
    suspend fun saveBusRoutes(busRoutes: List<BusRoute>)
    suspend fun getBusRoutes(): List<BusRoute>
    suspend fun isCached(): Boolean

    suspend fun clearBusStops()
    suspend fun saveBusStops(busStopList: List<BusStop>)
    suspend fun getBusStops(): List<BusStop>
    suspend fun getBusStopsByName(name: String) : List<BusStop>
    suspend fun isBusStopsCached(): Boolean
    suspend fun getNumberBusStops(): Int

    suspend fun getNearestBusStops(location: Location): Result<List<BusStop>>
    suspend fun getBusStops(routeId: String): List<List<BusStop>>

    suspend fun getDepartures(stopRef: String): List<Departure>
    suspend fun getSchedules(): Map<String, RouteSchedule>
}