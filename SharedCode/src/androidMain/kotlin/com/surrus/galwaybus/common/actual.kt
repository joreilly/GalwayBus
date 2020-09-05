package com.surrus.galwaybus.common

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


lateinit var appContext: Context

actual fun createDb(): MyDatabase? {
    val driver = AndroidSqliteDriver(MyDatabase.Schema, appContext, "galwaybus.db")
    return MyDatabase(driver)
}

