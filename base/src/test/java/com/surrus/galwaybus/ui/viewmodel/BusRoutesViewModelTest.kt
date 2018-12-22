package com.surrus.galwaybus.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.*
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock

@RunWith(JUnit4::class)
class BusRoutesViewModelTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock lateinit var getBusRoutesUseCase: GetBusRoutesUseCase

    private lateinit var busRoutesViewModel: BusRoutesViewModel

    @Before
    fun setUp() {
        getBusRoutesUseCase = mock()
        busRoutesViewModel = BusRoutesViewModel(getBusRoutesUseCase, Dispatchers.Unconfined)
    }


    @Test
    fun getRoutes() = runBlocking {
        val busRouteList = mutableListOf<BusRouteSchedule>()
        busRouteList.add(BusRouteSchedule("1", "route 1 long", "route 1", "route 1 pdf"))
        busRouteList.add(BusRouteSchedule("2", "route 2 long", "route 2", "route 2 pdf"))

        whenever(getBusRoutesUseCase.getBusRoutes()).thenReturn(async { busRouteList } )
        val br = busRoutesViewModel.fetchRoutes()
        assert(busRoutesViewModel.getBusRoutes().value == busRouteList)
    }

}
