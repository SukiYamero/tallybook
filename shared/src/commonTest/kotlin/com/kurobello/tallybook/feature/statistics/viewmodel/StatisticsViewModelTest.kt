package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
  @Test
  fun aFreshEntryReadsTheCurrentMonthInThePrimaryCurrency() = runTest {
    val (fixture, vm) = started()

    val state = vm.uiState.value
    assertEquals(StatisticsSelection.initial(TODAY), state.selection)
    assertEquals(Moneda.USD, state.moneda)
    assertTrue(state.showsMonedaSelector)
    assertEquals(StatisticsDimension.Categorias, state.dimension)
    assertTrue(2025 in state.years && 2026 in state.years)
    val content = assertIs<RetainedLoadState.Ready<StatisticsContent>>(state.content).value
    assertEquals("$10.00", content.summary.gastos)
    assertEquals(
        listOf(StatisticsSelection.initial(TODAY).request(Moneda.USD, TODAY)),
        fixture.statistics.requests,
    )
  }

  @Test
  fun scopePeriodAndYearChangesReadTheNewSelectionWhileKeepingTheLastContent() = runTest {
    val (fixture, vm) = started()
    val first = fixture.statistics.requests.single()
    fixture.statistics.respond.value = { if (it == first) success(it.moneda) else null }

    vm.onScopeChanged(StatisticsScope.Year)
    runCurrent()
    val loading = assertIs<RetainedLoadState.Loading<StatisticsContent>>(vm.uiState.value.content)
    assertEquals(StatisticsScope.Month, loading.previous?.scope)
    assertEquals(
        MovimientoDateRange(LocalDate(2026, 1, 1), TODAY),
        fixture.statistics.requests.last().period,
    )

    fixture.statistics.respond.value = { success(it.moneda) }
    runCurrent()
    assertEquals(StatisticsScope.Year, vm.uiState.value.content.cachedValue?.scope)

    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31)))
    vm.onScopeChanged(StatisticsScope.Month)
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2025, 3, 1), LocalDate(2025, 3, 31)))
    runCurrent()
    assertEquals(
        StatisticsSelection(StatisticsScope.Month, LocalDate(2025, 3, 24)),
        vm.uiState.value.selection,
    )
    vm.onYearSelected(2026)
    runCurrent()
    assertEquals(LocalDate(2026, 3, 24), vm.uiState.value.selection.anchor)
    assertEquals(
        MovimientoDateRange(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31)),
        fixture.statistics.requests.last().period,
    )

    val reads = fixture.statistics.requests.size
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)))
    vm.onYearSelected(2027)
    runCurrent()
    assertEquals(reads, fixture.statistics.requests.size)
  }

  @Test
  fun aCurrencyChangeReadsThatCurrencyAndIgnoresOneWithoutAccounts() = runTest {
    val (fixture, vm) = started()

    vm.onMonedaSelected(Moneda.COP)
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.moneda)
    assertEquals(Moneda.COP, fixture.statistics.requests.last().moneda)
    assertEquals(Moneda.COP, vm.uiState.value.content.cachedValue?.moneda)

    vm.onMonedaSelected(Moneda.BRL)
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.moneda)
  }

  @Test
  fun aDimensionChangeNeverReadsAgain() = runTest {
    val (fixture, vm) = started()

    vm.onDimensionSelected(StatisticsDimension.Etiquetas)
    runCurrent()

    assertEquals(StatisticsDimension.Etiquetas, vm.uiState.value.dimension)
    assertEquals(1, fixture.statistics.requests.size)
    assertIs<RetainedLoadState.Ready<StatisticsContent>>(vm.uiState.value.content)
  }

  @Test
  fun aFailedReadKeepsTheSelectionAndRetryReadsAgain() = runTest {
    val fixture = StatisticsFixture(this)
    fixture.statistics.respond.value = { DataOutcome.Failure(DataError.InvalidInput("boom")) }
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    val failed =
        assertIs<RetainedLoadState.Failed<StatisticsContent, StatisticsFailure>>(
            vm.uiState.value.content
        )
    assertIs<StatisticsFailure.Repository>(failed.failure)
    assertEquals(StatisticsSelection.initial(TODAY), vm.uiState.value.selection)

    fixture.statistics.respond.value = { success(it.moneda) }
    vm.onRetry()
    runCurrent()
    assertIs<RetainedLoadState.Ready<StatisticsContent>>(vm.uiState.value.content)
    assertEquals(2, fixture.statistics.requests.size)
  }

  @Test
  fun unusablePreferencesFailTheContent() = runTest {
    val fixture = StatisticsFixture(this)
    fixture.preferences.state.value = DataOutcome.Failure(DataError.InvalidInput("prefs"))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    assertIs<RetainedLoadState.Failed<StatisticsContent, StatisticsFailure>>(
        vm.uiState.value.content
    )
    assertTrue(fixture.statistics.requests.isEmpty())
  }

  @Test
  fun aPeriodChangeClosesTheSheetAndViewMovimientosOpensSearchOnceItHides() = runTest {
    val (fixture, vm) = started()
    val effects = collectEffects(vm)

    vm.onShareSelected(StatisticsKey.Categoria("comida"))
    runCurrent()
    val open = assertNotNull(vm.uiState.value.detail)
    assertEquals(3L, open.content.cachedValue?.summary?.count)
    assertEquals(1, fixture.statistics.categoryRequests.size)

    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
    vm.onPeriodSelected(september)
    runCurrent()
    val closing = assertNotNull(vm.uiState.value.detail)
    assertTrue(closing.isClosing)
    assertEquals(3L, closing.content.cachedValue?.summary?.count)
    assertEquals(1, fixture.statistics.categoryRequests.size)
    vm.onDetailHidden()
    assertNull(vm.uiState.value.detail)

    vm.onShareSelected(StatisticsKey.Categoria("comida"))
    runCurrent()
    assertEquals(september, fixture.statistics.categoryRequests.last().period)
    vm.onViewMovimientos()
    runCurrent()
    assertTrue(vm.uiState.value.detail?.isClosing == true)
    assertTrue(effects.isEmpty())

    vm.onDetailHidden()
    vm.onDetailHidden()
    runCurrent()
    assertEquals(
        listOf<StatisticsEffect>(
            StatisticsEffect.OpenSearch(StatisticsKey.Categoria("comida"), Moneda.USD, september)
        ),
        effects,
    )
  }

  @Test
  fun dismissingTheSheetOpensNothing() = runTest {
    val (_, vm) = started()
    val effects = collectEffects(vm)

    vm.onShareSelected(StatisticsKey.Categoria("comida"))
    vm.onDetailDismissed()
    vm.onViewMovimientos()
    vm.onDetailHidden()
    runCurrent()

    assertNull(vm.uiState.value.detail)
    assertTrue(effects.isEmpty())
  }

  @Test
  fun tagAndAccountRowsOpenSearchAndMedioRowsDoNothing() = runTest {
    val (_, vm) = started()
    val effects = collectEffects(vm)
    val october = MovimientoDateRange(LocalDate(2026, 10, 1), TODAY)

    vm.onShareSelected(StatisticsKey.Etiqueta("trip"))
    vm.onShareSelected(StatisticsKey.Cuenta("usd"))
    vm.onShareSelected(StatisticsKey.Medio(MedioPago.DEBITO))
    vm.onAddMovimiento()
    runCurrent()

    assertEquals(
        listOf(
            StatisticsEffect.OpenSearch(StatisticsKey.Etiqueta("trip"), Moneda.USD, october),
            StatisticsEffect.OpenSearch(StatisticsKey.Cuenta("usd"), Moneda.USD, october),
            StatisticsEffect.OpenCreation,
        ),
        effects,
    )
    assertNull(vm.uiState.value.detail)
  }

  @Test
  fun resumingOnALaterDayFollowsTheMonthInProgress() = runTest {
    val (fixture, vm) = started()

    fixture.clock.now = Instant.parse("2026-11-02T09:00:00Z")
    vm.onResume()
    runCurrent()

    assertEquals(LocalDate(2026, 11, 2), vm.uiState.value.selection.anchor)
    assertEquals(
        MovimientoDateRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 2)),
        fixture.statistics.requests.last().period,
    )
  }

  @Test
  fun theTrendSelectionResetsWithThePeriod() = runTest {
    val fixture = StatisticsFixture(this)
    fixture.statistics.respond.value = { request ->
      DataOutcome.Success(
          statisticsSnapshot(request.moneda)
              .copy(
                  months =
                      listOf(9, 10).map {
                        MonthTotals(
                            YearMonth(2026, it),
                            statisticsSnapshot(request.moneda).current,
                        )
                      }
              )
      )
    }
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    vm.onTrendMonthSelected(0)
    assertEquals(0, vm.uiState.value.trendSelection)
    vm.onTrendMonthSelected(5)
    assertEquals(0, vm.uiState.value.trendSelection)
    vm.onScopeChanged(StatisticsScope.Year)
    assertNull(vm.uiState.value.trendSelection)
  }

  @Test
  fun anEntryWithoutMovimientosIsEmpty() = runTest {
    val fixture = StatisticsFixture(this)
    fixture.statistics.respond.value = {
      DataOutcome.Success(statisticsSnapshot(it.moneda, gastos = 0).copy(hasAnyMovimiento = false))
    }
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    assertFalse(assertNotNull(vm.uiState.value.content.cachedValue).hasAnyMovimiento)
  }

  private fun TestScope.started(): Pair<StatisticsFixture, StatisticsViewModel> {
    val fixture = StatisticsFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    return fixture to vm
  }

  private fun TestScope.collectEffects(vm: StatisticsViewModel): List<StatisticsEffect> {
    val effects = mutableListOf<StatisticsEffect>()
    backgroundScope.launch { vm.effects.collect { effects += it } }
    runCurrent()
    return effects
  }
}

private val TODAY = LocalDate(2026, 10, 24)

private fun success(moneda: Moneda) = DataOutcome.Success(statisticsSnapshot(moneda))
