package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
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
    composeRule
        .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
        .performScrollToNode(hasText("2019"))
    composeRule.onNodeWithText("2019").performClick()

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

  @Test
  fun selectingAnAlreadyVisiblePeriodPreservesItsPosition() {
    var selected by mutableStateOf(22)
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerRow(
            options = (1..30).map { TallybookPeriodOption(it, it.toString()) },
            selected = selected,
            onSelect = { selected = it },
            contentDescription = "Day periods",
            modifier = Modifier.width(240.dp),
        )
      }
    }
    val nextPeriod = composeRule.onNodeWithText("23").assertIsDisplayed()
    val before = nextPeriod.fetchSemanticsNode().boundsInRoot
    nextPeriod.performClick().assertIsSelected()
    assertEquals(before, nextPeriod.fetchSemanticsNode().boundsInRoot)
  }

  @Test
  fun contextChangeAtSameIndexRevealsTheSelectedFullDate() {
    var month by mutableStateOf(6)
    composeRule.setContent {
      TallybookTheme {
        TallybookPeriodPickerRow(
            options =
                (1..30).map { day ->
                  TallybookPeriodOption(
                      LocalDate(2026, month, day),
                      day.toString(),
                      contentDescription = "2026-$month-$day",
                  )
                },
            selected = LocalDate(2026, month, 23),
            onSelect = {},
            contentDescription = "Day periods",
            modifier = Modifier.width(240.dp),
        )
      }
    }
    composeRule.onNodeWithContentDescription("2026-6-23").assertIsDisplayed().assertIsSelected()
    composeRule.onNodeWithContentDescription("Day periods").performScrollToIndex(0)
    composeRule.runOnIdle { month = 9 }
    composeRule.onNodeWithContentDescription("2026-9-23").assertIsDisplayed().assertIsSelected()
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
