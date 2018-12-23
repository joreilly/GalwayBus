package com.surrus.galwaybus.common.model


import kotlinx.serialization.*

@Serializable
data class BusRoute(val timetableId: Long, val longName: String, val shortName: String)

