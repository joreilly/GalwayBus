package com.surrus.galwaybus.common.di

import com.russhwolf.settings.AppleSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsApi::class)
actual fun platformModule() = module {
    single<ObservableSettings> { AppleSettings(NSUserDefaults.standardUserDefaults) }
}
