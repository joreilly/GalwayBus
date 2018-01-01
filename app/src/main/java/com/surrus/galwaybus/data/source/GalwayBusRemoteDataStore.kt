package com.surrus.galwaybus.data.source

import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.model.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

class GalwayBusRemoteDataStore @Inject constructor(private val galwayBusRemote: GalwayBusRemote) : GalwayBusDataStore {

    override fun getBusRoutes(): Flowable<List<BusRoute>> {
        return galwayBusRemote.getBusRoutes()
    }

    override fun getBusStops(routeId: String): Flowable<List<List<BusStop>>> {
        return galwayBusRemote.getBusStops(routeId)
    }

    override fun getBusStops() : Flowable<List<BusStop>> {
        return galwayBusRemote.getAllStops()
    }

    override fun getNearestBusStops(location: Location): Flowable<List<BusStop>> {
        return galwayBusRemote.getNearestBusStops(location)
    }

    override fun getBusStopsByName(name: String) : Flowable<List<BusStop>> {
        throw UnsupportedOperationException()
    }

    override fun getDepartures(stopRef: String): Flowable<List<Departure>> {
        return galwayBusRemote.getDepartures(stopRef)
    }


    override fun getSchedules(): Flowable<Map<String, RouteSchedule>> {
        return galwayBusRemote.getSchedules()
    }

    override fun clearBusRoutes(): Completable {
        throw UnsupportedOperationException()
    }

    override fun saveBusRoutes(bufferoos: List<BusRoute>): Completable {
        throw UnsupportedOperationException()
    }

    override fun isCached(): Single<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun clearBusStops(): Completable {
        throw UnsupportedOperationException()
    }

    override fun saveBusStops(busStops: List<BusStop>): Completable {
        throw UnsupportedOperationException()
    }

    override fun isBusStopsCached(): Single<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun getNumberBusStops(): Single<Int> {
        throw UnsupportedOperationException()
    }

}