package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookTextFieldTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun typingIntoEnabledFieldEmitsNewValue() {
    var value = ""
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = value,
            onValueChange = { value = it },
            label = "Description",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInput("Lunch")

    composeRule.runOnIdle { assertEquals("Lunch", value) }
  }

  @Test
  fun emptyFieldDisplaysPlaceholder() {
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "",
            onValueChange = {},
            placeholder = "Enter a description",
        )
      }
    }

    composeRule.onNodeWithText("Enter a description").assertIsDisplayed()
  }

  @Test
  fun disabledFieldExposesDisabledSemantics() {
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "Lunch",
            onValueChange = {},
            label = "Description",
            enabled = false,
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assertIsNotEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
  }

  @Test
  fun readOnlyFieldIsEnabledButDoesNotExposeTextEditingAction() {
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "Lunch",
            onValueChange = {},
            label = "Description",
            readOnly = true,
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assertIsEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
  }

  @Test
  fun errorMessageExposesErrorAndPoliteLiveRegion() {
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "",
            onValueChange = {},
            label = "Description",
            message = TallybookFieldMessage.Error("Description is required"),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Description is required"))
    composeRule
        .onNodeWithText("Description is required")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
  }

  @Test
  fun supportingMessageDoesNotExposeErrorSemantics() {
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "",
            onValueChange = {},
            label = "Description",
            message = TallybookFieldMessage.Supporting("Optional"),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
  }

  @Test
  fun largeTextRemainsWithinFieldsAndGrowsVertically() {
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme {
          Column {
            TallybookTextField(
                value = "Lunch",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("value_field"),
                label = "Description",
                message = TallybookFieldMessage.Error("Description is required"),
            )
            TallybookTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("placeholder_field"),
                label = "Details",
                placeholder = "Enter a description",
                message = TallybookFieldMessage.Supporting("Optional"),
            )
          }
        }
      }
    }

    assertContentFitsInField("value_field", "Description", "Lunch", "Description is required")
    assertContentFitsInField("placeholder_field", "Details", "Enter a description", "Optional")
  }

  private fun assertContentFitsInField(fieldTag: String, vararg content: String) {
    val field = composeRule.onNodeWithTag(fieldTag).fetchSemanticsNode().boundsInRoot
    content.forEach { text ->
      val contentBounds = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
      assertTrue(contentBounds.left >= field.left)
      assertTrue(contentBounds.right <= field.right)
      assertTrue(contentBounds.top >= field.top)
      assertTrue(contentBounds.bottom <= field.bottom)
    }
  }
}
