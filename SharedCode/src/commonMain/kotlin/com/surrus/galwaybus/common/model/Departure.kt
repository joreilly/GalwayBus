package com.surrus.galwaybus.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Departure(
        @SerialName("timetable_id")
        val timetableId: String,
        @SerialName("display_name")
        val displayName: String,
        @SerialName("depart_timestamp")
        val departTimestamp: String?)

