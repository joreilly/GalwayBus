package dev.johnoreilly.galwaybus

import android.app.Application
import co.touchlab.kermit.Logger
import org.koin.android.ext.koin.androidContext

import com.surrus.galwaybus.common.di.initKoin
import dev.johnoreilly.galwaybus.di.appModule
import kotlin.time.ExperimentalTime


class GalwayBusApplication : Application() {

    @OptIn(ExperimentalTime::class)
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@GalwayBusApplication)
            modules(appModule)
        }

        Logger.d { "GalwayBusApplication" }
    }
}
