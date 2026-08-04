package dev.johnoreilly.galwaybus

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

internal actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = LocalConfiguration.current.locales[0].toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        if (default == null) default = Locale.getDefault()
        val locale = value?.let { Locale.forLanguageTag(it) } ?: default!!
        Locale.setDefault(locale)
        val newConfiguration = Configuration(configuration).apply { setLocale(locale) }
        return LocalConfiguration.provides(newConfiguration)
    }
}

internal actual val localeChangeRequiresRestart: Boolean = false

// The in-app override calls Locale.setDefault, so getDefault() reflects the chosen language.
internal actual fun currentLanguageTag(): String = Locale.getDefault().toLanguageTag()
