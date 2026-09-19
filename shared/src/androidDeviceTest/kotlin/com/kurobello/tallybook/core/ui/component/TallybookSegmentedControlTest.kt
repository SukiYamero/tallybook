package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookSegmentedControlTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun selectedValuePublishesExactlyOneSelectedRadioSemantic() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSegmentedControl(
            options = options,
            selected = TransactionType.Expense,
            onSelect = {},
        )
      }
    }

    composeRule
        .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        .assertCountEquals(1)
    composeRule
        .onNodeWithText("Expense")
        .assertIsSelected()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
  }

  @Test
  fun selectingIncomeReturnsTypedIncomeValue() {
    var selectedValue: TransactionType? = null
    composeRule.setContent {
      TallybookTheme {
        TallybookSegmentedControl(
            options = options,
            selected = TransactionType.Expense,
            onSelect = { selectedValue = it },
        )
      }
    }

    composeRule.onNodeWithText("Income").performClick()

    composeRule.runOnIdle { assertEquals(TransactionType.Income, selectedValue) }
  }

  @Test
  fun disabledSegmentsDoNotInvokeSelectionCallback() {
    var selections = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookSegmentedControl(
            options = options,
            selected = TransactionType.Expense,
            onSelect = { selections += 1 },
            enabled = false,
        )
      }
    }

    segmentNode("Income")
        .assertIsNotEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        .performTouchInput { click(center) }

    composeRule.runOnIdle { assertEquals(0, selections) }
  }

  @Test
  fun emptyOptionsThrowActionableIllegalArgumentException() {
    val exception =
        assertFailsWith<IllegalArgumentException> {
          composeRule.setContent {
            TallybookTheme {
              TallybookSegmentedControl(
                  options = emptyList(),
                  selected = TransactionType.Expense,
                  onSelect = {},
              )
            }
          }
        }

    assertTrue(exception.message?.contains("options") == true)
  }

  @Test
  fun everySegmentMeetsMinimumTouchTarget() {
    val compactOptions =
        listOf(
            TallybookSegmentOption(TransactionType.Expense, "E"),
            TallybookSegmentOption(TransactionType.Income, "I"),
        )
    composeRule.setContent {
      TallybookTheme {
        TallybookSegmentedControl(
            options = compactOptions,
            selected = TransactionType.Expense,
            onSelect = {},
            enabled = false,
        )
      }
    }

    val minimumTouchTargetPx =
        with(composeRule.density) { TallybookTheme.dimensions.minimumTouchTarget.toPx() }
    compactOptions.forEach { option ->
      val bounds = segmentNode(option.label).fetchSemanticsNode().boundsInRoot
      assertTrue(
          bounds.width >= minimumTouchTargetPx,
          "Expected ${option.label} width ${bounds.width}px to meet $minimumTouchTargetPx px",
      )
      assertTrue(
          bounds.height >= minimumTouchTargetPx,
          "Expected ${option.label} height ${bounds.height}px to meet $minimumTouchTargetPx px",
      )
    }
  }

  @Test
  fun segmentContentIsCenteredWithinItsTouchTarget() {
    composeRule.setContent {
      TallybookTheme(darkTheme = true) {
        TallybookSegmentedControl(
            options = tintedOptions,
            selected = TransactionType.Expense,
            onSelect = {},
        )
      }
    }

    val segmentBounds = segmentNode("Expense").fetchSemanticsNode().boundsInRoot
    val labelBounds =
        composeRule
            .onNodeWithText("Expense", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    assertEquals(segmentBounds.center.y, labelBounds.center.y, absoluteTolerance = 1f)
  }

  @Test
  fun disabledSegmentsRevealTheTrackInsteadOfAnIndividualContainer() {
    var trackColor = Color.Unspecified
    composeRule.setContent {
      TallybookTheme(darkTheme = true) {
        trackColor = TallybookTheme.colors.surface.deep
        TallybookSegmentedControl(
            options = tintedOptions,
            selected = TransactionType.Expense,
            onSelect = {},
            enabled = false,
        )
      }
    }

    val image = composeRule.onRoot().captureToImage().toPixelMap()

    assertColorClose(trackColor, pixelInsideSegment(image, "Expense"))
    assertColorClose(trackColor, pixelInsideSegment(image, "Income"))
  }

  @Test
  fun tappingTheSelectedSegmentDoesNotAddPressedFeedback() {
    composeRule.mainClock.autoAdvance = false
    var selected by mutableStateOf(TransactionType.Expense)
    var selectedTint = Color.Unspecified
    composeRule.setContent {
      TallybookTheme(darkTheme = true) {
        selectedTint =
            TallybookTheme.colors.status.danger.base
                .copy(alpha = 0.15f)
                .compositeOver(TallybookTheme.colors.surface.deep)
        TallybookSegmentedControl(
            options = tintedOptions,
            selected = selected,
            onSelect = { selected = it },
        )
      }
    }

    segmentNode("Expense").performClick()
    composeRule.mainClock.advanceTimeByFrame()

    val image = composeRule.onRoot().captureToImage().toPixelMap()

    assertColorClose(selectedTint, pixelInsideSegment(image, "Expense"))
  }

  @Test
  fun selectedSegmentsUseSoftTonalContainerAndUnselectedSegmentsRevealTrack() {
    var selectedTint = Color.Unspecified
    var trackColor = Color.Unspecified
    composeRule.setContent {
      TallybookTheme(darkTheme = true) {
        selectedTint =
            TallybookTheme.colors.status.danger.base
                .copy(alpha = 0.15f)
                .compositeOver(TallybookTheme.colors.surface.deep)
        trackColor = TallybookTheme.colors.surface.deep
        TallybookSegmentedControl(
            options = tintedOptions,
            selected = TransactionType.Expense,
            onSelect = {},
        )
      }
    }

    val image = composeRule.onRoot().captureToImage().toPixelMap()
    val selectedPixel = pixelInsideSegment(image, "Expense")
    val unselectedPixel = pixelInsideSegment(image, "Income")

    assertColorClose(selectedTint, selectedPixel)
    assertColorClose(trackColor, unselectedPixel)
  }

  private enum class TransactionType {
    Expense,
    Income,
  }

  private fun segmentNode(label: String) =
      composeRule.onNode(
          hasText(label) and
              SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
      )

  private fun pixelInsideSegment(image: PixelMap, label: String): Color {
    val bounds = segmentNode(label).fetchSemanticsNode().boundsInRoot
    return image[(bounds.left + bounds.width * 0.15f).toInt(), bounds.center.y.toInt()]
  }

  private fun assertColorClose(expected: Color, actual: Color) {
    val tolerance = 0.01f
    assertTrue(kotlin.math.abs(expected.red - actual.red) < tolerance)
    assertTrue(kotlin.math.abs(expected.green - actual.green) < tolerance)
    assertTrue(kotlin.math.abs(expected.blue - actual.blue) < tolerance)
  }

  private companion object {
    val options =
        listOf(
            TallybookSegmentOption(TransactionType.Expense, "Expense"),
            TallybookSegmentOption(TransactionType.Income, "Income"),
        )

    val tintedOptions =
        listOf(
            TallybookSegmentOption(
                value = TransactionType.Expense,
                label = "Expense",
                icon = Lucide.ArrowUpRight,
                tone = TallybookSegmentTone.Expense,
            ),
            TallybookSegmentOption(
                value = TransactionType.Income,
                label = "Income",
                icon = Lucide.ArrowDownLeft,
                tone = TallybookSegmentTone.Income,
            ),
        )
  }
}
