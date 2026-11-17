package com.kurobello.tallybook.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** Asks the system for what dictation needs: the microphone, plus speech recognition on iOS. */
@Stable
internal interface DictationPermissionLauncher {
  fun request()

  fun openSettings()
}

@Composable
internal expect fun rememberDictationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit
): DictationPermissionLauncher
