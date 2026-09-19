package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookAmountInputTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun acceptedEditRendersParentValueWithoutMaterialTextFieldChrome() {
    val parentValue = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookAmountInput(
            value = parentValue.value,
            onValueChange = { parentValue.value = it },
            label = "Amount",
            currencySymbol = "$",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Amount").performTextInput("12,50")

    composeRule.onNodeWithContentDescription("Amount").assertTextEquals("12,50")
    composeRule.runOnIdle { assertEquals("12,50", parentValue.value) }
  }

  @Test
  fun errorPublishesErrorSemanticsAndPoliteMessage() {
    composeRule.setContent {
      TallybookTheme {
        TallybookAmountInput(
            value = "",
            onValueChange = {},
            label = "Amount",
            currencySymbol = "$",
            message = TallybookFieldMessage.Error("Enter an amount"),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Amount")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter an amount"))
    composeRule
        .onNodeWithText("Enter an amount")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
  }

  @Test
  fun disabledInputRejectsEditing() {
    composeRule.setContent {
      TallybookTheme {
        TallybookAmountInput(
            value = "12,50",
            onValueChange = {},
            label = "Amount",
            currencySymbol = "$",
            enabled = false,
        )
      }
    }

    composeRule.onNodeWithContentDescription("Amount").assertIsNotEnabled()
  }
}
