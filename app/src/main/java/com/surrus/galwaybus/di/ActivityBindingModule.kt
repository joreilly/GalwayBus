package com.surrus.galwaybus.di

import com.surrus.galwaybus.di.scopes.PerActivity
import com.surrus.galwaybus.ui.BusStopListActivity
import com.surrus.galwaybus.ui.HomeActivity
import com.surrus.galwaybus.ui.NearbyFragment
import com.surrus.galwaybus.ui.RoutesFragment
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

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(ActivityModule::class))
    abstract fun bindNearbyFragment(): NearbyFragment

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(ActivityModule::class))
    abstract fun bindRoutesFragment(): RoutesFragment

}