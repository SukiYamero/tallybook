package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
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
class HomePresentationTest {
  @Test
  fun cachedEmptySnapshotRemainsEmptyWithoutInitialLoadingFeedback() = runTest {
    val empty =
        HomeUiState.ready(
            HomeContent(HomeUiState.emptySummary(Moneda.USD), emptyList(), emptyMap())
        )
    val source = MutableStateFlow(empty)
    val frames = mutableListOf<HomePresentation>()
    backgroundScope.launch {
      source.deferHomeLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = empty.revalidating()
    runCurrent()
    advanceTimeBy(500)
    runCurrent()
    assertTrue(frames.last().state.isEmpty)
    assertFalse(frames.any { it.showLoading })
  }

  @Test
  fun quickReloadRetainsTheBalanceUntilTheResultWithoutLoadingFeedback() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HomePresentation>()
    backgroundScope.launch {
      source.deferHomeLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = ready.revalidating()
    runCurrent()
    advanceTimeBy(100)
    runCurrent()
    assertEquals(ready.summary, frames.last().state.summary)
    assertEquals(ready.recent, frames.last().state.recent)
    val updated =
        HomeUiState.ready(
            requireNotNull(ready.content.cachedValue)
                .copy(
                    summary = MovimientoSummary(Money(200, Moneda.USD), Money(0, Moneda.USD)),
                    recent = listOf(movement.copy(monto = Money(200, Moneda.USD))),
                )
        )
    source.value = updated
    runCurrent()
    assertEquals(updated, frames.last().state)
    advanceTimeBy(300)
    runCurrent()
    assertFalse(frames.any { it.showLoading })
  }

  @Test
  fun pendingCurrencyChangesReceiveTheirOwnGracePeriod() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HomePresentation>()
    backgroundScope.launch {
      source.deferHomeLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = ready.revalidating()
    runCurrent()
    advanceTimeBy(150)
    source.value = HomeUiState.loading(Moneda.COP)
    runCurrent()
    advanceTimeBy(50)
    runCurrent()
    assertEquals(ready.summary, frames.last().state.summary)
    assertFalse(frames.last().showLoading)
    advanceTimeBy(150)
    runCurrent()
    assertEquals(source.value, frames.last().state)
    assertTrue(frames.last().showLoading)
  }

  @Test
  fun sameCurrencyUpdatesKeepTheDeadlineAndErrorsCancelPendingFeedback() = runTest {
    val source = MutableStateFlow(ready)
    val frames = mutableListOf<HomePresentation>()
    backgroundScope.launch {
      source.deferHomeLoading(timeSource = testScheduler.timeSource).collect { frames.add(it) }
    }
    runCurrent()
    source.value = HomeUiState.loading(Moneda.USD)
    runCurrent()
    advanceTimeBy(150)
    source.value = HomeUiState.loading(Moneda.USD).revalidating()
    runCurrent()
    advanceTimeBy(50)
    runCurrent()
    assertTrue(frames.last().showLoading)
    assertEquals(source.value, frames.last().state)
    val failed = HomeUiState.error(DataError.Local(IllegalStateException()))
    source.value = failed
    runCurrent()
    assertEquals(failed, frames.last().state)
    source.value = HomeUiState.loading()
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
    val category = Categoria("salary", "Salary", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val movement =
        Movimiento(
            "snapshot",
            TipoMovimiento.INGRESO,
            Money(100, Moneda.USD),
            LocalDate(2026, 10, 6),
            "salary",
            null,
            null,
            Instant.parse("2026-10-06T12:00:00Z"),
            Cuenta.defaultCashId(Moneda.USD),
            MedioPago.EFECTIVO,
        )
    val ready =
        HomeUiState.ready(
            HomeContent(
                MovimientoSummary(Money(100, Moneda.USD), Money(0, Moneda.USD)),
                listOf(movement),
                mapOf(category.id to category),
            )
        )
  }
}
