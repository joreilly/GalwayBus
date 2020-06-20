package com.surrus.galwaybus.common

import com.surrus.galwaybus.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


actual fun createDb(): MyDatabase? {
    return null
}

actual fun ktorScope(block: suspend () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) { block() }
}
