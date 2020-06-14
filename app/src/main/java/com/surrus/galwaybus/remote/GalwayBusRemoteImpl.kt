package com.surrus.galwaybus.remote

import com.surrus.galwaybus.model.*
import java.io.IOException


class GalwayBusRemoteImpl  constructor(private val galwayBusService: GalwayBusService) {
/*
    override suspend fun getBusRoutes(): List<BusRoute> {
        try {
            val busRoutesResponse = galwayBusService.getBusRoutes().await()
            if (busRoutesResponse.isSuccessful) {
                val busRoutes = busRoutesResponse.body()
                val busRouteList = mutableListOf<BusRoute>()
                busRoutes?.values?.forEach {
                    busRouteList.add(it)
                }
                return busRouteList
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }


    override suspend fun getBusStops(routeId: String): List<List<BusStop>> {
        try {
            val stopsResponse = galwayBusService.getStops(routeId).await()
            if (stopsResponse.isSuccessful) {
                return stopsResponse.body()!!.stops
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override suspend fun getAllStops(): List<BusStop> {
        try {
            val stopsResponse = galwayBusService.getAllStops().await()
            if (stopsResponse.isSuccessful) {
                return stopsResponse.body()!!
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }


    override suspend fun getNearestBusStops(location: Location): Result<List<BusStop>> {
        try {
            val nearestBusStopsResponse = galwayBusService.getNearestStops(location.latitude, location.longitude).await()
            if (nearestBusStopsResponse.isSuccessful) {
                return Result.Success(nearestBusStopsResponse.body()!!)
            } else {
                return Result.Error(IOException("Error occurred fetching timetable information"))
            }
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }


    override suspend fun getDepartures(stopRef: String): List<Departure> {
        try {
            val departuresResponse = galwayBusService.getDepartures(stopRef).await()
            if (departuresResponse.isSuccessful) {
                return departuresResponse.body()!!.departureTimes
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }


    override suspend fun getSchedules(): Map<String, RouteSchedule>  {
        try {
            val schedulesResponse = galwayBusService.getSchedules().await()
            if (schedulesResponse.isSuccessful) {
                val scheduleMap = HashMap<String, RouteSchedule>()
                val schedules = schedulesResponse.body()
                schedules?.keys?.forEach {
                    val schedule = schedules.get(it)!![0]
                    for (key in schedule.keys) {
                        val routeName = key
                        val pdfUrl = schedule[key]
                        scheduleMap[it] = RouteSchedule(it, routeName, pdfUrl!!)
                    }
                }
                return scheduleMap
            } else {
                return emptyMap()
            }
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    override suspend fun getBusListForRoute(routeId: String): Result<List<Bus>> {
        try {
            val busListResponse = galwayBusService.getBusListForRoute(routeId).await()
            if (busListResponse.isSuccessful && busListResponse.body() != null) {
                return Result.Success(busListResponse.body()!!.bus)
            } else {
                return Result.Error(IOException("Error occurred fetching bus information"))
            }

        } catch (e: Exception) {
            return Result.Error(IOException("Error occurred fetching bus information"))
        }
    }
*/
}