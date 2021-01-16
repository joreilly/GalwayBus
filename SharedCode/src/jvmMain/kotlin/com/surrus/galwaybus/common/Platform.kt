package com.surrus.galwaybus.common

import co.touchlab.kermit.CommonLogger
import co.touchlab.kermit.Logger
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.surrus.galwaybus.db.MyDatabase

actual fun createDb(): MyDatabase? {
    val driver = JdbcSqliteDriver("galwaybus.db")
    return MyDatabase(driver)
}

actual fun getLogger(): Logger = CommonLogger()
