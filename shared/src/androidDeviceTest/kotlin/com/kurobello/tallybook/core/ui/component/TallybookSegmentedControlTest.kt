package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    composeRule.onNodeWithText("Income").assertIsNotEnabled().performClick()

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

  private enum class TransactionType {
    Expense,
    Income,
  }

  private companion object {
    val options =
        listOf(
            TallybookSegmentOption(TransactionType.Expense, "Expense"),
            TallybookSegmentOption(TransactionType.Income, "Income"),
        )
  }
}
