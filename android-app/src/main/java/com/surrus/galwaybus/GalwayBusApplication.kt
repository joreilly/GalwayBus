package com.surrus.galwaybus

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import com.surrus.galwaybus.common.appContext
import com.surrus.galwaybus.di.appModule


class GalwayBusApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        appContext = this
        startKoin {
            androidContext(this@GalwayBusApplication)
            modules(appModule)
        }
    }
}
