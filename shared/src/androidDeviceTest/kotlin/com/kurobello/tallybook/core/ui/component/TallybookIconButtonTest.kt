package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
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

  @Test
  fun disabledIconButtonKeepsItsParentBackground() {
    var parentColor = Color.Unspecified
    composeRule.setContent {
      TallybookTheme(darkTheme = true) {
        parentColor = TallybookTheme.colors.surface.deep
        Box(modifier = Modifier.fillMaxSize().background(parentColor)) {
          TallybookIconButton(
              onClick = {},
              icon = Lucide.X,
              contentDescription = "Disabled close sample",
              enabled = false,
          )
        }
      }
    }

    val bounds =
        composeRule
            .onNodeWithContentDescription("Disabled close sample")
            .fetchSemanticsNode()
            .boundsInRoot
    val image = composeRule.onRoot().captureToImage().toPixelMap()
    val backgroundPixel =
        image[(bounds.left + bounds.width * 0.2f).toInt(), bounds.center.y.toInt()]

    assertColorClose(parentColor, backgroundPixel)
  }

  private fun assertColorClose(expected: Color, actual: Color) {
    val tolerance = 0.01f
    assertTrue(kotlin.math.abs(expected.red - actual.red) < tolerance)
    assertTrue(kotlin.math.abs(expected.green - actual.green) < tolerance)
    assertTrue(kotlin.math.abs(expected.blue - actual.blue) < tolerance)
  }
}
