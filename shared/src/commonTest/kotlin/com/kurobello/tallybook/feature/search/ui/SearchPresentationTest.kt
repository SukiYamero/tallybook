package com.kurobello.tallybook.feature.search.ui

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.feature.search.viewmodel.SearchCategoryState
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilters
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.SearchWork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPresentationTest {
  @Test
  fun equivalentCriteriaEditsKeepTheInitialFeedbackDeadline() = runTest {
    val week = pending.copy(filters = pending.filters.copy(preset = SearchDatePreset.Last7Days))
    val customWeek =
        week.copy(
            filters =
                week.filters.copy(
                    preset = SearchDatePreset.Custom,
                    customStart = LocalDate(2026, 9, 30),
                    customEnd = today,
                )
        )
    val dormantDate =
        pending.copy(filters = pending.filters.copy(customStart = LocalDate(1900, 1, 1)))
    val nextDay = pending.copy(today = LocalDate(2026, 10, 7))
    listOf(week to customWeek, pending to dormantDate, pending to nextDay).forEach {
        (initial, equivalent) ->
      assertEquals(initial.criteria, equivalent.criteria)
      val source = MutableStateFlow(initial)
      val frames = mutableListOf<SearchPresentation>()
      val collector = backgroundScope.launch {
        source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
      }
      runCurrent()
      advanceTimeBy(150)
      source.value = equivalent
      runCurrent()
      advanceTimeBy(49)
      runCurrent()
      assertTrue(frames.isEmpty())
      advanceTimeBy(1)
      runCurrent()
      assertTrue(frames.isNotEmpty())
      assertTrue(frames.last().showLoading)
      collector.cancel()
    }
  }

  @Test
  fun differentCriteriaStartTheirOwnInitialFeedbackDeadline() = runTest {
    val week = pending.copy(filters = pending.filters.copy(preset = SearchDatePreset.Last7Days))
    val source = MutableStateFlow(week)
    val frames = mutableListOf<SearchPresentation>()
    backgroundScope.launch {
      source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
    }
    runCurrent()
    advanceTimeBy(150)
    source.value = week.copy(filters = week.filters.copy(preset = SearchDatePreset.Last30Days))
    runCurrent()
    advanceTimeBy(199)
    runCurrent()
    assertTrue(frames.isEmpty())
    advanceTimeBy(1)
    runCurrent()
    assertTrue(frames.last().showLoading)
  }

  @Test
  fun initialFeedbackWaitsTwoHundredMillisecondsWithoutRestartingForMetadata() = runTest {
    val source = MutableStateFlow(pending)
    val frames = mutableListOf<SearchPresentation>()
    backgroundScope.launch {
      source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
    }
    runCurrent()
    advanceTimeBy(150)
    source.value =
        pending.copy(
            rawQuery = "coffee",
            work = SearchWork.Reading,
            categories = SearchCategoryState(emptyList(), false),
        )
    runCurrent()
    advanceTimeBy(49)
    runCurrent()
    assertTrue(frames.isEmpty())
    advanceTimeBy(1)
    runCurrent()
    assertTrue(frames.last().showLoading)
    assertTrue(frames.last().state.kind == SearchFeedbackKind.Skeleton)
  }

  @Test
  fun preparedCandidatesWaitingForTextDoNotShowInitialReadFeedbackWithoutRows() = runTest {
    val source = MutableStateFlow(pending)
    val frames = mutableListOf<SearchPresentation>()
    backgroundScope.launch {
      source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
    }
    runCurrent()
    advanceTimeBy(150)
    source.value = pending.copy(rawQuery = "coffee", work = SearchWork.WaitingText)
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertFalse(frames.any { it.showLoading })
    assertTrue(frames.last().state.kind == null)
  }

  @Test
  fun fastReadCancelsSkeletonAndCachedEmptyRevalidationUsesOnlyProgress() = runTest {
    val source = MutableStateFlow(pending)
    val frames = mutableListOf<SearchPresentation>()
    backgroundScope.launch {
      source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
    }
    runCurrent()
    advanceTimeBy(100)
    source.value = ready
    runCurrent()
    advanceTimeBy(300)
    runCurrent()
    assertFalse(frames.any { it.showLoading })
    source.value =
        ready.copy(results = RetainedLoadState.Loading(content), work = SearchWork.Revalidating)
    runCurrent()
    advanceTimeBy(200)
    runCurrent()
    assertTrue(frames.last().state.kind == SearchFeedbackKind.Progress)
  }

  @Test
  fun textDebounceDoesNotShowProgressAndMatchingHasItsOwnGracePeriod() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<SearchPresentation>()
    backgroundScope.launch {
      source.deferSearchLoading(testScheduler.timeSource).collect { frames += it }
    }
    runCurrent()
    source.value = ready.copy(rawQuery = "coffee", work = SearchWork.WaitingText)
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertFalse(frames.last().showLoading)
    source.value = source.value.copy(work = SearchWork.Matching)
    runCurrent()
    advanceTimeBy(199)
    runCurrent()
    assertFalse(frames.last().showLoading)
    advanceTimeBy(1)
    runCurrent()
    assertTrue(frames.last().showLoading)
    assertTrue(frames.last().state.kind == SearchFeedbackKind.Progress)
  }

  private companion object {
    val today = LocalDate(2026, 10, 6)
    val content =
        SearchResultsUi(
            SearchRequest(MovimientoReadCriteria(Moneda.COP), ""),
            emptyList(),
            SearchCurrencyYears(Moneda.COP, listOf(2026)),
        )
    val pending = SearchUiState("", SearchFilters.initial(today), today, Moneda.COP)
    val ready = pending.copy(results = RetainedLoadState.Ready(content), work = null)
  }
}
