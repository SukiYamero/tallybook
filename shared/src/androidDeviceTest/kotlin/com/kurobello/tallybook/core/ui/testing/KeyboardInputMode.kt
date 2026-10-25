package com.kurobello.tallybook.core.ui.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Runs [block] with the display out of touch mode, as it is for a hardware keyboard or D-pad user.
 * A real device starts in touch mode, where Compose clickables cannot take focus at all, so focus
 * and key-activation contracts are only observable after leaving it.
 */
internal fun ComposeTestRule.withKeyboardInputMode(block: () -> Unit) {
  val instrumentation = InstrumentationRegistry.getInstrumentation()
  instrumentation.setInTouchMode(false)
  try {
    waitForIdle()
    block()
  } finally {
    instrumentation.setInTouchMode(true)
  }
}
