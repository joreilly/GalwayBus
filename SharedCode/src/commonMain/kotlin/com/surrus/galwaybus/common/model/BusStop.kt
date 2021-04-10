package com.surrus.galwaybus.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BusStop(val stop_id: String,
                   @SerialName("short_name") val shortName: String,
                   @SerialName("long_name") val longName: String = "",
                   @SerialName("stop_ref") val stopRef: String = "",
                   val latitude: Double? = 0.0,
                   val longitude: Double? = 0.0,
                   var times: List<Departure> = emptyList(),
                   val routes: List<String> = emptyList(),
                   val distance: Double? = 0.0,
                   val galway: Boolean = false)


