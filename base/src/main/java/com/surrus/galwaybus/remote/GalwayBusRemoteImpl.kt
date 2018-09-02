package com.surrus.galwaybus.remote

import com.orhanobut.logger.Logger
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*
import io.reactivex.Flowable


class GalwayBusRemoteImpl  constructor(private val galwayBusService: GalwayBusService) : GalwayBusRemote {

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return galwayBusService.getBusRoutes()
                .map {
                    val busRouteList = mutableListOf<BusRoute>()
                    it.values.forEach {
                        busRouteList.add(it)
                    }
                    busRouteList
                }
    }


    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return galwayBusService.getStops(routeId)
                .map { it.stops }
    }

    override fun getAllStops() : Flowable<List<BusStop>> {
        return galwayBusService.getAllStops()
    }

    override fun getNearestBusStops(location: Location): Flowable<List<BusStop>> {
        return galwayBusService.getNearestStops(location.latitude, location.longitude)
                .doOnNext { Logger.d("Got nearest bus stop info: ${location.latitude}, ${location.longitude}") }
    }

    override fun getDepartures(stopRef: String): Flowable<List<Departure>> {
        return galwayBusService.getDepartures(stopRef)
                .map { it.departureTimes }
    }


    override fun getSchedules(): Flowable<Map<String, RouteSchedule>> {
        return galwayBusService.getSchedules()
                .map {
                    val scheduleMap = HashMap<String, RouteSchedule>()
                    val response = it
                    it.keys.forEach {
                        val schedule = response.get(it)!![0]
                        for (key in schedule.keys) {
                            val routeName = key
                            val pdfUrl = schedule[key]
                            scheduleMap.put(it, RouteSchedule(it, routeName, pdfUrl!!))
                        }
                    }
                    scheduleMap
                }
    }

}