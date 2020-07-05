package com.surrus.galwaybus

import android.app.Application
import com.nhaarman.mockitokotlin2.mock
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.appContext
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.common.remote.RTPIApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock


@RunWith(JUnit4::class)
class GalwayBusTest {

    private val galwayBusApi = GalwayBusApi()

    private val api = RTPIApi()


    @Test
    fun fetchSchedules() = runBlocking  {
        val result = galwayBusApi.fetchSchedules()
        println(result)
    }


    @Test
    fun fetchNearestStops() = runBlocking  {
        val result = galwayBusApi.fetchNearestStops(53.2743394, -9.0514163)
        println(result)
    }


    @Test
    fun fetchAllBusStops() = runBlocking  {
        val result = galwayBusApi.fetchAllBusStops()
        println(result)
    }

    @Test
    fun fetchRouteStops() = runBlocking  {
        val result = galwayBusApi.fetchRouteStops("401")
        println(result)
    }

    @Test
    fun fetchBusRoutes() = runBlocking  {
        val result = galwayBusApi.fetchBusRoutes()
        println(result)
    }

    @Test
    fun fetchBusListForRoute() = runBlocking  {
        val result = galwayBusApi.fetchBusListForRoute("401")
        println(result)
    }

    @Mock
    private var context: Application = mock()

    @Test
    fun fetchRouteInformation() = runBlocking {

//        val routes = listOf("401") //, "402", "403", "404", "405", "407", "409", "410")
//        routes.forEach {route ->
//            val result = api.getRouteInformation(route)
//            println(result)
//        }

        appContext = context

        val repo = GalwayBusRepository()

        val center = Location(53.2743394, -9.0514163)

        val nearestStopsResult = repo.getNearestStops(center)
//        val result = api.getBusStopInformation()
//        val nearestPoints = result.results.map { stop ->
//            stop to center.distance((Location(stop.latitude.toDouble(), stop.longitude.toDouble())))      //poses.sortedBy { point.distance(it) }.drop(1).take(10)
//        }.sortedBy { it.second }.take(10)
        //println(nearestPoints)

        if (nearestStopsResult is Result.Success) {
            nearestStopsResult.data.forEach { stop ->

                async {
                    val result = api.getRealtimeBusInformation(stop.stopid)
                    println(stop.shortname)

                    result.results.forEach {
                        println(it.duetime + " " + it.route)
                    }
                }

            }
        }

    }


    @Test
    fun fetchRealtimeBusInformation() = runBlocking {

        val result = api.getRealtimeBusInformation("522301")
        println(result)
    }

}