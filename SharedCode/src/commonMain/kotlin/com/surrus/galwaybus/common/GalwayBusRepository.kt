package com.surrus.galwaybus.common

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.surrus.galwaybus.common.model.*
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


expect fun createDb() : MyDatabase?

@ExperimentalTime
data class GalwayBusDeparture(
        val timetableId: String,
        val displayName: String,
        val departTimestamp: String,
        val durationUntilDeparture: Duration)


@ExperimentalTime
open class GalwayBusRepository {
    private val galwayBusApi = GalwayBusApi()
    private val galwayBusDb = createDb()
    private val galwayBusQueries = galwayBusDb?.galwayBusQueries
    private val coroutineScope: CoroutineScope = MainScope()

    init {
        coroutineScope.launch {
            // TODO have "staleness check" here?
            fetchAndStoreBusStops()
        }
    }


    private suspend fun fetchAndStoreBusStops() {
        try {
            val existingBusStops = getBusStops()
            if (existingBusStops.isEmpty()) {
                val busStops = galwayBusApi.fetchAllBusStops()

                val galwayBusStops = busStops.filter { it.distance < 10000.0 }
                galwayBusStops.forEach {
                    galwayBusQueries?.insertItem(it.stop_id, it.stopRef, it.shortName, it.longName, it.latitude, it.longitude)
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    @ExperimentalCoroutinesApi
    fun getBusStopsFlow() = galwayBusQueries?.selectAll(mapper = { stop_id, stop_ref, short_name, long_name, latitude, longitude ->
            BusStop(stop_id, short_name, long_name, stop_ref, latitude = latitude, longitude = longitude)
        })?.asFlow()?.mapToList()


    fun getBusStops(): List<BusStop> {
        return galwayBusQueries?.selectAll(mapper = { stop_id, short_name, long_name, stop_ref, latitude, longitude  ->
            BusStop(stop_id, short_name, long_name, stop_ref, latitude = latitude, longitude = longitude)
        })?.executeAsList() ?: emptyList<BusStop>()
    }


    suspend fun fetchRouteStops(routeId: String): Result<List<List<BusStop>>> {
        try {
            val busStopLists = galwayBusApi.fetchRouteStops(routeId)
            return Result.Success(busStopLists)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun fetchBusListForRoute(routeId: String): Result<List<Bus>> {
        try {
            val busList = galwayBusApi.fetchBusListForRoute(routeId)
            return Result.Success(busList)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun fetchBusStopDepartures(stopRef: String): Result<List<GalwayBusDeparture>> {
        try {
            val busStopResponse = galwayBusApi.fetchBusStop(stopRef)

            val now = Clock.System.now()
            val departures = busStopResponse.times
                .map {
                    val departureTime = Instant.parse(it.departTimestamp)
                    val durationUntilDeparture: Duration = departureTime - now
                    GalwayBusDeparture(it.timetableId, it.displayName, it.departTimestamp,
                            durationUntilDeparture)
                }
                .take(5)

            return Result.Success(departures)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    fun fetchBusListForRoute(routeId: String, success: (List<Bus>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val busList = galwayBusApi.fetchBusListForRoute(routeId)
            success(busList)
        }
    }


    fun getBusStops(success: (List<BusStop>) -> Unit) {
        coroutineScope.launch {
            getBusStopsFlow()?.collect {
                success(it)
            }
        }
    }

    suspend fun fetchSchedules() = galwayBusApi.fetchSchedules()
        .mapValues {
            it.value[0].values.elementAt(0)
        }


    suspend fun fetchBusRoutes(): List<BusRoute> {
        val busRoutes = galwayBusApi.fetchBusRoutes()
        return transformBusRouteMapToList(busRoutes)
    }

    fun fetchBusRoutes(success: (List<BusRoute>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            success(fetchBusRoutes())
        }
    }

    suspend fun fetchNearestStops(latitude: Double, longitude: Double): Result<List<BusStop>> {
        try {
            val busStops = galwayBusApi.fetchNearestStops(latitude, longitude)
            return Result.Success(busStops)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    private fun transformBusRouteMapToList(busRoutesMap: Map<String, BusRoute>): List<BusRoute> {
        val busRouteList = mutableListOf<BusRoute>()
        busRoutesMap.values.forEach {
            busRouteList.add(it)
        }
        return busRouteList
    }

}