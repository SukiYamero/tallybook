package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class TallybookDateChipPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun selectingAnAvailableDayUpdatesTheParentValueAndClosesTheCalendar() {
    val selectedDate = mutableStateOf(LocalDate(2026, 9, 15))
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = selectedDate.value,
            onValueChange = { selectedDate.value = it },
            label = "Date",
            maxDate = LocalDate(2026, 9, 30),
            size = TallybookDateChipSize.M,
        )
      }
    }

    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithText("16").performClick()

    composeRule.runOnIdle { assertEquals(LocalDate(2026, 9, 16), selectedDate.value) }
    composeRule.onNodeWithText("16").assertDoesNotExist()
  }

  @Test
  fun futureDaysAreDisabled() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 9, 15),
        )
      }
    }

    composeRule.onNodeWithContentDescription("Date").performClick()

    composeRule.onNodeWithText("16").assertIsNotEnabled()
  }

  @Test
  fun selectingAYearChangesTheVisibleCalendarMonth() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 9, 15),
        )
      }
    }

    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithContentDescription("2026").performClick()
    composeRule.onNodeWithText("2025").performClick()

    composeRule.onNodeWithText("September 2025").assertIsDisplayed()
  }
}
