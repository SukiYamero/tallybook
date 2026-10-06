package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookFilterTagTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun wholeTagRemovesOnceWithCallerDescriptionAndNoSelectedSemanticsOrPressIndication() {
    var removals = 0
    val indication = RecordingFilterTagIndication()
    composeRule.setContent {
      TallybookTheme {
        CompositionLocalProvider(LocalIndication provides indication) {
          TallybookFilterTag("Last 7 days", "Remove date filter", { removals++ })
        }
      }
    }
    val tag = composeRule.onNodeWithContentDescription("Remove date filter")
    tag.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    tag.performTouchInput { down(center) }
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.runOnIdle {
      assertEquals(0, removals)
      assertEquals(0, indication.createdNodes)
    }
    tag.performTouchInput { up() }
    composeRule.runOnIdle { assertEquals(1, removals) }
  }

  @Test
  fun disabledTagDoesNotRemoveItsFilter() {
    var removals = 0
    composeRule.setContent {
      TallybookTheme { TallybookFilterTag("Food", "Remove Food", { removals++ }, enabled = false) }
    }
    composeRule.onNodeWithContentDescription("Remove Food").assertIsNotEnabled().performTouchInput {
      click()
    }
    composeRule.runOnIdle { assertEquals(0, removals) }
  }

  @Test
  fun compactTintedTagCentersCompleteLabelWithinTheFullTouchTarget() {
    val tint = Color(0xFF225566)
    composeRule.setContent {
      TallybookTheme {
        TallybookFilterTag(
            "Food",
            "Remove Food",
            {},
            colors = TallybookButtonColors(tint, Color.White, tint, Color.Gray),
        )
      }
    }
    val tag = composeRule.onNodeWithContentDescription("Remove Food")
    val target = tag.getUnclippedBoundsInRoot()
    val label =
        composeRule.onNodeWithText("Food", useUnmergedTree = true).getUnclippedBoundsInRoot()
    assertTrue(target.bottom - target.top >= 48.dp)
    val targetCenter = (target.top + target.bottom) / 2
    val labelCenter = (label.top + label.bottom) / 2
    assertTrue(abs((targetCenter - labelCenter).value) <= 1f)
    val image = tag.captureToImage().asAndroidBitmap()
    val paddingPixelX = with(composeRule.density) { 4.dp.roundToPx() }
    assertEquals(tint.toArgb(), image.getPixel(paddingPixelX, image.height / 2))
  }
}

private class RecordingFilterTagIndication : IndicationNodeFactory {
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
