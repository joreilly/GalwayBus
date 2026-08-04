package dev.johnoreilly.galwaybus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

internal actual object LocalAppLocale {
    private var default: Locale? = null
    private val localAppLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) default = Locale.getDefault()
        val locale = value?.let { Locale.forLanguageTag(it) } ?: default!!
        // Compose Resources on desktop resolves strings from java.util.Locale.getDefault().
        Locale.setDefault(locale)
        return localAppLocale.provides(locale.toLanguageTag())
    }
}

internal actual val localeChangeRequiresRestart: Boolean = false
