package dev.johnoreilly.galwaybus

import android.content.Context

/**
 * Holder for the application [Context] used to back favourites persistence with
 * SharedPreferences. Set this once from the Android entry point
 * (e.g. `GalwayBusPrefs.appContext = applicationContext`).
 *
 * When no context is available (such as in Compose previews) an in-memory
 * fallback is used so the app still functions, just without persistence.
 */
object GalwayBusPrefs {
    @Volatile
    var appContext: Context? = null

    @Volatile
    internal var fallback: String? = null
}

private const val PREFS_NAME = "galwaybus_prefs"
private const val KEY_FAVOURITES = "favourite_stops"

internal actual fun readFavouritesJson(): String? {
    val ctx = GalwayBusPrefs.appContext ?: return GalwayBusPrefs.fallback
    return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_FAVOURITES, null)
}

internal actual fun writeFavouritesJson(value: String) {
    val ctx = GalwayBusPrefs.appContext
    if (ctx == null) {
        GalwayBusPrefs.fallback = value
        return
    }
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_FAVOURITES, value)
        .apply()
}
