package dev.johnoreilly.galwaybus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

internal actual object LocalAppLocale {
    private const val APPLE_LANGUAGES = "AppleLanguages"
    private val localAppLocale = staticCompositionLocalOf { systemLanguage() }

    private fun systemLanguage(): String = NSLocale.currentLocale.languageCode

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        // Persist for the next launch — iOS resolves bundle/locale strings at startup, so the
        // change only fully applies after a restart (see localeChangeRequiresRestart).
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(APPLE_LANGUAGES)
        } else {
            defaults.setObject(listOf(value), APPLE_LANGUAGES)
        }
        return localAppLocale.provides(value ?: systemLanguage())
    }
}

internal actual val localeChangeRequiresRestart: Boolean = true

// NSLocale reflects the persisted language after the app restart the switcher prompts for.
internal actual fun currentLanguageTag(): String = NSLocale.currentLocale.languageCode
