package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.MovimientoDateRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HistorySelectionTest {
  @Test
  fun scopeChangesPreserveAnInteriorHistoricalAnchor() {
    val day = HistorySelection(HistoryScope.Day, LocalDate(2025, 2, 19))
    val week = day.selectScope(HistoryScope.Week)
    val month = week.selectScope(HistoryScope.Month)

    assertEquals(HistoryScope.Week, week.scope)
    assertEquals(day.anchor, week.anchor)
    assertEquals(
        MovimientoDateRange(LocalDate(2025, 2, 15), LocalDate(2025, 2, 21)),
        week.range(today),
    )
    assertEquals(HistoryScope.Month, month.scope)
    assertEquals(day.anchor, month.anchor)
    assertEquals(YearMonth(2025, 2), month.contextMonth)
    assertEquals(
        MovimientoDateRange(LocalDate(2025, 2, 1), LocalDate(2025, 2, 28)),
        month.range(today),
    )
    assertEquals(day, month.selectScope(HistoryScope.Day))
  }

  @Test
  fun currentMonthChipIsCompleteWhileHistoryRangeStopsAtToday() {
    val selection = HistorySelection(HistoryScope.Month, today)

    assertEquals(
        MovimientoDateRange(LocalDate(2026, 6, 1), LocalDate(2026, 6, 30)),
        selection.periods(today).last(),
    )
    assertEquals(MovimientoDateRange(LocalDate(2026, 6, 1), today), selection.range(today))
  }

  @Test
  fun currentWeekReadAndOptionsEndOnToday() {
    val selection = HistorySelection(HistoryScope.Week, today)

    assertEquals(MovimientoDateRange(LocalDate(2026, 6, 22), today), selection.range(today))
    assertEquals(
        listOf(1 to 7, 8 to 14, 15 to 21, 22 to 23),
        selection.periods(today).map { it.start.day to it.end.day },
    )
  }

  @Test
  fun changingYearClampsLeapDayAndFutureContext() {
    val leap = HistorySelection(HistoryScope.Day, LocalDate(2024, 2, 29))
    assertEquals(LocalDate(2025, 2, 28), leap.selectYear(2025, today).anchor)

    val november = HistorySelection(HistoryScope.Week, LocalDate(2024, 11, 30))
    assertEquals(HistorySelection(HistoryScope.Week, today), november.selectYear(2026, today))
    assertEquals(LocalDate(2025, 11, 30), november.selectYear(2025, today).anchor)
  }

  @Test
  fun changingMonthPreservesAValidDayAndClampsMonthEnd() {
    val january = HistorySelection(HistoryScope.Day, LocalDate(2024, 1, 31))
    assertEquals(LocalDate(2024, 2, 29), january.selectMonth(YearMonth(2024, 2), today).anchor)
    assertEquals(LocalDate(2025, 2, 28), january.selectMonth(YearMonth(2025, 2), today).anchor)
    assertEquals(LocalDate(2025, 4, 30), january.selectMonth(YearMonth(2025, 4), today).anchor)

    val interior = HistorySelection(HistoryScope.Week, LocalDate(2025, 1, 12))
    assertEquals(LocalDate(2025, 2, 12), interior.selectMonth(YearMonth(2025, 2), today).anchor)
    assertEquals(LocalDate(2026, 6, 12), interior.selectMonth(YearMonth(2026, 11), today).anchor)
    assertEquals(today, january.selectMonth(YearMonth(2026, 6), today).anchor)
  }

  @Test
  fun choosingAnotherWeekAnchorsAtItsStartBeforeChangingToDay() {
    val selection = HistorySelection(HistoryScope.Week, LocalDate(2026, 6, 23))
    val changed =
        selection.selectPeriod(
            MovimientoDateRange(LocalDate(2026, 6, 8), LocalDate(2026, 6, 14)),
            today,
        )

    assertEquals(HistoryScope.Week, changed.scope)
    assertEquals(LocalDate(2026, 6, 8), changed.anchor)
    assertEquals(
        MovimientoDateRange(LocalDate(2026, 6, 8), LocalDate(2026, 6, 8)),
        changed.selectScope(HistoryScope.Day).range(today),
    )
  }

  @Test
  fun choosingTheSelectedWeekPreservesItsInteriorAnchor() {
    val selection = HistorySelection(HistoryScope.Week, LocalDate(2025, 6, 12))
    val unchanged =
        selection.selectPeriod(
            MovimientoDateRange(LocalDate(2025, 6, 8), LocalDate(2025, 6, 14)),
            today,
        )

    assertEquals(selection, unchanged)
    assertEquals(LocalDate(2025, 6, 12), unchanged.selectScope(HistoryScope.Day).anchor)
  }

  @Test
  fun selectingDayUsesItsExactDate() {
    val selection = HistorySelection(HistoryScope.Day, today)
    val date = LocalDate(2026, 6, 9)

    assertEquals(
        HistorySelection(HistoryScope.Day, date),
        selection.selectPeriod(MovimientoDateRange(date, date), today),
    )
  }

  @Test
  fun selectingMonthUsesTheMonthTransitionAndClampsDay() {
    val selection = HistorySelection(HistoryScope.Month, LocalDate(2025, 1, 31))

    assertEquals(
        HistorySelection(HistoryScope.Month, LocalDate(2025, 2, 28)),
        selection.selectPeriod(
            MovimientoDateRange(LocalDate(2025, 2, 1), LocalDate(2025, 2, 28)),
            today,
        ),
    )
  }

  @Test
  fun periodsFromAnotherScopeOrFutureContextAreRejected() {
    val selection = HistorySelection(HistoryScope.Week, today)
    assertFailsWith<IllegalArgumentException> {
      selection.selectPeriod(
          MovimientoDateRange(LocalDate(2026, 6, 8), LocalDate(2026, 6, 8)),
          today,
      )
    }
    assertFailsWith<IllegalArgumentException> { selection.selectYear(2027, today) }
    assertFailsWith<IllegalArgumentException> { selection.selectMonth(YearMonth(2027, 1), today) }
    assertFailsWith<IllegalArgumentException> {
      HistorySelection(HistoryScope.Day, LocalDate(2026, 6, 24)).range(today)
    }
  }

  @Test
  fun refreshedDateFollowsLiveTodayAcrossMidnightAndYearRollover() {
    val previous = LocalDate(2025, 12, 31)
    val next = LocalDate(2026, 1, 1)
    val live = HistorySelection(HistoryScope.Week, previous)

    assertEquals(HistorySelection(HistoryScope.Week, next), live.refresh(previous, next))
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        live.refresh(previous, next).availableYears(listOf(2025), next),
    )
  }

  @Test
  fun refreshedDateKeepsHistoricalAnchorAcrossMidnight() {
    val selection = HistorySelection(HistoryScope.Month, LocalDate(2025, 2, 19))
    assertEquals(selection, selection.refresh(today, LocalDate(2026, 6, 24)))
  }

  @Test
  fun backwardClockOrTimezoneChangeClampsAnyFutureAnchor() {
    val previous = LocalDate(2026, 6, 23)
    val earlier = LocalDate(2026, 6, 21)
    assertEquals(
        HistorySelection(HistoryScope.Week, earlier),
        HistorySelection(HistoryScope.Week, previous).refresh(previous, earlier),
    )
    assertEquals(
        HistorySelection(HistoryScope.Month, earlier),
        HistorySelection(HistoryScope.Month, LocalDate(2026, 6, 22)).refresh(previous, earlier),
    )
    val past = HistorySelection(HistoryScope.Day, LocalDate(2026, 6, 20))
    assertEquals(past, past.refresh(previous, earlier))
  }

  @Test
  fun yearOptionsRetainSelectedPastYearWhenItsMetadataDisappears() {
    val selection = HistorySelection(HistoryScope.Day, LocalDate(2024, 2, 29))
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        selection.availableYears(listOf(2022, 2025, 2025), today),
    )
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        selection.availableYears(emptyList(), today),
    )
  }

  @Test
  fun retainedEntryYearsRemainAvailableAfterSelectingAnotherYear() {
    val changed = HistorySelection(HistoryScope.Day, LocalDate(2024, 2, 29)).selectYear(2025, today)
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        changed.availableYears(emptyList(), today, retainedYears = setOf(2024)),
    )
    val earlier = LocalDate(2023, 12, 31)
    assertEquals(
        listOf(2023, 2022, 2021, 2020, 2019),
        changed
            .refresh(today, earlier)
            .availableYears(emptyList(), earlier, retainedYears = setOf(2022, 2024, 2025, 2026)),
    )
  }

  @Test
  fun emptyHistoryOffersEveryYearFrom2019ThroughToday() {
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        HistorySelection.initial(today).availableYears(emptyList(), today),
    )
  }

  @Test
  fun legacyYearExceptionsDoNotInventIntermediateOlderYears() {
    val selection = HistorySelection(HistoryScope.Day, LocalDate(2016, 2, 19))

    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2016, 2014, 2012),
        selection.availableYears(
            persistedYears = listOf(2014, 2014, 2027),
            today = today,
            retainedYears = setOf(2012, 2027),
        ),
    )
  }

  @Test
  fun restoredLegacyYearStaysReachableWithoutPersistedMetadata() {
    val restored = HistorySelection.restore(HistorySavedInput(1, "Month", "2010-03-10"), today)

    assertEquals(LocalDate(2010, 3, 10), restored.anchor)
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2010),
        restored.availableYears(emptyList(), today),
    )
  }

  @Test
  fun smallSavedInputRoundTripsEveryScopeAndKeepsAPastYearWithoutMetadata() {
    for (scope in HistoryScope.entries) {
      val selection = HistorySelection(scope, LocalDate(2024, 2, 29))
      val input = selection.savedInput()
      assertEquals(HistorySavedInput(1, scope.name, "2024-02-29"), input)
      val decoded = Json.decodeFromString<HistorySavedInput>(Json.encodeToString(input))
      val restored = HistorySelection.restore(decoded, today)
      assertEquals(selection, restored)
      assertEquals(
          listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
          restored.availableYears(emptyList(), today),
      )
    }
  }

  @Test
  fun absentSavedInputStartsDayAtToday() {
    assertEquals(HistorySelection(HistoryScope.Day, today), HistorySelection.initial(today))
    assertEquals(HistorySelection(HistoryScope.Day, today), HistorySelection.restore(null, today))
  }

  @Test
  fun unknownVersionOrScopeRecoversToDayAtToday() {
    val invalid =
        listOf(
            HistorySavedInput(0, "Week", "2025-02-19"),
            HistorySavedInput(2, "Month", "2025-02-19"),
            HistorySavedInput(1, "Year", "2025-02-19"),
            HistorySavedInput(1, "day", "2025-02-19"),
            HistorySavedInput(1, "", "2025-02-19"),
        )
    for (input in invalid) {
      assertEquals(
          HistorySelection(HistoryScope.Day, today),
          HistorySelection.restore(input, today),
      )
    }
  }

  @Test
  fun malformedOrFutureAnchorRecoversToDayAtToday() {
    val invalid =
        listOf(
            "",
            "not-a-date",
            "2025-02-29",
            "2024-13-01",
            "+1000000000-01-01",
            "2026-06-24",
            "2027-01-01",
        )
    for (anchor in invalid) {
      assertEquals(
          HistorySelection(HistoryScope.Day, today),
          HistorySelection.restore(HistorySavedInput(1, "Week", anchor), today),
      )
    }
  }

  @Test
  fun validSavedInputRecomputesItsRangeAgainstTheNewToday() {
    val saved = HistorySavedInput(1, "Week", "2026-06-23")
    val newToday = LocalDate(2026, 6, 25)
    val restored = HistorySelection.restore(saved, newToday)

    assertEquals(LocalDate(2026, 6, 23), restored.anchor)
    assertEquals(MovimientoDateRange(LocalDate(2026, 6, 22), newToday), restored.range(newToday))
  }

  private companion object {
    val today = LocalDate(2026, 6, 23)
  }
}
