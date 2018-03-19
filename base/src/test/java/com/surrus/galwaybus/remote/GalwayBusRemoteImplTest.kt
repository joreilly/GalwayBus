package com.surrus.galwaybus.remote

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.surrus.galwaybus.factory.GalwayBusFactory
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.GetDeparturesResponse
import io.reactivex.Flowable
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
    fun getBusRoutes() {
        val busRouteList = GalwayBusFactory.makeBusRouteList(2)
        val busRouteResponse = LinkedHashMap<String, BusRoute>()
        busRouteList.forEach {
            busRouteResponse.put(it.timetableId, it)
        }
        val busRouteResponseFlowable = Flowable.just(busRouteResponse)

        whenever(galwayBusService.getBusRoutes()).thenReturn(busRouteResponseFlowable)
        val testObserver = galwayBusRemoteImpl.getBusRoutes().test()
        testObserver.assertValue(busRouteList)
    }


    @Test
    fun getDepartures() {
        val departureTimes = GalwayBusFactory.makeDepartureList(3)
        val busStop = GalwayBusFactory.makeBusStop()
        val getDeparturesResponse = GetDeparturesResponse(busStop, departureTimes)

        whenever(galwayBusService.getDepartures(any())).thenReturn(Flowable.just(getDeparturesResponse))
        val testObserver = galwayBusRemoteImpl.getDepartures("some_stop_ref").test()
        testObserver.assertValue(departureTimes)
    }

}