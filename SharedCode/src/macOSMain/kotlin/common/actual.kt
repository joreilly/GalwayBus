package com.surrus.galwaybus.common

import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.surrus.galwaybus.db.MyDatabase

actual fun createDb(): MyDatabase? {
    val driver = NativeSqliteDriver(MyDatabase.Schema, "galwaybus.db")
    return MyDatabase(driver)
}

actual fun ktorScope(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }
