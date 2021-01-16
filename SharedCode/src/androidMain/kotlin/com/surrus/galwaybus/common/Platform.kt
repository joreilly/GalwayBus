package com.surrus.galwaybus.common

import android.content.Context
import co.touchlab.kermit.LogcatLogger
import co.touchlab.kermit.Logger
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.surrus.galwaybus.db.MyDatabase


lateinit var appContext: Context

actual fun createDb(): MyDatabase? {
    val driver = AndroidSqliteDriver(MyDatabase.Schema, appContext, "galwaybus.db")
    return MyDatabase(driver)
}

actual fun getLogger(): Logger = LogcatLogger()
