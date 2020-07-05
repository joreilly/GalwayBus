package com.surrus.galwaybus.di

import com.surrus.galwaybus.ui.viewmodel.*
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module


val galwayBusAppModule = module {

    viewModel { GalwayBusViewModel(get()) }

    single { com.surrus.galwaybus.common.GalwayBusRepository() }
}


// Gather all app modules
val appModule = listOf(galwayBusAppModule)