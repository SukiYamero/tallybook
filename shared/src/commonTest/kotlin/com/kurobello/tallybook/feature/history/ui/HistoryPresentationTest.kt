package com.kurobello.tallybook.feature.history.ui

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryPresentationTest {
  @Test
  fun cachedEmptyPeriodRemainsVisibleDuringRevalidationWithoutInitialFeedback() = runTest {
    val content =
        HistoryProjection.Content(
            MovimientoSummary(Money(0, Moneda.USD), Money(0, Moneda.USD)),
            emptyList(),
        )
    val empty = ready.copy(period = RetainedLoadState.Ready(content))
    val source = MutableStateFlow(empty)
    val frames = mutableListOf<HistoryPresentation>()
    backgroundScope.launch {
      source.deferHistoryLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = empty.copy(period = RetainedLoadState.Loading(content))
    runCurrent()
    advanceTimeBy(500)
    runCurrent()
    assertTrue(frames.last().state.isEmpty)
    assertFalse(frames.any { it.showLoading })
  }

  @Test
  fun metadataUpdatesDoNotRestartThePendingReadGracePeriod() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HistoryPresentation>()
    backgroundScope.launch {
      source.deferHistoryLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = pending
    runCurrent()
    advanceTimeBy(150)
    source.value =
        pending.copy(years = pending.years.copy(persisted = listOf(2025), isLoading = false))
    runCurrent()
    advanceTimeBy(50)
    runCurrent()
    assertTrue(frames.last().showLoading)
    assertEquals(source.value, frames.last().state)
  }

  @Test
  fun quickReadRetainsDateAndContentWithoutShowingLoading() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HistoryPresentation>()
    backgroundScope.launch {
      source.deferHistoryLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = pending
    runCurrent()
    advanceTimeBy(100)
    runCurrent()
    assertEquals(ready, frames.last().state)
    source.value = pending.copy(period = ready.period)
    runCurrent()
    assertEquals(source.value, frames.last().state)
    advanceTimeBy(300)
    runCurrent()
    assertFalse(frames.any { it.showLoading })
  }

  @Test
  fun slowReadShowsLoadingOnlyAfterGracePeriodAndFailureCancelsPendingFeedback() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HistoryPresentation>()
    backgroundScope.launch {
      source.deferHistoryLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = pending
    runCurrent()
    advanceTimeBy(199)
    runCurrent()
    assertEquals(ready, frames.last().state)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(pending, frames.last().state)
    assertTrue(frames.last().showLoading)
    val failed = pending.copy(period = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow))
    source.value = failed
    runCurrent()
    assertEquals(failed, frames.last().state)
    source.value = pending
    runCurrent()
    advanceTimeBy(100)
    source.value = failed
    runCurrent()
    advanceTimeBy(300)
    runCurrent()
    assertEquals(failed, frames.last().state)
    assertFalse(frames.last().showLoading)
  }

  private companion object {
    val today = LocalDate(2026, 10, 6)
    val movement =
        Movimiento(
            "snapshot",
            TipoMovimiento.GASTO,
            Money(100, Moneda.USD),
            today,
            "food",
            null,
            null,
            Instant.parse("2026-10-06T12:00:00Z"),
        )
    val content =
        HistoryProjection.Content(
            MovimientoSummary(Money(0, Moneda.USD), Money(100, Moneda.USD)),
            listOf(
                HistoryDayGroupUi(
                    today,
                    listOf(HistoryMovimientoRowUi(movement, null, "Food", "Unknown category")),
                )
            ),
        )
    val ready =
        HistoryUiState(
            HistorySelection(HistoryScope.Day, today),
            today,
            Moneda.USD,
            RetainedLoadState.Ready(content),
        )
    val pending =
        ready.copy(
            selection = HistorySelection(HistoryScope.Week, today),
            period = RetainedLoadState.Loading(),
        )
  }
}
