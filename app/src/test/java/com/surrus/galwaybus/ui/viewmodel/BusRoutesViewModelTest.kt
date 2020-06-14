package com.surrus.galwaybus.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.BusRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Test

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock

@RunWith(JUnit4::class)
class BusRoutesViewModelTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock lateinit var galwayBusRepository: GalwayBusRepository

    private lateinit var busRoutesViewModel: BusRoutesViewModel

    @Before
    fun setUp() {
        galwayBusRepository = mock()

        Dispatchers.setMain(Dispatchers.Unconfined)
        busRoutesViewModel = BusRoutesViewModel(galwayBusRepository)
    }


    @Test
    fun getRoutes() = runBlocking {
        val busRouteList = mutableListOf<BusRoute>()
        busRouteList.add(BusRoute("1", "route 1 long", "route 1"))
        busRouteList.add(BusRoute("2", "route 2 long", "route 2"))

        whenever(galwayBusRepository.fetchBusRoutes()).thenReturn(busRouteList)
        busRoutesViewModel.fetchRoutes()
        assert(busRoutesViewModel.busRoutes.value == busRouteList)
    }

}
