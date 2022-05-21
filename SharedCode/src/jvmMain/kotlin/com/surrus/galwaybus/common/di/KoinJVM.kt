package com.surrus.galwaybus.common.di

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.JvmPreferencesSettings
import com.russhwolf.settings.ObservableSettings
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.surrus.galwaybus.db.MyDatabase
import org.koin.dsl.module
import java.util.prefs.Preferences

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual fun platformModule() = module {
    single<ObservableSettings> { JvmPreferencesSettings(Preferences.userRoot()) }
    single { createDb() }
}


fun createDb(): MyDatabase {
    val driver = JdbcSqliteDriver("galwaybus.db")
    return MyDatabase(driver)
}