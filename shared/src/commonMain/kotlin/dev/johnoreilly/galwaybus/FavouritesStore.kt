package dev.johnoreilly.galwaybus

/**
 * Minimal key/value persistence for the favourites feature.
 * Stores a single JSON string; platform actuals back it with the native
 * preferences store (SharedPreferences / NSUserDefaults / java.util.prefs).
 */
internal expect fun readFavouritesJson(): String?
internal expect fun writeFavouritesJson(value: String)
