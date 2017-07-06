package com.surrus.galwaybus.model

import com.google.gson.annotations.SerializedName

data class BusRoute(@SerializedName("timetable_id") val timetableId: String, @SerializedName("long_name") val longName: String, @SerializedName("short_name") val shortName: String)

