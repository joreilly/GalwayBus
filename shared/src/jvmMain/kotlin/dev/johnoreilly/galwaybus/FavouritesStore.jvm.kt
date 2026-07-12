package dev.johnoreilly.galwaybus

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("dev/johnoreilly/galwaybus")
private const val KEY_FAVOURITES = "favourite_stops"

internal actual fun readFavouritesJson(): String? = prefs.get(KEY_FAVOURITES, null)

internal actual fun writeFavouritesJson(value: String) {
    prefs.put(KEY_FAVOURITES, value)
}
