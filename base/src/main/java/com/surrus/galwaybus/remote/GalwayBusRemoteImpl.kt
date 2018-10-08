package com.surrus.galwaybus.remote

import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.coroutineScope


class GalwayBusRemoteImpl  constructor(private val galwayBusService: GalwayBusService) : GalwayBusRemote {

    override suspend fun getBusRoutes(): List<BusRoute> = coroutineScope {
        val busRoutes = galwayBusService.getBusRoutes().await()
        val busRouteList = mutableListOf<BusRoute>()
        busRoutes.values.forEach {
            busRouteList.add(it)
        }
        busRouteList
    }


    override suspend fun getBusStops(routeId: String): List<List<BusStop>> = coroutineScope {
        galwayBusService.getStops(routeId).await().stops
    }

    override suspend fun getAllStops() : List<BusStop> = coroutineScope {
        galwayBusService.getAllStops().await()
    }

    override suspend fun getNearestBusStops(location: Location): List<BusStop> = coroutineScope {
        galwayBusService.getNearestStops(location.latitude, location.longitude).await()
    }

    override suspend fun getDepartures(stopRef: String): List<Departure> = coroutineScope {
        galwayBusService.getDepartures(stopRef).await().departureTimes
    }


    override suspend fun getSchedules(): Map<String, RouteSchedule> = coroutineScope {
        val schedules = galwayBusService.getSchedules().await()

        val scheduleMap = HashMap<String, RouteSchedule>()
        schedules.keys.forEach {
            val schedule = schedules.get(it)!![0]
            for (key in schedule.keys) {
                val routeName = key
                val pdfUrl = schedule[key]
                scheduleMap[it] = RouteSchedule(it, routeName, pdfUrl!!)
            }
        }
        scheduleMap
    }

}