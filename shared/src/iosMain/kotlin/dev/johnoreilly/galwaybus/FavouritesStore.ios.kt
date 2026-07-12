package dev.johnoreilly.galwaybus

import platform.Foundation.NSUserDefaults

private const val KEY_FAVOURITES = "favourite_stops"

internal actual fun readFavouritesJson(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(KEY_FAVOURITES)

internal actual fun writeFavouritesJson(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, KEY_FAVOURITES)
}
