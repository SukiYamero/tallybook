package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookSelectionFieldTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun emptyValueDisplaysPlaceholder() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = null,
            placeholder = "Choose a category",
            onClick = {},
        )
      }
    }

    composeRule.onNodeWithText("Choose a category").assertIsDisplayed()
  }

  @Test
  fun valueDisplaysInsteadOfPlaceholder() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = "Food",
            placeholder = "Choose a category",
            onClick = {},
        )
      }
    }

    composeRule.onNodeWithText("Food").assertIsDisplayed()
    composeRule.onNodeWithText("Choose a category").assertDoesNotExist()
  }

  @Test
  fun fieldPublishesOneEnabledButtonAndDeliversClick() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = "Food",
            placeholder = "Choose a category",
            onClick = { clicks += 1 },
        )
      }
    }

    composeRule
        .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assertCountEquals(1)
    composeRule
        .onNodeWithText("Category")
        .assertIsEnabled()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .performClick()

    composeRule.runOnIdle { assertEquals(1, clicks) }
  }

  @Test
  fun disabledFieldRejectsTouchAndDoesNotInvokeCallback() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = "Food",
            placeholder = "Choose a category",
            onClick = { clicks += 1 },
            enabled = false,
        )
      }
    }

    composeRule
        .onNodeWithText("Category")
        .assertIsNotEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        .performTouchInput { click(center) }

    composeRule.runOnIdle { assertEquals(0, clicks) }
  }

  @Test
  fun errorMessagePublishesErrorAndPoliteCopy() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = null,
            placeholder = "Choose a category",
            onClick = {},
            message = TallybookFieldMessage.Error("Category is required"),
        )
      }
    }

    composeRule
        .onNodeWithText("Category")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Category is required"))
    composeRule
        .onNodeWithText("Category is required", useUnmergedTree = true)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
  }

  @Test
  fun supportingMessageDoesNotPublishErrorOrLiveRegion() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSelectionField(
            label = "Category",
            value = null,
            placeholder = "Choose a category",
            onClick = {},
            message = TallybookFieldMessage.Supporting("Optional"),
        )
      }
    }

    composeRule
        .onNodeWithText("Category")
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    composeRule
        .onNodeWithText("Optional", useUnmergedTree = true)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
  }

  @Test
  fun largeTextRemainsWithinFieldsAndGrowsVertically() {
    composeRule.setContent {
      val density = LocalDensity.current
      TallybookTheme {
        Column {
          TallybookSelectionField(
              label = "Normal category",
              value = "Normal food",
              placeholder = "Choose normal category",
              onClick = {},
              modifier = Modifier.fillMaxWidth().testTag("normal_selection_field"),
              message = TallybookFieldMessage.Error("Normal category is required"),
          )
          CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            TallybookSelectionField(
                label = "Large category",
                value = "Large food",
                placeholder = "Choose large category",
                onClick = {},
                modifier = Modifier.fillMaxWidth().testTag("large_selection_field"),
                message = TallybookFieldMessage.Error("Large category is required"),
            )
          }
        }
      }
    }

    val normalField =
        composeRule.onNodeWithTag("normal_selection_field").fetchSemanticsNode().boundsInRoot
    val largeField =
        composeRule.onNodeWithTag("large_selection_field").fetchSemanticsNode().boundsInRoot
    assertTrue(largeField.height > normalField.height)
    listOf("Large category", "Large food", "Large category is required").forEach { text ->
      val contentBounds =
          composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
      assertTrue(contentBounds.left >= largeField.left)
      assertTrue(contentBounds.right <= largeField.right)
      assertTrue(contentBounds.top >= largeField.top)
      assertTrue(contentBounds.bottom <= largeField.bottom)
    }
  }
}
