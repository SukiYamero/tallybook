package com.kurobello.tallybook.core.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberDictationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit
): DictationPermissionLauncher {
  val context = LocalContext.current
  val currentOnResult by rememberUpdatedState(onResult)
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnResult(it)
      }
  return remember(launcher, context) {
    object : DictationPermissionLauncher {
      override fun request() = launcher.launch(Manifest.permission.RECORD_AUDIO)

      override fun openSettings() {
        context.startActivity(
            Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
      }
    }
  }
}
