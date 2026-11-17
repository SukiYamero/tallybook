package com.kurobello.tallybook.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.AVFAudio.AVAudioApplication
import platform.Foundation.NSURL
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Dictation needs two grants on iOS: speech recognition, then the microphone. */
@Composable
internal actual fun rememberDictationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit
): DictationPermissionLauncher {
  val currentOnResult by rememberUpdatedState(onResult)
  return remember {
    object : DictationPermissionLauncher {
      override fun request() {
        SFSpeechRecognizer.requestAuthorization { status ->
          if (
              status !=
                  SFSpeechRecognizerAuthorizationStatus
                      .SFSpeechRecognizerAuthorizationStatusAuthorized
          ) {
            onMain { currentOnResult(false) }
          } else {
            AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted ->
              onMain { currentOnResult(granted) }
            }
          }
        }
      }

      override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { _ -> }
      }
    }
  }
}

// Both system prompts answer on a background queue; Compose state belongs to the main thread.
private fun onMain(block: () -> Unit) {
  dispatch_async(dispatch_get_main_queue(), block)
}
