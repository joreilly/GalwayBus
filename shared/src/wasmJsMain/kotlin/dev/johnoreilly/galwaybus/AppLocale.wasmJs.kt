package dev.johnoreilly.galwaybus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

// Web resolves resources from the browser language; the in-app override is best-effort
// (the value is tracked but the browser locale can't be changed at runtime), so on web the
// app effectively follows the device/browser language.
internal actual object LocalAppLocale {
    private val localAppLocale = staticCompositionLocalOf<String?> { null }

    actual val current: String
        @Composable get() = localAppLocale.current ?: "en"

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> = localAppLocale.provides(value)
}

internal actual val localeChangeRequiresRestart: Boolean = false

// Web renders stops through the Compose map (which uses Locale.current), so this non-composable
// path isn't exercised by native markers on web; default to English.
internal actual fun currentLanguageTag(): String = "en"
