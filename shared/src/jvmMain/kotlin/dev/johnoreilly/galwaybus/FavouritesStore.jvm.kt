package dev.johnoreilly.galwaybus

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("dev/johnoreilly/galwaybus")

internal actual fun readPref(key: String): String? = prefs.get(key, null)

internal actual fun writePref(key: String, value: String) {
    prefs.put(key, value)
}
