package com.surrus.galwaybus.common

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.surrus.galwaybus.db.MyDatabase

actual fun createDb(): MyDatabase? {
    val driver = JdbcSqliteDriver("test")
    return MyDatabase(driver)
}