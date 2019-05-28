package com.surrus.galwaybus.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.surrus.galwaybus.base.R
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.support.RecyclerViewMatchers.withRecyclerView
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import org.junit.Test
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class RouteListFragmentTest {

    @Test
    fun testRouteListFragment() {

        // setup mock TeamsViewModel
        val mockBusRoutesViewModel = mock<BusRoutesViewModel>()
        loadKoinModules(module {
            viewModel(override = true) {
                mockBusRoutesViewModel
            }
        })


        val busRoutes: MutableLiveData<List<BusRouteSchedule>> = MutableLiveData()

        val busRoute1 = BusRouteSchedule("401", "401 long name", "401 short name", "")
        val busRoute2 = BusRouteSchedule("402", "402 long name", "402 short name", "")
        val busRouteList = listOf(busRoute1, busRoute2)
        busRoutes.postValue(busRouteList)

        doReturn(busRoutes).whenever(mockBusRoutesViewModel).getBusRoutes()

        launchFragmentInContainer<RouteListFragment>()

        onView(withId(R.id.busRoutesList)).check(matches(isDisplayed()))

        for (i in busRouteList.indices) {
            val busRoute = busRouteList[i]
            onView(withRecyclerView(R.id.busRoutesList)
                    .atPositionOnView(i, R.id.title))
                    .check(matches(ViewMatchers.withText(busRoute.timetableId)))

        }
    }
}