package dev.johnoreilly.galwaybus.di

import co.touchlab.kermit.Kermit
import co.touchlab.kermit.LogcatLogger
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module


val galwayBusAppModule = module {

    viewModel { GalwayBusViewModel(get(), get(),get()) }

    single { Kermit(LogcatLogger()) }
    single { com.surrus.galwaybus.common.GalwayBusRepository() }
}


// Gather all app modules
val appModule = listOf(galwayBusAppModule)