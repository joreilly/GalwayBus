package com.surrus.galwaybus.common

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalSettingsApi::class,ExperimentalCoroutinesApi::class)
class AppSettings(val settings: ObservableSettings) {

    val favorites: Flow<Set<String>> =
        settings.getStringOrNullFlow(FAVORITES_SETTING).map { getFavoritesFromString(it) }

    fun toggleFavorite(stopRef: String) {
        val currentFavoritesString = settings.getStringOrNull(FAVORITES_SETTING)
        val currentFavoritesSet = getFavoritesFromString(currentFavoritesString)

        val newFavoritesString = if (currentFavoritesSet.contains(stopRef)) {
            currentFavoritesSet.minus(stopRef)
        } else {
            currentFavoritesSet.plus(stopRef)
        }
        settings.putString(FAVORITES_SETTING, newFavoritesString.joinToString(separator = ","))
    }

    private fun getFavoritesFromString(settingsString: String?) =
        settingsString?.split(",")?.toSet() ?: emptySet()

    companion object {
        const val FAVORITES_SETTING = "favorites"
    }
}