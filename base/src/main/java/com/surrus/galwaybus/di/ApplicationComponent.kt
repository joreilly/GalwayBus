package com.surrus.galwaybus.di

import android.app.Application
import com.surrus.galwaybus.GalwayBusApplication
import com.surrus.galwaybus.di.scopes.PerApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule


@PerApplication
@Component(modules = arrayOf(ActivityBindingModule::class, ApplicationModule::class, AndroidSupportInjectionModule::class))
interface ApplicationComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): ApplicationComponent
    }

    fun inject(app: GalwayBusApplication)

}
