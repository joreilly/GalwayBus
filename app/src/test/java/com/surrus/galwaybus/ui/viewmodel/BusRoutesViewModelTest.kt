package com.surrus.galwaybus.ui.viewmodel

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.*
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.subscribers.DisposableSubscriber
import org.junit.Test

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Captor
import org.mockito.Mock

@RunWith(JUnit4::class)
class BusRoutesViewModelTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock lateinit var getBusRoutesUseCase: GetBusRoutesUseCase

    @Captor
    private lateinit var captor: KArgumentCaptor<DisposableSubscriber<List<BusRouteSchedule>>>

    private lateinit var busRoutesViewModel: BusRoutesViewModel

    @Before
    fun setUp() {
        captor = argumentCaptor<DisposableSubscriber<List<BusRouteSchedule>>>()
        getBusRoutesUseCase = mock()
        busRoutesViewModel = BusRoutesViewModel(getBusRoutesUseCase)
    }


    @Test
    fun getRoutes() {
        val busRouteList = mutableListOf<BusRouteSchedule>()
        busRouteList.add(BusRouteSchedule("1", "route 1 long", "route 1", "route 1 pdf"))
        busRouteList.add(BusRouteSchedule("2", "route 2 long", "route 2", "route 2 pdf"))

        verify(getBusRoutesUseCase).execute(captor.capture(), eq(null))
        captor.firstValue.onNext(busRouteList)

        assert(busRoutesViewModel.getBusRoutes().value == busRouteList)
    }

}
