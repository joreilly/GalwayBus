package com.surrus.galwaybus.common

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NSLogLogger
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.surrus.galwaybus.db.MyDatabase

actual fun createDb(): MyDatabase? {
    val driver = NativeSqliteDriver(MyDatabase.Schema, "galwaybus.db")
    return MyDatabase(driver)
}

actual fun getLogger(): Logger = NSLogLogger()
