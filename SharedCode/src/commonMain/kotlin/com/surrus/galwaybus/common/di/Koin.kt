package com.surrus.galwaybus.common.di

import co.touchlab.kermit.Kermit
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.getLogger
import com.surrus.galwaybus.common.remote.GalwayBusApi
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun initKoin(enableNetworkLogs: Boolean = false, appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule())
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
    single { Kermit(getLogger()) }
}

fun createJson() = Json { isLenient = true; ignoreUnknownKeys = true }

fun createHttpClient(json: Json) = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(json)
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}
