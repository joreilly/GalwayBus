package com.surrus.galwaybus.common

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.surrus.galwaybus.common.di.initKoin
import com.surrus.galwaybus.common.remote.GalwayBusApi
import com.surrus.galwaybus.db.MyDatabase

actual fun createDb(): MyDatabase? {
    val driver = JdbcSqliteDriver("galwaybus.db")
    return MyDatabase(driver)
}

suspend fun main() {
    val koin = initKoin(enableNetworkLogs = true).koin


    val galwayBusApi = koin.get<GalwayBusApi>()

    val bustInfo = galwayBusApi.fetchBusListForRoute("401")

    println("hello, bustInfo = $bustInfo")
}