package com.surrus.galwaybus.di

import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModelFactory
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModelFactory
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModelFactory
import dagger.Module
import dagger.Provides

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

}
