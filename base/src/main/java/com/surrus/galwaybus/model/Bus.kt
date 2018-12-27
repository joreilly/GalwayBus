package com.surrus.galwaybus.model


data class DepartureMetadata(val destination: String, val delay: Int)

data class Bus(val vehicle_id: String, val latitude: Double, val longitude: Double, val direction: Int, val departure_metadata: DepartureMetadata?)