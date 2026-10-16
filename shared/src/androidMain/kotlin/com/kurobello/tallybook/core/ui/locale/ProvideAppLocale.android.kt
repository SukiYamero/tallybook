package com.kurobello.tallybook.core.ui.locale

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
internal actual fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit) {
  val base = LocalConfiguration.current
  // The system configuration keeps the device language even after Locale.setDefault overrides it.
  val locale =
      languageTag?.let(Locale::forLanguageTag) ?: Resources.getSystem().configuration.locales[0]
  // Resource lookup reads the process default; it must change before the children compose.
  Locale.setDefault(locale)
  val configuration = remember(base, locale) { Configuration(base).apply { setLocale(locale) } }
  CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
