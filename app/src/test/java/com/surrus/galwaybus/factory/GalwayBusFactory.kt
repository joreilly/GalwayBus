package com.surrus.galwaybus.factory

import com.surrus.galwaybus.model.BusRoute

class GalwayBusFactory {
    companion object Factory {

        fun makeBusRouteList(count: Int) : List<BusRoute> {
            val busRouteList = mutableListOf<BusRoute>()
            for (i in 1..count) {
                val busRoute = BusRoute(i.toString(), "lnog name 40" + i, "short name 40" + i)
                busRouteList.add(busRoute)
            }
            return busRouteList
        }

    }
}