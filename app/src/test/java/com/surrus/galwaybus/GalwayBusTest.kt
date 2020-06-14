package com.surrus.galwaybus

import com.surrus.galwaybus.common.remote.GalwayBusApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GalwayBusTest {

    private val galwayBusApi = GalwayBusApi()

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

}