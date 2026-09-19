package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookSearchFieldTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun acceptedEditRendersParentQuery() {
    val parentQuery = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookSearchField(
            query = parentQuery.value,
            onQueryChange = { parentQuery.value = it },
            placeholder = "Search movements",
            clearContentDescription = "Clear search",
            onClear = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Search movements").performTextInput("Lunch")

    composeRule.onNodeWithText("Lunch").assertIsDisplayed()
    composeRule.runOnIdle { assertEquals("Lunch", parentQuery.value) }
  }

  @Test
  fun clearActionIsAbsentForEmptyQuery() {
    composeRule.setContent {
      TallybookTheme {
        TallybookSearchField(
            query = "",
            onQueryChange = {},
            placeholder = "Search movements",
            clearContentDescription = "Clear search",
            onClear = {},
        )
      }
    }

    composeRule.onNodeWithText("Search movements").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
  }

  @Test
  fun clearActionInvokesCallbackOnceAndParentClearsQuery() {
    val parentQuery = mutableStateOf("Lunch")
    val clears = mutableIntStateOf(0)
    composeRule.setContent {
      TallybookTheme {
        TallybookSearchField(
            query = parentQuery.value,
            onQueryChange = { parentQuery.value = it },
            placeholder = "Search movements",
            clearContentDescription = "Clear search",
            onClear = {
              clears.intValue += 1
              parentQuery.value = ""
            },
        )
      }
    }

    composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed().performClick()

    composeRule.runOnIdle {
      assertEquals(1, clears.intValue)
      assertEquals("", parentQuery.value)
    }
    composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
  }

  @Test
  fun populatedQueryKeepsTheSearchFieldAtTheEmptyFieldHeight() {
    val parentQuery = mutableStateOf("")
    composeRule.setContent {
      TallybookTheme {
        TallybookSearchField(
            query = parentQuery.value,
            onQueryChange = { parentQuery.value = it },
            placeholder = "Search movements",
            clearContentDescription = "Clear search",
            onClear = { parentQuery.value = "" },
        )
      }
    }

    val emptyHeight =
        composeRule
            .onNodeWithContentDescription("Search movements")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

    composeRule.runOnIdle { parentQuery.value = "Lunch" }

    val populatedHeight =
        composeRule
            .onNodeWithContentDescription("Search movements")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

    assertEquals(emptyHeight, populatedHeight, absoluteTolerance = 1f)
  }

  @Test
  fun disabledFieldDoesNotExposeEditingOrInvokeClear() {
    val parentQuery = mutableStateOf("Lunch")
    val queryChanges = mutableIntStateOf(0)
    val clears = mutableIntStateOf(0)
    composeRule.setContent {
      TallybookTheme {
        TallybookSearchField(
            query = parentQuery.value,
            onQueryChange = {
              queryChanges.intValue += 1
              parentQuery.value = it
            },
            placeholder = "Search movements",
            clearContentDescription = "Clear search",
            enabled = false,
            onClear = { clears.intValue += 1 },
        )
      }
    }

    composeRule
        .onNodeWithContentDescription("Search movements")
        .assertIsNotEnabled()
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    composeRule.onNodeWithContentDescription("Clear search").assertIsNotEnabled().performClick()

    composeRule.runOnIdle {
      assertEquals("Lunch", parentQuery.value)
      assertEquals(0, queryChanges.intValue)
      assertEquals(0, clears.intValue)
    }
  }
}
