package com.surrus.galwaybus.model


data class GetDeparturesResponse(val stop: BusStop, val departureTimes: List<Departure>)

