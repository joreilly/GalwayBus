package com.surrus.galwaybus.remote

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.surrus.galwaybus.factory.GalwayBusFactory
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.GetDeparturesResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

@RunWith(JUnit4::class)
class GalwayBusRemoteImplTest {

    private lateinit var galwayBusRemoteImpl: GalwayBusRemoteImpl
    private lateinit var galwayBusService: GalwayBusService

    @Before
    fun setup() {
        galwayBusService = mock()
        galwayBusRemoteImpl = GalwayBusRemoteImpl(galwayBusService)
    }

    @Test
    fun getBusRoutes() = runBlocking  {
        val busRouteList = GalwayBusFactory.makeBusRouteList(2)
        val busRouteResponse = LinkedHashMap<String, BusRoute>()
        busRouteList.forEach {
            busRouteResponse.put(it.timetableId, it)
        }
        val response = Response.success(busRouteResponse)

        whenever(galwayBusService.getBusRoutes()).thenReturn(async { response })
        val br = galwayBusRemoteImpl.getBusRoutes()
        assert(br.size == 2)
    }


    @Test
    fun getDepartures() = runBlocking {
        val departureTimes = GalwayBusFactory.makeDepartureList(3)
        val busStop = GalwayBusFactory.makeBusStop()
        val getDeparturesResponse = GetDeparturesResponse(busStop, departureTimes)
        val response = Response.success(getDeparturesResponse)

        whenever(galwayBusService.getDepartures(any())).thenReturn(async { response })
        val bs = galwayBusRemoteImpl.getDepartures("some_stop_ref")
        assert(bs.size == 3)
    }

}