package com.kurobello.tallybook.core.ui.locale

import androidx.compose.runtime.Composable

/**
 * Applies an in-app language over the device language; `null` follows the device. Compose
 * Multiplatform has no common API for this yet, so each platform follows the documented workaround:
 * https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html
 */
@Composable
internal expect fun ProvideAppLocale(languageTag: String?, content: @Composable () -> Unit)
