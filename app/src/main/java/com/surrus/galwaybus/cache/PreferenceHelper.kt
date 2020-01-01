package com.surrus.galwaybus.cache


import android.content.Context
import android.content.SharedPreferences

/**
 * General Preferences Helper class, used for storing preference values using the Preference API
 */
open class PreferencesHelper constructor(context: Context) {

    companion object {
        private val PREF_GALWAY_BUS_PACKAGE_NAME = "com.surrus.galwaybus.preferences"

        private val PREF_KEY_LAST_CACHE = "last_cache"
    }

    private val galwayBusPref: SharedPreferences

    init {
        galwayBusPref = context.getSharedPreferences(PREF_GALWAY_BUS_PACKAGE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Store and retrieve the last time data was cached
     */
    var lastCacheTime: Long
        get() = galwayBusPref.getLong(PREF_KEY_LAST_CACHE, 0)
        set(lastCache) = galwayBusPref.edit().putLong(PREF_KEY_LAST_CACHE, lastCache).apply()

}
