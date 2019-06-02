package com.surrus.galwaybus.common.model


import kotlinx.serialization.*

@Serializable
data class BusRoute(
        @SerialName("timetable_id")
        val timetableId: String,
        @SerialName("long_name")
        val longName: String,
        @SerialName("short_name")
        val shortName: String)

