package dev.johnoreilly.galwaybus

import android.app.Application
import co.touchlab.kermit.Logger
import org.koin.android.ext.koin.androidContext

import com.surrus.galwaybus.common.appContext
import com.surrus.galwaybus.common.di.initKoin
import dev.johnoreilly.galwaybus.di.appModule
import org.koin.android.ext.android.inject


class GalwayBusApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        appContext = this
        initKoin {
            androidContext(this@GalwayBusApplication)
            modules(appModule)
        }

        Logger.d { "GalwayBusApplication" }
    }
}
