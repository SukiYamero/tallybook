package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.calendar_next_month
import tallybook.shared.generated.resources.calendar_previous_month
import tallybook.shared.generated.resources.date_picker_months

class TallybookDateChipPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun compactWidthsKeepSaturdayAndSundayReachableWithFullClickableTargets() {
    val windowWidth = mutableStateOf(320)
    val selections = mutableListOf<LocalDate>()
    val month = runBlocking { getStringArray(Res.array.date_picker_months)[8] }
    composeRule.setContent {
      val density = LocalDensity.current
      val compactWindow =
          object : WindowInfo {
            override val isWindowFocused = true
            override val containerSize =
                with(density) { IntSize(windowWidth.value.dp.roundToPx(), 640.dp.roundToPx()) }
          }
      CompositionLocalProvider(LocalWindowInfo provides compactWindow) {
        TallybookTheme {
          TallybookDateChipPicker(
              value = LocalDate(2026, 9, 15),
              onValueChange = { selections += it },
              label = "Date",
              maxDate = LocalDate(2026, 9, 30),
              calendarDaySize = 48.dp,
          )
        }
      }
    }
    listOf(320, 360).forEach { width ->
      composeRule.runOnIdle { windowWidth.value = width }
      composeRule.onNodeWithContentDescription("Date").performClick()
      composeRule
          .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
          .performTouchInput { swipeLeft() }
      listOf(26, 27).forEach { day ->
        val description = runBlocking {
          getString(Res.string.calendar_date_content_description, day, month, 2026)
        }
        val cell =
            composeRule
                .onNodeWithContentDescription(description)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val pixels = with(composeRule.density) { 48.dp.toPx() }
        val bounds = cell.fetchSemanticsNode().boundsInRoot
        assertEquals(pixels, bounds.width, absoluteTolerance = 1f)
        assertEquals(pixels, bounds.height, absoluteTolerance = 1f)
        if (day == 27) cell.performTouchInput { click() }
      }
    }
    composeRule.runOnIdle {
      assertEquals(listOf(LocalDate(2026, 9, 27), LocalDate(2026, 9, 27)), selections)
    }
  }

  @Test
  fun raisingTheYearFloorWhileBrowsingKeepsNextMonthAvailableForRecovery() {
    val minimumYear = mutableStateOf(1900)
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 10, 6),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 10, 6),
            minYear = minimumYear.value,
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithContentDescription("2026").performClick()
    composeRule.onNodeWithText("2019").performClick()
    composeRule.runOnIdle { minimumYear.value = 2026 }
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_next_month) })
        .assertIsEnabled()
        .performClick()
    val month = runBlocking { getStringArray(Res.array.date_picker_months)[10] }
    composeRule.onNodeWithText("$month 2019").assertIsDisplayed()
  }

  @Test
  fun minimumDateRejectsEarlierDaysAndAllowsTheInclusiveEndpoint() {
    val selections = mutableListOf<LocalDate>()
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 9, 15),
            onValueChange = { selections += it },
            label = "Date",
            maxDate = LocalDate(2026, 10, 6),
            minDate = LocalDate(2026, 9, 20),
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_previous_month) })
        .assertIsNotEnabled()
    composeRule.onNodeWithText("19").assertIsNotEnabled().performTouchInput { click() }
    composeRule.runOnIdle { assertEquals(emptyList(), selections) }
    composeRule.onNodeWithText("20").assertIsEnabled().performClick()
    composeRule.runOnIdle { assertEquals(listOf(LocalDate(2026, 9, 20)), selections) }
  }

  @Test
  fun lowerBoundRevealsItsMonthAndExcludesEarlierYears() {
    val month = runBlocking { getStringArray(Res.array.date_picker_months)[8] }
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(1900, 1, 1),
            onValueChange = {},
            label = "Date",
            maxDate = LocalDate(2026, 10, 6),
            minYear = 1900,
            valueLabel = "1 January 1900",
            minDate = LocalDate(2025, 9, 20),
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithText("$month 2025").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("2025").performClick()
    composeRule.onNodeWithText("2024").assertDoesNotExist()
    composeRule.onNodeWithText("1900").assertDoesNotExist()
    composeRule.onNodeWithText("2026").performClick()
    composeRule.onNodeWithText("$month 2026").assertIsDisplayed()
  }

  @Test
  fun boundsChangingDuringPressRejectSelectionUntilTheMinimumIsCorrected() {
    val minimum = mutableStateOf(LocalDate(2026, 10, 1))
    val selections = mutableListOf<LocalDate>()
    composeRule.setContent {
      TallybookTheme {
        TallybookDateChipPicker(
            value = LocalDate(2026, 10, 6),
            onValueChange = { selections += it },
            label = "Date",
            maxDate = LocalDate(2026, 10, 6),
            valueLabel = "6 October 2026",
            minDate = minimum.value,
        )
      }
    }
    composeRule.onNodeWithContentDescription("Date").performClick()
    composeRule.onNodeWithText("5").performTouchInput { down(center) }
    composeRule.runOnIdle { minimum.value = LocalDate(2026, 10, 7) }
    composeRule.onNodeWithText("5").assertIsNotEnabled().performTouchInput { up() }
    composeRule.onNodeWithContentDescription("2026").assertIsNotEnabled()
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_previous_month) })
        .assertIsNotEnabled()
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_next_month) })
        .assertIsNotEnabled()
    composeRule.onNodeWithText("6 October 2026").assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(emptyList(), selections)
      minimum.value = LocalDate(2026, 10, 1)
    }
    composeRule.onNodeWithText("5").assertIsEnabled().performClick()
    composeRule.runOnIdle { assertEquals(listOf(LocalDate(2026, 10, 5)), selections) }
  }

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
