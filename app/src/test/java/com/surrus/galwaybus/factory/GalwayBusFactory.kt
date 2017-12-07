package com.surrus.galwaybus.factory

import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import java.time.LocalDateTime
import java.util.*

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


        fun makeDepartureList(count: Int) : List<Departure> {
            val departureList = mutableListOf<Departure>()
            for (i in 1..count) {
                val now = Date()
                val cal = Calendar.getInstance()
                cal.time = now
                cal.add(Calendar.HOUR, i);

                val departure = Departure(i.toString(), "departure display name " + i, cal.time)
                departureList.add(departure)
            }
            return departureList
        }

        fun makeBusStop() : BusStop {
            return BusStop(1, "short name")
        }

    }
}