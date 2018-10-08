package com.surrus.galwaybus.remote

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.surrus.galwaybus.factory.GalwayBusFactory
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.GetDeparturesResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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

        whenever(galwayBusService.getBusRoutes()).thenReturn(async { busRouteResponse })
        val br = galwayBusRemoteImpl.getBusRoutes()
        assert(br.size == 2)
    }


    @Test
    fun getDepartures() = runBlocking {
        val departureTimes = GalwayBusFactory.makeDepartureList(3)
        val busStop = GalwayBusFactory.makeBusStop()
        val getDeparturesResponse = GetDeparturesResponse(busStop, departureTimes)

        whenever(galwayBusService.getDepartures(any())).thenReturn(async { getDeparturesResponse })
        val bs = galwayBusRemoteImpl.getDepartures("some_stop_ref")
        assert(bs.size == 3)
    }

}