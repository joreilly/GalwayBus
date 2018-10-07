package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*

import kotlinx.coroutines.Deferred

interface GalwayBusDataStore {
    suspend fun clearBusRoutes() : Deferred<Unit>
    suspend fun saveBusRoutes(busRoutes: List<BusRoute>) : Deferred<Unit>
    suspend fun getBusRoutes(): Deferred<List<BusRoute>>
    suspend fun isCached(): Deferred<Boolean>

    suspend fun clearBusStops() : Deferred<Unit>
    suspend fun saveBusStops(busStopList: List<BusStop>) : Deferred<Unit>
    suspend fun getBusStops(): Deferred<List<BusStop>>
    suspend fun getBusStopsByName(name: String) : Deferred<List<BusStop>>
    suspend fun isBusStopsCached(): Deferred<Boolean>
    suspend fun getNumberBusStops(): Deferred<Int>

    suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>>
    suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>>

    suspend fun getDepartures(stopRef: String): Deferred<List<Departure>>
    suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>>
}