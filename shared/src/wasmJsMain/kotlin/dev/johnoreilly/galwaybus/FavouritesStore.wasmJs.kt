package dev.johnoreilly.galwaybus

import kotlinx.browser.localStorage

internal actual fun readPref(key: String): String? = localStorage.getItem(key)

internal actual fun writePref(key: String, value: String) {
    localStorage.setItem(key, value)
}
