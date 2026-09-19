package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class TallybookPeriodPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun selectingAPeriodReturnsItsTypedValue() {
    var selected: String? = null
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerRow(
            options = dayOptions,
            selected = "2026-06-23",
            onSelect = { selected = it },
            contentDescription = "Day periods",
        )
      }
    }

    composeRule.onNodeWithText("24").performClick()

    composeRule.runOnIdle { assertEquals("2026-06-24", selected) }
  }

  @Test
  fun selectedPeriodPublishesSelectedRadioSemantic() {
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerRow(
            options = dayOptions,
            selected = "2026-06-23",
            onSelect = {},
            contentDescription = "Day periods",
        )
      }
    }

    composeRule
        .onNodeWithText("23")
        .assertIsSelected()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
  }

  @Test
  fun typedPeriodValuesRenderWithSaveableItemKeys() {
    val selected = TallybookDayPeriod(LocalDate(2026, 6, 23))
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerRow(
            options = listOf(TallybookPeriodOption(value = selected, label = "23")),
            selected = selected,
            onSelect = {},
            contentDescription = "Day periods",
        )
      }
    }

    composeRule.onNodeWithText("23").assertIsDisplayed()
  }

  @Test
  fun selectingAYearReturnsItsValueAndClosesTheMenu() {
    var selectedYear by mutableStateOf(2026)
    composeRule.setContent {
      TallybookTheme {
        TallybookYearMenu(
            years = listOf(2026, 2025, 2024),
            selectedYear = selectedYear,
            onSelect = { selectedYear = it },
            contentDescription = "Year selector",
        )
      }
    }

    composeRule.onNodeWithText("2026").performClick()
    composeRule.onNodeWithText("2025").performClick()

    composeRule.runOnIdle { assertEquals(2025, selectedYear) }
    composeRule.onNodeWithText("2024").assertDoesNotExist()
  }

  @Test
  fun scrollingTheYearMenuRevealsYearsBeyondTheVisibleLimit() {
    var selectedYear by mutableStateOf(2026)
    composeRule.setContent {
      TallybookTheme {
        TallybookYearMenu(
            years = (2026 downTo 2019).toList(),
            selectedYear = selectedYear,
            onSelect = { selectedYear = it },
            contentDescription = "Year selector",
            maxVisibleYears = 4,
        )
      }
    }

    composeRule.onNodeWithText("2026").performClick()
    composeRule.onNodeWithText("2019").performScrollTo().performClick()

    composeRule.runOnIdle { assertEquals(2019, selectedYear) }
  }

  @Test
  fun periodHeaderRendersContextToTheLeftOfItsYearControl() {
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerHeader(
            title = "Tuesday 23",
            supportingText = "June",
            yearControl = { TallybookText(text = "2026") },
        )
      }
    }

    composeRule.onNodeWithText("Tuesday 23").assertIsDisplayed()
    composeRule.onNodeWithText("June").assertIsDisplayed()
    composeRule.onNodeWithText("2026").assertIsDisplayed()
  }

  private companion object {
    val dayOptions =
        listOf(
            TallybookPeriodOption(value = "2026-06-22", label = "22", caption = "L"),
            TallybookPeriodOption(value = "2026-06-23", label = "23", caption = "M"),
            TallybookPeriodOption(value = "2026-06-24", label = "24", caption = "M"),
        )
  }
}
