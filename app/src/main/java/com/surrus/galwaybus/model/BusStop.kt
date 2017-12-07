package com.surrus.galwaybus.model

data class BusStop(val stopId: Int, val shortName: String, val longName: String? = null, val stopRef: String? = null,
    val irishShortName: String? = null, val irishLongName: String? = null, val latitude: Double = 0.toDouble(),
                   val longitude: Double = 0.toDouble(), val times: List<Departure>? = null)

