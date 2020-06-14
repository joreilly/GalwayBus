package com.surrus.galwaybus.di.koin

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.BusInfoViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module


val galwayBusAppModule = module {

    viewModel { NearestBusStopsViewModel(get()) }
    viewModel { BusRoutesViewModel(get()) }
    viewModel { BusStopsViewModel(get()) }
    viewModel { BusInfoViewModel(get()) }

    single { com.surrus.galwaybus.common.GalwayBusRepository() }

    single { createFirebaseAnalytics(get()) }
}





internal fun createFirebaseAnalytics(context: Context): FirebaseAnalytics {
    return FirebaseAnalytics.getInstance(context)
}


// Gather all app modules
val appModule = listOf(galwayBusAppModule)