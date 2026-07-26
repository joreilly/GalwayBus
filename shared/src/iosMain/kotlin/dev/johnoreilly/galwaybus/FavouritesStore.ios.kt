package dev.johnoreilly.galwaybus

import platform.Foundation.NSUserDefaults

internal actual fun readPref(key: String): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(key)

internal actual fun writePref(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, key)
}
