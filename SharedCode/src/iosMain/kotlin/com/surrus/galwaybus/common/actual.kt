package com.surrus.galwaybus.common

import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

actual fun createDb(): MyDatabase? {
    val driver = NativeSqliteDriver(MyDatabase.Schema, "galwaybus.db")
    return MyDatabase(driver)
}

