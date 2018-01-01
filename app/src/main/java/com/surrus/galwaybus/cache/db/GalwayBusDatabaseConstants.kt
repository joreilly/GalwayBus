package com.surrus.galwaybus.cache.db

object GalwayBusDatabaseConstants {

    const val BUS_ROUTES_TABLE_NAME = "bus_routes"
    const val BUS_STOPS_TABLE_NAME = "bus_stops"

    const val QUERY_BUS_ROUTES = "SELECT * FROM" + " " + BUS_ROUTES_TABLE_NAME
    const val DELETE_ALL_BUS_ROUTES = "DELETE FROM" + " " + BUS_ROUTES_TABLE_NAME


    const val QUERY_BUS_STOPS_COUNNT = "SELECT COUNT(*) FROM " + BUS_STOPS_TABLE_NAME
    const val QUERY_BUS_STOPS = "SELECT * FROM" + " " + BUS_STOPS_TABLE_NAME
    const val QUERY_BUS_STOPS_BY_NAME = "SELECT * FROM $BUS_STOPS_TABLE_NAME WHERE longName LIKE :longNameText"
    const val DELETE_ALL_BUS_STOPS = "DELETE FROM" + " " + BUS_STOPS_TABLE_NAME

}