package com.surrus.galwaybus.di

import android.content.Context
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModelFactory
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModelFactory
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import dagger.Module
import dagger.Provides
import com.google.firebase.analytics.FirebaseAnalytics



@Module
open class ActivityModule {

    @Provides
    fun provideBusRoutesViewModelFactory(getBusRoutesUseCase: GetBusRoutesUseCase): BusRoutesViewModelFactory {
        return BusRoutesViewModelFactory(getBusRoutesUseCase)
    }

    @Provides
    fun provideBusStopsViewModelFactory(getBusStopsUseCase: GetBusStopsUseCase): BusStopsViewModelFactory {
        return BusStopsViewModelFactory(getBusStopsUseCase)
    }

    @Provides
    fun provideNearestBusStopsViewModelFactory(getNearestBusStopsUseCase: GetNearestBusStopsUseCase): NearestBusStopsViewModelFactory {
        return NearestBusStopsViewModelFactory(getNearestBusStopsUseCase)
    }


    @Provides
    fun providesFirebaseAnalytics(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

}
