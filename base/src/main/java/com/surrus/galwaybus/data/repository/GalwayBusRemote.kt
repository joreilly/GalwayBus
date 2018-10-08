package com.surrus.galwaybus.data.repository

import com.surrus.galwaybus.model.*


interface GalwayBusRemote {
    suspend fun getBusRoutes(): List<BusRoute>
    suspend fun getBusStops(routeId: String): List<List<BusStop>>
    suspend fun getAllStops() : List<BusStop>
    suspend fun getNearestBusStops(location: Location): List<BusStop>
    suspend fun getDepartures(stopRef: String): List<Departure>
    suspend fun getSchedules(): Map<String, RouteSchedule>
}