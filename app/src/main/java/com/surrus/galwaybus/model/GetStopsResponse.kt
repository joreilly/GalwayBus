package com.surrus.galwaybus.model

data class GetStopsResponse(val route: BusRoute, val stops: List<List<BusStop>>)
