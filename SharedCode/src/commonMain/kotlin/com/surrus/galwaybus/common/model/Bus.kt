package com.surrus.galwaybus.common.model

import kotlinx.serialization.Serializable


@Serializable
data class DepartureMetadata(val destination: String, val delay: Int)

@Serializable
data class Bus(val vehicle_id: String, val modified_timestamp: String, val latitude: Double,
               val longitude: Double,
               val direction: Int = 0,
               val departure_metadata: DepartureMetadata? = null,
               val route: Map<String, String>? = null)