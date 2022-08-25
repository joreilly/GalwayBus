package com.surrus.galwaybus.common.di

import com.surrus.galwaybus.common.AppSettings
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.remote.CityBikesApi
import com.surrus.galwaybus.common.remote.GalwayBusApi
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.ExperimentalTime


expect fun platformModule(): Module

@ExperimentalTime
fun initKoin(enableNetworkLogs: Boolean = false, appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule(), platformModule())
    }

// called by iOS etc
@ExperimentalTime
fun initKoin() = initKoin() {}

@ExperimentalTime
fun commonModule() = module {
    single { createJson() }
    single { createHttpClient(get()) }
    single { GalwayBusRepository() }
    single { GalwayBusApi(get()) }
    single { CityBikesApi(get()) }
    single { AppSettings(get()) }
}

fun createJson() = Json { isLenient = true; ignoreUnknownKeys = true }

fun createHttpClient(json: Json) = HttpClient {
    install(ContentNegotiation) {
        json(json)
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}
