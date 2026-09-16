package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookTextFieldTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun acceptedEditRendersParentValue() {
    val parentValue = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { parentValue.value = it },
            label = "Description",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInput("Lunch")

    composeRule.onNodeWithContentDescription("Description").assertTextEquals("Lunch")
    composeRule.runOnIdle { assertEquals("Lunch", parentValue.value) }
  }

  @Test
  fun rapidConsecutiveEditsRetainTheLatestParentValue() {
    val parentValue = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { parentValue.value = it },
            label = "Description",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInput("L")
    composeRule.onNodeWithContentDescription("Description").performTextInput("unch")

    composeRule.onNodeWithContentDescription("Description").assertTextEquals("Lunch")
    composeRule.runOnIdle { assertEquals("Lunch", parentValue.value) }
  }

  @Test
  fun rejectedEditRetainsParentValue() {
    val parentValue = mutableStateOf("")
    val callbackCount = mutableIntStateOf(0)
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { callbackCount.intValue += 1 },
            label = "Description",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInput("Lunch")

    composeRule.onNodeWithContentDescription("Description").assertTextEquals("")
    composeRule.runOnIdle {
      assertEquals("", parentValue.value)
      assertEquals(1, callbackCount.intValue)
    }
  }

  @Test
  fun transformedEditRendersTransformedParentValue() {
    val parentValue = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { parentValue.value = it.uppercase() },
            label = "Description",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInput("Lunch")

    composeRule.onNodeWithContentDescription("Description").assertTextEquals("LUNCH")
  }

  @Test
  fun externalValueChangeSynchronizesField() {
    val parentValue = mutableStateOf("Lunch")
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { parentValue.value = it },
            label = "Description",
        )
      }
    }

    composeRule.runOnIdle { parentValue.value = "Dinner" }

    composeRule.onNodeWithContentDescription("Description").assertTextEquals("Dinner")
  }

  @Test
  fun recompositionWithSameValuePreservesTextSelection() {
    val parentValue = mutableStateOf("Lunch")
    val label = mutableStateOf("Description")
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = parentValue.value,
            onValueChange = { parentValue.value = it },
            label = label.value,
        )
      }
    }

    composeRule.onNodeWithContentDescription("Description").performTextInputSelection(TextRange(2))
    composeRule.runOnIdle { label.value = "Updated description" }

    composeRule
        .onNodeWithContentDescription("Updated description")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TextSelectionRange,
                TextRange(2),
            )
        )
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
    val callbackCount = mutableIntStateOf(0)
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "Lunch",
            onValueChange = { callbackCount.intValue += 1 },
            label = "Description",
            enabled = false,
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assertIsNotEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    composeRule.runOnIdle { assertEquals(0, callbackCount.intValue) }
  }

  @Test
  fun readOnlyFieldIsEnabledButDoesNotExposeTextEditingAction() {
    val callbackCount = mutableIntStateOf(0)
    composeRule.setContent {
      TallybookTheme {
        TallybookTextField(
            value = "Lunch",
            onValueChange = { callbackCount.intValue += 1 },
            label = "Description",
            readOnly = true,
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Description")
        .assertIsEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    composeRule.runOnIdle { assertEquals(0, callbackCount.intValue) }
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
    composeRule
        .onNodeWithText("Optional")
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
  }

  @Test
  fun largeTextRemainsWithinFieldsAndGrowsVertically() {
    composeRule.setContent {
      val density = LocalDensity.current
      TallybookTheme {
        Column {
          TallybookTextField(
              value = "Normal lunch",
              onValueChange = {},
              modifier = Modifier.fillMaxWidth().testTag("normal_value_field"),
              label = "Normal description",
              message = TallybookFieldMessage.Error("Normal description is required"),
          )
          TallybookTextField(
              value = "",
              onValueChange = {},
              modifier = Modifier.fillMaxWidth().testTag("normal_placeholder_field"),
              label = "Normal details",
              placeholder = "Enter normal description",
              message = TallybookFieldMessage.Supporting("Normal optional"),
          )
          CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            TallybookTextField(
                value = "Large lunch",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("large_value_field"),
                label = "Large description",
                message = TallybookFieldMessage.Error("Large description is required"),
            )
            TallybookTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("large_placeholder_field"),
                label = "Large details",
                placeholder = "Enter large description",
                message = TallybookFieldMessage.Supporting("Large optional"),
            )
          }
        }
      }
    }

    assertFieldGrowsAndContainsContent(
        normalTag = "normal_value_field",
        largeTag = "large_value_field",
        content = listOf("Large description", "Large lunch", "Large description is required"),
    )
    assertFieldGrowsAndContainsContent(
        normalTag = "normal_placeholder_field",
        largeTag = "large_placeholder_field",
        content = listOf("Large details", "Enter large description", "Large optional"),
    )
  }

  private fun assertFieldGrowsAndContainsContent(
      normalTag: String,
      largeTag: String,
      content: List<String>,
  ) {
    val normalField = composeRule.onNodeWithTag(normalTag).fetchSemanticsNode().boundsInRoot
    val largeField = composeRule.onNodeWithTag(largeTag).fetchSemanticsNode().boundsInRoot
    assertTrue(largeField.height > normalField.height)
    content.forEach { text ->
      val contentBounds = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
      assertTrue(contentBounds.left >= largeField.left)
      assertTrue(contentBounds.right <= largeField.right)
      assertTrue(contentBounds.top >= largeField.top)
      assertTrue(contentBounds.bottom <= largeField.bottom)
    }
  }
}
