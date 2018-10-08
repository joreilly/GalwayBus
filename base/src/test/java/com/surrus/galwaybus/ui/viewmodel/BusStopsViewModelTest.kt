package com.surrus.galwaybus.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockito_kotlin.*
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import junit.framework.Assert.assertTrue
import org.junit.Test

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock

@RunWith(JUnit4::class)
class BusStopsViewModelTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock lateinit var getBusStopsUseCase: GetBusStopsUseCase

    @Mock lateinit var observer: Observer<List<BusStop>>

    private lateinit var busStopsViewModel: BusStopsViewModel

    @Before
    fun setUp() {
        observer = mock()
        getBusStopsUseCase = mock()
        busStopsViewModel = BusStopsViewModel(getBusStopsUseCase)
    }


    @Test
    fun getBusStops() {
        val busStopsList = mutableListOf<List<BusStop>>()

        val busStopsDir0 = mutableListOf<BusStop>()
        busStopsDir0.add(BusStop(1, "short name"))
        busStopsDir0.add(BusStop(2, "short name"))

        val busStopsDir1 = mutableListOf<BusStop>()
        busStopsDir1.add(BusStop(3, "short name"))
        busStopsDir1.add(BusStop(4, "short name"))

        busStopsList.add(busStopsDir0)
        busStopsList.add(busStopsDir1)

        busStopsViewModel.busStops.observeForever(observer)


        busStopsViewModel.fetchBusStops("some route id")
        assertTrue(busStopsViewModel.busStops.value!!.equals(busStopsDir0))


        busStopsViewModel.setDirection(1)
        assertTrue(busStopsViewModel.busStops.value!!.equals(busStopsDir1))

        busStopsViewModel.setDirection(0)
        assertTrue(busStopsViewModel.busStops.value!!.equals(busStopsDir0))

    }

}
