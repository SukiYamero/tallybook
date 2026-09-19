package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookButtonTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun textButtonUsesTheSharedActionContract() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookTextButton(onClick = { clicks += 1 }) { TallybookText(text = "Text action") }
      }
    }

    composeRule.onNodeWithText("Text action").assertHasClickAction().performClick()
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }
}
