package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookChoicePillTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun pressDoesNotCreateThemeIndicationAndReleaseEmitsOneCallback() {
    val indication = RecordingPillIndication()
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        CompositionLocalProvider(LocalIndication provides indication) {
          TallybookChoicePill("Last 7 days", false, { clicks++ })
        }
      }
    }
    val pill = composeRule.onNodeWithText("Last 7 days")
    pill.performTouchInput { down(center) }
    composeRule.mainClock.advanceTimeByFrame()
    pill.assertIsNotSelected()
    composeRule.runOnIdle {
      assertEquals(0, indication.createdNodes)
      assertEquals(0, clicks)
    }
    pill.performTouchInput { up() }
    composeRule.runOnIdle {
      assertEquals(0, indication.createdNodes)
      assertEquals(1, clicks)
    }
  }

  @Test
  fun focusedPillKeepsKeyboardActivationWithTheCallerFocusModifier() {
    val requester = FocusRequester()
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookChoicePill("Last 7 days", false, { clicks++ }, Modifier.focusRequester(requester))
      }
    }
    composeRule.runOnIdle { requester.requestFocus() }
    composeRule.onNodeWithText("Last 7 days").assertIsFocused().performKeyInput {
      pressKey(Key.Enter)
    }
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }

  @Test
  fun disabledPillRetainsSelectionAndDoesNotEmitACallback() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { TallybookChoicePill("Last 7 days", true, { clicks++ }, enabled = false) }
    }
    composeRule
        .onNodeWithText("Last 7 days")
        .assertIsSelected()
        .assertIsNotEnabled()
        .performTouchInput { click() }
    composeRule.runOnIdle { assertEquals(0, clicks) }
  }

  @Test
  fun compactPresetCentersItsLabelWithinAFullSizeSelectedTapTarget() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { TallybookChoicePill("Last 7 days", true, { clicks++ }) }
    }
    val target = composeRule.onNodeWithText("Last 7 days").assertIsSelected()
    val targetBounds = target.getUnclippedBoundsInRoot()
    val textBounds =
        composeRule.onNodeWithText("Last 7 days", useUnmergedTree = true).getUnclippedBoundsInRoot()
    assertTrue(targetBounds.bottom - targetBounds.top >= 48.dp)
    val targetCenter = (targetBounds.top + targetBounds.bottom) / 2
    val textCenter = (textBounds.top + textBounds.bottom) / 2
    assertTrue(abs((targetCenter - textCenter).value) <= 1f)
    target.performClick()
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }
}

private class RecordingPillIndication : IndicationNodeFactory {
  var createdNodes = 0
    private set

  override fun create(interactionSource: InteractionSource): DelegatableNode {
    createdNodes++
    return object : Modifier.Node(), DrawModifierNode {
      override fun ContentDrawScope.draw() = drawContent()
    }
  }

  override fun equals(other: Any?): Boolean = this === other

  override fun hashCode(): Int = super.hashCode()
}
