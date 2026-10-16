package com.kurobello.tallybook.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

@Composable
internal actual fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
  remember(languageTag) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (languageTag == null) defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    else defaults.setObject(listOf(languageTag), APPLE_LANGUAGES_KEY)
  }
  // iOS resource lookup is not tied to a composition local, so only a new subtree re-resolves it.
  key(languageTag) { content() }
}
