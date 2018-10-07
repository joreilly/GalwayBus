package com.surrus.galwaybus.remote

import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class GalwayBusRemoteImpl  constructor(private val galwayBusService: GalwayBusService) : GalwayBusRemote {

    override suspend fun getBusRoutes(): Deferred<List<BusRoute>> = coroutineScope {
        async {
            val busRoutes = galwayBusService.getBusRoutes().await()
            val busRouteList = mutableListOf<BusRoute>()
            busRoutes.values.forEach {
                busRouteList.add(it)
            }
            busRouteList
        }
    }


    override suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>> = coroutineScope {
        async {
            val stopsResponse = galwayBusService.getStops(routeId).await()
            stopsResponse.stops
        }
    }

    override suspend fun getAllStops() : Deferred<List<BusStop>> {
        return galwayBusService.getAllStops()
    }

    override suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>> {
        return galwayBusService.getNearestStops(location.latitude, location.longitude)
    }

    override suspend fun getDepartures(stopRef: String): Deferred<List<Departure>> = coroutineScope {
        async {
            val departures = galwayBusService.getDepartures(stopRef).await()
            departures.departureTimes
        }
    }


    override suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>> = coroutineScope {
        async {
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

}