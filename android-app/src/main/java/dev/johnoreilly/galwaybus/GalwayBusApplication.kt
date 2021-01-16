package dev.johnoreilly.galwaybus

import android.app.Application
import co.touchlab.kermit.Kermit
import org.koin.android.ext.koin.androidContext

import com.surrus.galwaybus.common.appContext
import com.surrus.galwaybus.common.di.initKoin
import dev.johnoreilly.galwaybus.di.appModule
import org.koin.android.ext.android.inject


class GalwayBusApplication : Application() {
    private val logger: Kermit by inject()

    override fun onCreate() {
        super.onCreate()

        appContext = this
        initKoin {
            androidContext(this@GalwayBusApplication)
            modules(appModule)
        }

        logger.d { "GalwayBusApplication" }
    }
}
