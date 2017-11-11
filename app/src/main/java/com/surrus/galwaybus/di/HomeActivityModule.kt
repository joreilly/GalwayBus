package com.surrus.galwaybus.di

import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.ui.viewmodel.HomeViewModelFactory
import dagger.Module
import dagger.Provides

@Module
open class HomeActivityModule {

    @Provides
    fun provideHomeViewModelFactory(getBusRoutesUseCase: GetBusRoutesUseCase): HomeViewModelFactory {
        return HomeViewModelFactory(getBusRoutesUseCase)
    }

}
