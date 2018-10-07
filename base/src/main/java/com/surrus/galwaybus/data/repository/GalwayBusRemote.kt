package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*
import kotlinx.coroutines.Deferred

interface GalwayBusRemote {
    suspend fun getBusRoutes(): Deferred<List<BusRoute>>
    suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>>
    suspend fun getAllStops() : Deferred<List<BusStop>>
    suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>>
    suspend fun getDepartures(stopRef: String): Deferred<List<Departure>>
    suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>>
}