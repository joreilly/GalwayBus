package dev.johnoreilly.galwaybus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

/**
 * In-app UI language override, independent of the device locale. A language tag
 * (e.g. "en", "ga") forces that language; `null` follows the system locale.
 *
 * Compose Resources resolves `stringResource`/`pluralStringResource` from the platform
 * locale, so each platform's actual re-points that locale and we re-key the content to
 * force strings to re-resolve. On iOS the change only takes full effect after a restart
 * (see [localeChangeRequiresRestart]).
 */
internal expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/** True where an in-app language change needs an app restart to apply (iOS). */
internal expect val localeChangeRequiresRestart: Boolean

/** The persisted language pref key; `null`/absent means "follow the device locale". */
internal const val LANGUAGE_PREF_KEY = "app_language"

/**
 * Wraps [content] so it renders in [languageTag] (or the device locale when null).
 * The [key] forces the subtree to recompose when the language changes, so every
 * `stringResource` re-resolves against the newly-applied locale.
 */
@Composable
internal fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides languageTag) {
        if (localeChangeRequiresRestart) {
            // iOS: the change applies on next launch, so re-keying here would reset the UI
            // (including the "restart to apply" prompt) without actually switching language.
            content()
        } else {
            // Re-key so every stringResource re-resolves against the newly-applied locale.
            key(LocalAppLocale.current) {
                content()
            }
        }
    }
}
