package com.surrus.galwaybus.common

import com.surrus.galwaybus.common.di.initKoin
import com.surrus.galwaybus.common.remote.GalwayBusApi


suspend fun main() {
    val koin = initKoin(enableNetworkLogs = true).koin


    val galwayBusApi = koin.get<GalwayBusApi>()
    val bustInfo = galwayBusApi.fetchBusListForRoute("401")

    println("hello, bustInfo = $bustInfo")
}