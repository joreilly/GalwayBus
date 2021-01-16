package com.surrus.galwaybus.common

import co.touchlab.kermit.CommonLogger
import co.touchlab.kermit.Logger
import com.surrus.galwaybus.db.MyDatabase


actual fun createDb(): MyDatabase? {
    return null
}

actual fun getLogger(): Logger = CommonLogger()
