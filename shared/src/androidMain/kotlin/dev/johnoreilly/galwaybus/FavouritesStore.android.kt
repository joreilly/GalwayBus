package dev.johnoreilly.galwaybus

import android.content.Context

/**
 * Holder for the application [Context] used to back preference persistence with
 * SharedPreferences. Set this once from the Android entry point
 * (e.g. `GalwayBusPrefs.appContext = applicationContext`).
 *
 * When no context is available (such as in Compose previews) an in-memory
 * fallback is used so the app still functions, just without persistence.
 */
object GalwayBusPrefs {
    @Volatile
    var appContext: Context? = null

    internal val fallback = mutableMapOf<String, String>()
}

private const val PREFS_NAME = "galwaybus_prefs"

internal actual fun readPref(key: String): String? {
    val ctx = GalwayBusPrefs.appContext ?: return GalwayBusPrefs.fallback[key]
    return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(key, null)
}

internal actual fun writePref(key: String, value: String) {
    val ctx = GalwayBusPrefs.appContext
    if (ctx == null) {
        GalwayBusPrefs.fallback[key] = value
        return
    }
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(key, value)
        .apply()
}
