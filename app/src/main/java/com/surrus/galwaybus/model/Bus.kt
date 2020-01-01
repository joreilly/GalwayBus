package com.surrus.galwaybus.model

import java.util.Date

data class DepartureMetadata(val destination: String, val delay: Int)

data class Bus(val vehicle_id: String, val modified_timestamp: Date, val latitude: Double,
               val longitude: Double, val direction: Int, val departure_metadata: DepartureMetadata?,
               val route: Map<String, String>?)