package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class TallybookDateChipPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun compactWindowAllowsScrollingToACompleteFortyEightDpFinalCalendarRow() {
    composeRule.setContent {
      val density = LocalDensity.current
      val compactWindow =
          object : WindowInfo {
            override val isWindowFocused = true
            override val containerSize =
                with(density) { IntSize(400.dp.roundToPx(), 300.dp.roundToPx()) }
          }
      CompositionLocalProvider(LocalWindowInfo provides compactWindow) {
        TallybookTheme {
          TallybookDateChipPicker(
              value = LocalDate(2026, 9, 15),
              onValueChange = {},
              label = "Date",
              maxDate = LocalDate(2026, 9, 30),
              calendarDaySize = 48.dp,
          )
        }
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    val lastDay = composeRule.onNodeWithText("30").performScrollTo().assertIsDisplayed()
    val bounds = lastDay.getUnclippedBoundsInRoot()
    assertEquals(48.dp, bounds.bottom - bounds.top)
    val expectedPixels = with(composeRule.density) { 48.dp.toPx() }
    assertEquals(
        expectedPixels,
        lastDay.fetchSemanticsNode().boundsInRoot.height,
        absoluteTolerance = 1f,
    )
  }

  @Test
  fun extendedFloorKeepsOldHistoryReachableInABoundedYearSelector() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 9, 15),
            minYear = 1900,
            valueLabel = "15 September 2026",
        )
      }
    }
    composeRule.onNodeWithText("15 September 2026").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithContentDescription("2026").performClick()
    composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("1900"))
    composeRule.onNodeWithText("1900").performClick()
    composeRule.onNodeWithText("September 1900").assertIsDisplayed()
  }

  @Test
  fun futureValueKeepsItsLabelButOpensAtTheCorrectionMonth() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2027, 2, 1),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 9, 15),
            valueLabel = "1 February 2027",
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithText("September 2026").assertIsDisplayed()
    composeRule.onNodeWithText("16").assertIsNotEnabled()
  }

  @Test
  fun disablingAnOpenPickerDismissesTheCalendarAndRejectsSelection() {
    val enabled = mutableStateOf(true)
    var selections = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = { selections++ },
            label = "Date",
            enabled = enabled.value,
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.runOnIdle { enabled.value = false }
    composeRule.onNodeWithContentDescription("Date").assertIsNotEnabled()
    composeRule.onNodeWithText("16").assertDoesNotExist()
    composeRule.runOnIdle { assertEquals(0, selections) }
  }

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
  fun searchCanRequestFortyEightDpDaysWithoutChangingExistingDefaultSize() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 9, 15),
            calendarDaySize = 48.dp,
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    val bounds = composeRule.onNodeWithText("15").getUnclippedBoundsInRoot()
    assertEquals(48.dp, bounds.right - bounds.left)
    assertEquals(48.dp, bounds.bottom - bounds.top)
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
