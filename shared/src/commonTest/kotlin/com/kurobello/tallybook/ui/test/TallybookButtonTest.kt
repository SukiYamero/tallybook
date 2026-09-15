package com.kurobello.tallybook.ui.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class TallybookButtonTest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun enabledButtonInvokesItsAction() = runComposeUiTest {
    var clicks = 0
    setContent {
      TallybookTheme { TallybookButton(onClick = { clicks += 1 }) { TallybookText("Continue") } }
    }

    onNodeWithText("Continue").performClick()

    runOnIdle { assertEquals(1, clicks) }
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun disabledButtonDoesNotInvokeItsAction() = runComposeUiTest {
    var clicks = 0
    setContent {
      TallybookTheme {
        TallybookButton(onClick = { clicks += 1 }, enabled = false) { TallybookText("Continue") }
      }
    }

    onNodeWithText("Continue").assertIsNotEnabled().performClick()

    runOnIdle { assertEquals(0, clicks) }
  }
}
