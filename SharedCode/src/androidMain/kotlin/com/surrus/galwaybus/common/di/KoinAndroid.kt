package com.surrus.galwaybus.common.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import com.surrus.galwaybus.db.MyDatabase
import org.koin.dsl.module

actual fun platformModule() = module {
    single { createObservableSettings(get()) }
    single { createDb(get()) }
}


private fun createObservableSettings(context: Context): ObservableSettings {
    return SharedPreferencesSettings(context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE))
}

fun createDb(context: Context): MyDatabase {
    val driver = AndroidSqliteDriver(MyDatabase.Schema, context, "galwaybus.db")
    return MyDatabase(driver)
}