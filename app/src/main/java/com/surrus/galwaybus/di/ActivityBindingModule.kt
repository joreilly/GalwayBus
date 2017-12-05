package com.surrus.galwaybus.di

import com.surrus.galwaybus.di.scopes.PerActivity
import com.surrus.galwaybus.ui.BusStopListActivity
import com.surrus.galwaybus.ui.HomeActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBindingModule {

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(ActivityModule::class))
    abstract fun bindHomeActivity(): HomeActivity


    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(ActivityModule::class))
    abstract fun bindBusStopListActivity(): BusStopListActivity

}