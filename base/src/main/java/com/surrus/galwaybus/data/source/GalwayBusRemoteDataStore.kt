package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*



class GalwayBusRemoteDataStore constructor(private val galwayBusRemote: GalwayBusRemote) : GalwayBusDataStore {

    override suspend fun getBusRoutes(): List<BusRoute> {
        return galwayBusRemote.getBusRoutes()
    }

    override suspend fun getBusStops(routeId: String): List<List<BusStop>> {
        return galwayBusRemote.getBusStops(routeId)
    }

    override suspend fun getBusStops() : List<BusStop> {
        return galwayBusRemote.getAllStops()
    }

    override suspend fun getNearestBusStops(location: Location): List<BusStop> {
        return galwayBusRemote.getNearestBusStops(location)
    }

    override suspend fun getBusStopsByName(name: String) : List<BusStop> {
        throw UnsupportedOperationException()
    }

    override suspend fun getDepartures(stopRef: String): List<Departure> {
        return galwayBusRemote.getDepartures(stopRef)
    }


    override suspend fun getSchedules(): Map<String, RouteSchedule> {
        return galwayBusRemote.getSchedules()
    }

    override suspend fun clearBusRoutes()  {
        throw UnsupportedOperationException()
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>)  {
        throw UnsupportedOperationException()
    }

    override suspend fun isCached(): Boolean {
        throw UnsupportedOperationException()
    }

    override suspend fun clearBusStops()  {
        throw UnsupportedOperationException()
    }

    override suspend fun saveBusStops(busStopList: List<BusStop>)  {
        throw UnsupportedOperationException()
    }

    override suspend fun isBusStopsCached() : Boolean  {
        throw UnsupportedOperationException()
    }

    override suspend fun getNumberBusStops(): Int {
        throw UnsupportedOperationException()
    }

}