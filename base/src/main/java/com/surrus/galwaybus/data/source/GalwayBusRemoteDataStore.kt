package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*
import kotlinx.coroutines.Deferred


class GalwayBusRemoteDataStore constructor(private val galwayBusRemote: GalwayBusRemote) : GalwayBusDataStore {

    override suspend fun getBusRoutes(): Deferred<List<BusRoute>> {
        return galwayBusRemote.getBusRoutes()
    }

    override suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>> {
        return galwayBusRemote.getBusStops(routeId)
    }

    override suspend fun getBusStops() : Deferred<List<BusStop>> {
        return galwayBusRemote.getAllStops()
    }

    override suspend fun getNearestBusStops(location: Location): Deferred<List<BusStop>> {
        return galwayBusRemote.getNearestBusStops(location)
    }

    override suspend fun getBusStopsByName(name: String) : Deferred<List<BusStop>> {
        throw UnsupportedOperationException()
    }

    override suspend fun getDepartures(stopRef: String): Deferred<List<Departure>> {
        return galwayBusRemote.getDepartures(stopRef)
    }


    override suspend fun getSchedules(): Deferred<Map<String, RouteSchedule>> {
        return galwayBusRemote.getSchedules()
    }

    override suspend fun clearBusRoutes() : Deferred<Unit> {
        throw UnsupportedOperationException()
    }

    override suspend fun saveBusRoutes(busRoutes: List<BusRoute>) : Deferred<Unit> {
        throw UnsupportedOperationException()
    }

    override suspend fun isCached(): Deferred<Boolean> {
        throw UnsupportedOperationException()
    }

    override suspend fun clearBusStops() : Deferred<Unit> {
        throw UnsupportedOperationException()
    }

    override suspend fun saveBusStops(busStops: List<BusStop>) : Deferred<Unit> {
        throw UnsupportedOperationException()
    }

    override suspend fun isBusStopsCached(): Deferred<Boolean> {
        throw UnsupportedOperationException()
    }

    override suspend fun getNumberBusStops(): Deferred<Int> {
        throw UnsupportedOperationException()
    }

}