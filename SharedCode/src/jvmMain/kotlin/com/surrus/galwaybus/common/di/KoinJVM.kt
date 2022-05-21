package com.surrus.galwaybus.common.di

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.JvmPreferencesSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import java.util.prefs.Preferences

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual fun platformModule() = module {
    single<ObservableSettings> { JvmPreferencesSettings(Preferences.userRoot()) }
}
