package com.surrus.galwaybus.cache.db

object GalwayBusDatabaseConstants {

    const val BUS_ROUTES_TABLE_NAME = "bus_routes"
    const val QUERY_BUS_ROUTES = "SELECT * FROM" + " " + BUS_ROUTES_TABLE_NAME
    const val DELETE_ALL_BUS_ROUTES = "DELETE FROM" + " " + BUS_ROUTES_TABLE_NAME
}