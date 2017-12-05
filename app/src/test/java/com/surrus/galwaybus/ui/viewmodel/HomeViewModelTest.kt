package com.surrus.galwaybus.ui.viewmodel

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.*
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
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
class HomeViewModelTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock lateinit var getBusRoutesUseCase: GetBusRoutesUseCase

    @Captor
    private lateinit var captor: KArgumentCaptor<DisposableSubscriber<List<BusRoute>>>

    private lateinit var homeViewModel: BusRoutesViewModel

    @Before
    fun setUp() {
        captor = argumentCaptor<DisposableSubscriber<List<BusRoute>>>()
        getBusRoutesUseCase = mock()
        homeViewModel = BusRoutesViewModel(getBusRoutesUseCase)
    }


    @Test
    fun getRoutes() {
        val busRouteList = mutableListOf<BusRoute>()
        busRouteList.add(BusRoute("1", "route 1 long", "route 1"))
        busRouteList.add(BusRoute("2", "route 2 long", "route 2"))

        verify(getBusRoutesUseCase).execute(captor.capture(), eq(null))
        captor.firstValue.onNext(busRouteList)

        assert(homeViewModel.getBusRoutes().value == busRouteList)
    }

}
