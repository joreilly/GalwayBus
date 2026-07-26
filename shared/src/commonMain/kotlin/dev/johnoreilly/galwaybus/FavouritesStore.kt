package dev.johnoreilly.galwaybus

/**
 * Minimal key/value persistence (favourites, last-viewed UI state).
 * Platform actuals back it with the native preferences store
 * (SharedPreferences / NSUserDefaults / java.util.prefs).
 */
internal expect fun readPref(key: String): String?
internal expect fun writePref(key: String, value: String)
