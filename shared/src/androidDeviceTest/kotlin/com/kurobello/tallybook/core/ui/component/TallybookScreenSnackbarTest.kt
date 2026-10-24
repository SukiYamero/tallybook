package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kurobello.tallybook.core.ui.theme.TallybookMotion
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookScreenSnackbarTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun timerOnlyRunsWhileTheLifecycleIsResumed() {
    val owner = StartedLifecycleOwner()
    val dismissed = mutableListOf<Long>()
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      CompositionLocalProvider(LocalLifecycleOwner provides owner) {
        TallybookTheme {
          TallybookScreenSnackbar(
              message =
                  TallybookSnackbarMessage(
                      token = 1,
                      revision = 0,
                      message = "Category saved",
                      duration = TallybookSnackbarDuration.Short,
                  ),
              isActive = true,
              onDismiss = { dismissed += it },
          )
        }
      }
    }
    composeRule.mainClock.advanceTimeBy(TallybookMotion.snackbarShortMillis * 3L)
    composeRule.runOnIdle { assertEquals(emptyList(), dismissed) }

    composeRule.runOnIdle { owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    composeRule.mainClock.advanceTimeBy(TallybookMotion.snackbarShortMillis + FRAME_SLACK_MILLIS)
    composeRule.runOnIdle { assertEquals(listOf(1L), dismissed) }
  }
}

private const val FRAME_SLACK_MILLIS = 100L

private class StartedLifecycleOwner : LifecycleOwner {
  override val lifecycle = LifecycleRegistry.createUnsafe(this)

  init {
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
  }
}
