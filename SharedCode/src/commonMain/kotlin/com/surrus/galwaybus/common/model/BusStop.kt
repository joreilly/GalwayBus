package com.surrus.galwaybus.common.model

import kotlinx.serialization.Serializable


@Serializable
data class BusStop(val stop_id: Int, val short_name: String, val irish_short_name: String,
        val long_name: String = "", val irish_long_name: String = "", val stop_ref: String = "",
        val latitude: Double = 0.0, val longitude: Double = 0.0, var times: List<Departure> = emptyList(),
        val routes: List<String> = emptyList())


