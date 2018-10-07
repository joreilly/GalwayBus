package com.surrus.galwaybus.domain.repository

import com.surrus.galwaybus.model.*
import kotlinx.coroutines.Deferred

interface GalwayBusRepository {

    suspend fun getBusRoutes(): Deferred<List<BusRoute>>
    suspend fun saveBusRoutes(busRoutes: List<BusRoute>) : Deferred<Unit>
    suspend fun clearBusRoutes() : Deferred<Unit>

    suspend fun saveBusStops(busStops: List<BusStop>) : Deferred<Unit>
    suspend fun clearBusStops() : Deferred<Unit>


    suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>>
    suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>>
    suspend fun getBusStopsByName(name: String) : Deferred<List<BusStop>>

    suspend fun getDepartures(stopRef: String): Deferred<List<Departure>>
    suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>>
}