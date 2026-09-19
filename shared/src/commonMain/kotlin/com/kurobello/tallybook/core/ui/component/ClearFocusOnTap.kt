package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@Composable
internal fun Modifier.clearFocusOnUnconsumedTap(): Modifier {
  val focusManager = LocalFocusManager.current
  return pointerInput(focusManager) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false)
      val up = waitForUpOrCancellation()
      if (up != null && !up.isConsumed) {
        focusManager.clearFocus(force = true)
      }
    }
  }
}
