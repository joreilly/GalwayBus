package com.surrus.galwaybus.common.di

import android.content.Context
import com.russhwolf.settings.AndroidSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.surrus.galwaybus.db.MyDatabase
import org.koin.dsl.module

@OptIn(ExperimentalSettingsApi::class)
actual fun platformModule() = module {
    single { createObservableSettings(get()) }
    single { createDb(get()) }
}


@OptIn(ExperimentalSettingsApi::class)
private fun createObservableSettings(context: Context): ObservableSettings {
    return AndroidSettings(context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE))
}

fun createDb(context: Context): MyDatabase {
    val driver = AndroidSqliteDriver(MyDatabase.Schema, context, "galwaybus.db")
    return MyDatabase(driver)
}