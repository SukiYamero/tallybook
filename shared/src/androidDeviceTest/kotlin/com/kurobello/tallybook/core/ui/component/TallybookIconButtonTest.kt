package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookIconButtonTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun iconButtonExposesDescriptionAndInvokesCallback() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookIconButton(
            onClick = { clicks += 1 },
            icon = Lucide.X,
            contentDescription = "Close sample",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Close sample").assertHasClickAction().performClick()
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }

  @Test
  fun disabledIconButtonDoesNotInvokeCallback() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookIconButton(
            onClick = { clicks += 1 },
            icon = Lucide.X,
            contentDescription = "Close sample",
            enabled = false,
        )
      }
    }

    composeRule.onNodeWithContentDescription("Close sample").assertIsNotEnabled().performClick()
    composeRule.runOnIdle { assertEquals(0, clicks) }
  }

  @Test
  fun iconButtonRootMeetsMinimumTouchTarget() {
    composeRule.setContent {
      TallybookTheme {
        TallybookIconButton(
            onClick = {},
            icon = Lucide.X,
            contentDescription = "Close sample",
        )
      }
    }

    val bounds =
        composeRule.onNodeWithContentDescription("Close sample").fetchSemanticsNode().boundsInRoot
    val minimumTouchTargetPx =
        with(composeRule.density) { TallybookTheme.dimensions.minimumTouchTarget.toPx() }

    assertTrue(
        bounds.width >= minimumTouchTargetPx,
        "Expected icon-button width ${bounds.width}px to meet $minimumTouchTargetPx px",
    )
    assertTrue(
        bounds.height >= minimumTouchTargetPx,
        "Expected icon-button height ${bounds.height}px to meet $minimumTouchTargetPx px",
    )
  }
}
