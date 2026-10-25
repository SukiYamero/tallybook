package com.kurobello.tallybook.core.ui.testing

import android.view.WindowInsets
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

private const val KEYBOARD_TIMEOUT_MILLIS = 5_000L

/**
 * Reads the soft keyboard from the platform windows rather than from Compose insets: a test reading
 * `WindowInsets.ime` in the test activity sees 0 while the keyboard is attached to a sheet's or
 * dialog's own window.
 */
internal fun isSoftKeyboardVisible(): Boolean {
  var visible = false
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
    visible =
        WindowInspector.getGlobalWindowViews().any { view ->
          view.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        }
  }
  return visible
}

internal fun ComposeTestRule.awaitSoftKeyboard(visible: Boolean) {
  waitUntil(KEYBOARD_TIMEOUT_MILLIS) { isSoftKeyboardVisible() == visible }
  waitForIdle()
}

/**
 * Closes the real keyboard a focused field opened, the way the user dismisses it. It waits for the
 * keyboard first, because hiding before the show request lands would let it open afterwards.
 */
internal fun ComposeTestRule.closeSoftKeyboard() {
  awaitSoftKeyboard(visible = true)
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
    WindowInspector.getGlobalWindowViews()
        .filter { view -> view.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true }
        .forEach { view -> view.windowInsetsController?.hide(WindowInsets.Type.ime()) }
  }
  awaitSoftKeyboard(visible = false)
}
