package com.kurobello.tallybook.feature.home

import androidx.lifecycle.ViewModelStore
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.FakeSaldoRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.SaldoRepository
import com.kurobello.tallybook.core.data.StatisticsRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.statisticsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  @Test
  fun immediateFailureOnCurrencyChangeDoesNotRetainTheOtherCurrencySnapshot() = runTest {
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.USD))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(HomeUiState.emptySummary(Moneda.USD)),
        )
    val vm =
        createViewModel(
            preferences,
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
        )
    runCurrent()
    assertTrue(vm.uiState.value.hasSnapshot)
    val failure = DataError.Local(IllegalStateException())
    preferences.emit(readyPreferences(Moneda.COP))
    movimientos.emitRecent(DataOutcome.Failure(failure))
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.moneda)
    assertEquals(failure, vm.uiState.value.error)
    assertFalse(vm.uiState.value.hasSnapshot)
    movimientos.emitRecent(DataOutcome.Success(emptyList()))
    movimientos.awaitNextSummary()
    vm.retry()
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.moneda)
    assertTrue(vm.uiState.value.content.isInitialLoading)
  }

  @Test
  fun firstReadFailureKeepsItsKnownRequestCurrency() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.USD)),
            FakeMovimientoRepository(recent = DataOutcome.Failure(failure)),
            FakeCategoriaRepository(),
            backgroundScope,
        )
    runCurrent()
    assertEquals(Moneda.USD, vm.uiState.value.moneda)
    assertEquals(failure, vm.uiState.value.error)
  }

  @Test
  fun completedEmptySnapshotSurvivesReadFailureAndPendingRetry() = runTest {
    val cachedSummary = HomeUiState.emptySummary(Moneda.USD)
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(cachedSummary),
        )
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.USD)),
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
        )
    runCurrent()
    assertTrue(vm.uiState.value.isEmpty)
    val failure = DataError.Local(IllegalStateException())
    movimientos.emitRecent(DataOutcome.Failure(failure))
    runCurrent()
    assertEquals(failure, vm.uiState.value.error)
    assertEquals(cachedSummary, vm.uiState.value.summary)
    movimientos.emitRecent(DataOutcome.Success(emptyList()))
    movimientos.awaitNextSummary()
    vm.retry()
    runCurrent()
    assertTrue(vm.uiState.value.isLoading)
    assertTrue(vm.uiState.value.isEmpty)
    assertNull(vm.uiState.value.error)
  }

  @Test
  fun startsLoadingBeforeEverySourceEmits() = runTest {
    val preferences = FakePreferencesRepository()
    val viewModel = createViewModel(preferences = preferences, scope = backgroundScope)

    runCurrent()

    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun exposesAnEmptyHomeAfterReadySourcesEmitInDifferentOrders() = runTest {
    val preferences = FakePreferencesRepository()
    val movimientos = FakeMovimientoRepository()
    val categorias = FakeCategoriaRepository()
    val viewModel =
        createViewModel(
            preferences = preferences,
            movimientos = movimientos,
            categorias = categorias,
            scope = backgroundScope,
        )

    categorias.emit(DataOutcome.Success(emptyList()))
    movimientos.emitRecent(DataOutcome.Success(emptyList()))
    preferences.emit(readyPreferences(Moneda.USD))
    movimientos.emitSummary(DataOutcome.Success(summary(Moneda.USD)))
    runCurrent()

    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.isEmpty)
    assertNull(viewModel.uiState.value.error)
    assertEquals(Moneda.USD, viewModel.uiState.value.summary.balance.moneda)
  }

  @Test
  fun exposesOnlyPrincipalCurrencyMovimientosAndCategories() = runTest {
    val category = categoria("food")
    val usdMovimiento = movimiento("usd", Moneda.USD, category.id)
    val copMovimiento = movimiento("cop", Moneda.COP, category.id)
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.USD))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(listOf(copMovimiento, usdMovimiento)),
            summary = DataOutcome.Success(summary(Moneda.USD)),
        )
    val categorias = FakeCategoriaRepository(DataOutcome.Success(listOf(category)))
    val viewModel =
        createViewModel(
            preferences = preferences,
            movimientos = movimientos,
            categorias = categorias,
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(listOf(Moneda.USD to 6), movimientos.recentRequests)
    assertEquals(listOf(usdMovimiento), viewModel.uiState.value.recent)
    assertEquals(category, viewModel.uiState.value.categoriasById[category.id])
    assertEquals(Moneda.USD, viewModel.uiState.value.summary.balance.moneda)
  }

  @Test
  fun preservesNoPartialDataWhenAnUpstreamFailsAndRetryResubscribesEverything() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.COP))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Failure(failure),
            summary = DataOutcome.Success(summary(Moneda.COP)),
        )
    val categorias = FakeCategoriaRepository(DataOutcome.Success(listOf(categoria("food"))))
    val viewModel =
        createViewModel(
            preferences = preferences,
            movimientos = movimientos,
            categorias = categorias,
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(failure, viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.recent.isEmpty())
    assertTrue(viewModel.uiState.value.categoriasById.isEmpty())

    movimientos.emitRecent(DataOutcome.Success(emptyList()))
    viewModel.retry()
    runCurrent()

    assertEquals(2, preferences.subscriptionCount)
    assertEquals(2, movimientos.summarySubscriptions)
    assertEquals(2, movimientos.recentSubscriptions)
    assertEquals(2, categorias.subscriptionCount)
    assertNull(viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.isEmpty)
  }

  @Test
  fun exposesAnUpstreamFailureBeforeTheOtherSourcesEmit() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val viewModel =
        createViewModel(
            preferences = FakePreferencesRepository(readyPreferences(Moneda.COP)),
            movimientos = FakeMovimientoRepository(recent = DataOutcome.Failure(failure)),
            categorias = FakeCategoriaRepository(),
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(failure, viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun receivesReactiveInsertInvalidationWithoutOptimisticStateMutation() = runTest {
    val category = categoria("food")
    val movimiento = movimiento("new", Moneda.COP, category.id)
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.COP))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(summary(Moneda.COP)),
        )
    val viewModel =
        createViewModel(
            preferences = preferences,
            movimientos = movimientos,
            categorias = FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
            scope = backgroundScope,
        )

    runCurrent()
    movimientos.invalidateAfterInsert(movimiento)
    runCurrent()

    assertEquals(listOf(movimiento), viewModel.uiState.value.recent)
    assertEquals(0, movimientos.insertCalls)
  }

  @Test
  fun readsOnlyWithConsumersAndMarksFirstReentryStaleAfterHiddenWrite() = runTest {
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.USD))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(summary(Moneda.USD)),
        )
    val categorias = FakeCategoriaRepository(DataOutcome.Success(emptyList()))
    val vm =
        HomeViewModel(
            HomeRepositories(
                preferences,
                movimientos,
                categorias,
                primarySaldos(),
                FakeHomeStatistics(),
            ),
            today(),
            backgroundScope,
        )
    runCurrent()
    assertEquals(0, preferences.subscriptionCount)
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    val second = backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(1, preferences.subscriptionCount)
    assertFalse(vm.uiState.value.isLoading)
    collector.cancel()
    second.cancel()
    runCurrent()
    advanceTimeBy(4_999)
    runCurrent()
    assertEquals(1, movimientos.activeRecent)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(0, movimientos.activeRecent)
    assertTrue(vm.uiState.value.isLoading)
    val hidden = movimiento("hidden", Moneda.USD, "food")
    movimientos.invalidateAfterInsert(hidden)
    var first: HomeUiState? = null
    backgroundScope.launch { vm.uiState.take(1).collect { first = it } }
    runCurrent()
    assertTrue(requireNotNull(first).isLoading)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(listOf(hidden), vm.uiState.value.recent)
  }

  @Test
  fun pendingReentryReadsPreserveCachedDataWithoutClaimingSuccess() = runTest {
    val category = categoria("food")
    val movement = movimiento("cached", Moneda.USD, category.id)
    val cachedSummary = summary(Moneda.USD)
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(listOf(movement)),
            summary = DataOutcome.Success(cachedSummary),
        )
    val preferences = FakePreferencesRepository(readyPreferences(Moneda.USD))
    val vm =
        HomeViewModel(
            HomeRepositories(
                preferences,
                movimientos,
                FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
                primarySaldos(),
                FakeHomeStatistics(),
            ),
            today(),
            backgroundScope,
        )
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    collector.cancel()
    runCurrent()
    advanceTimeBy(5_000)
    runCurrent()
    movimientos.awaitNextSummary()
    vm.onResume()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    assertTrue(vm.uiState.value.isLoading)
    assertEquals(cachedSummary, vm.uiState.value.summary)
    assertEquals(listOf(movement), vm.uiState.value.recent)
    assertEquals(mapOf(category.id to category), vm.uiState.value.categoriasById)

    val refreshed = MovimientoSummary(Money(2_000, Moneda.USD), Money(250, Moneda.USD))
    movimientos.emitSummary(DataOutcome.Success(refreshed))
    runCurrent()
    assertFalse(vm.uiState.value.isLoading)
    assertEquals(refreshed, vm.uiState.value.summary)

    movimientos.awaitNextSummary()
    preferences.emit(readyPreferences(Moneda.COP))
    runCurrent()
    assertTrue(vm.uiState.value.isLoading)
    assertEquals(HomeUiState.emptySummary(Moneda.COP), vm.uiState.value.summary)
    assertTrue(vm.uiState.value.recent.isEmpty())
    assertTrue(vm.uiState.value.categoriasById.isEmpty())
    movimientos.emitSummary(DataOutcome.Success(summary(Moneda.COP)))
    runCurrent()
    assertFalse(vm.uiState.value.isLoading)
    assertEquals(Moneda.COP, vm.uiState.value.summary.balance.moneda)
  }

  @Test
  fun clearingHomeStopsOwnedReadsWithoutCancelingInjectedScope() = runTest {
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(summary(Moneda.USD)),
        )
    val viewModel =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.USD)),
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
        )
    val sibling = backgroundScope.launch { awaitCancellation() }
    runCurrent()
    assertEquals(1, movimientos.activeRecent)
    ViewModelStore().apply {
      put("home", viewModel)
      clear()
    }
    runCurrent()
    assertEquals(0, movimientos.activeRecent)
    assertTrue(sibling.isActive)
  }

  @Test
  fun theCardShowsThePrimaryCurrencyTotalBalanceFromTheReadModel() = runTest {
    val saldos = primarySaldos(Money(125_000, Moneda.COP), Money(9_900, Moneda.USD))
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            FakeMovimientoRepository(
                recent = DataOutcome.Success(emptyList()),
                summary = DataOutcome.Success(summary(Moneda.COP)),
            ),
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            saldos = saldos,
        )
    runCurrent()
    assertEquals(Money(125_000, Moneda.COP), vm.uiState.value.totalBalance)

    saldos.saldos.value =
        saldos.saldos.value.map {
          if (it.saldo.moneda == Moneda.COP) it.copy(saldo = Money(100_000, Moneda.COP)) else it
        }
    runCurrent()
    assertEquals(Money(100_000, Moneda.COP), vm.uiState.value.totalBalance)
  }

  @Test
  fun theCardsSecondaryValuesAreThisCivilMonthInTheInjectedTimeZone() = runTest {
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(summary(Moneda.COP)),
        )
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            today =
                CivilToday(
                    MutableClock(Instant.parse("2026-10-01T03:00:00Z")),
                    TimeZoneProvider { TimeZone.of("UTC-05:00") },
                ),
        )
    runCurrent()

    assertEquals(
        listOf(Moneda.COP to MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))),
        movimientos.summaryRanges,
    )
    assertEquals(summary(Moneda.COP), vm.uiState.value.summary)
  }

  @Test
  fun resumingInANewMonthReadsThatMonth() = runTest {
    val clock = MutableClock(Instant.parse("2026-10-31T12:00:00Z"))
    val movimientos =
        FakeMovimientoRepository(
            recent = DataOutcome.Success(emptyList()),
            summary = DataOutcome.Success(summary(Moneda.COP)),
        )
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            today = today(clock),
        )
    runCurrent()
    assertEquals(OCTOBER, movimientos.summaryRanges.last().second)

    clock.now = Instant.parse("2026-11-01T12:00:00Z")
    vm.onResume()
    runCurrent()

    assertEquals(
        MovimientoDateRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)),
        movimientos.summaryRanges.last().second,
    )
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun withoutALivePrimaryCurrencyAccountTheTotalIsZeroInThatCurrency() = runTest {
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            FakeMovimientoRepository(
                recent = DataOutcome.Success(emptyList()),
                summary = DataOutcome.Success(HomeUiState.emptySummary(Moneda.COP)),
            ),
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            saldos =
                FakeSaldoRepository(
                    primarySaldos(Money(9_900, Moneda.USD)).saldos.value.filter {
                      it.saldo.moneda == Moneda.USD
                    }
                ),
        )
    runCurrent()

    assertTrue(vm.uiState.value.hasSnapshot)
    assertNull(vm.uiState.value.error)
    assertEquals(Money(0, Moneda.COP), vm.uiState.value.totalBalance)
  }

  @Test
  fun aBalanceReadFailureIsAHomeFailure() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val saldos = primarySaldos().apply { this.failure.value = failure }
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            FakeMovimientoRepository(
                recent = DataOutcome.Success(emptyList()),
                summary = DataOutcome.Success(summary(Moneda.COP)),
            ),
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            saldos = saldos,
        )
    runCurrent()

    assertEquals(failure, vm.uiState.value.error)
  }

  @Test
  fun theStatisticsCardReadsThisMonthInThePrimaryCurrency() = runTest {
    val statistics = FakeHomeStatistics()
    readyHome(statistics)
    runCurrent()

    assertEquals(
        listOf(statisticsRequest(StatisticsScope.Month, OCTOBER_24, Moneda.COP, OCTOBER_24)),
        statistics.requests,
    )
  }

  @Test
  fun theStatisticsCardShowsTheTopThreeCategoriasTheRestAndThePace() = runTest {
    val statistics =
        FakeHomeStatistics(
            DataOutcome.Success(
                monthSnapshot(
                    categorias =
                        listOf(
                            "a" to 4_000L,
                            "b" to 3_000L,
                            "c" to 2_000L,
                            "d" to 500L,
                            "e" to 500L,
                        ),
                    previousMonthGastos = 10_000,
                )
            )
        )
    val vm = readyHome(statistics)
    runCurrent()

    assertEquals(
        HomeStatistics(
            top =
                listOf(
                    HomeCategoryShare("a", 4_000),
                    HomeCategoryShare("b", 3_000),
                    HomeCategoryShare("c", 2_000),
                ),
            otherBasisPoints = 1_000,
            pace = Change(29, Direction.Up),
        ),
        vm.uiState.value.statistics,
    )
  }

  @Test
  fun aFourthCategoriaJoinsTheRestOfTheCard() = runTest {
    val statistics =
        FakeHomeStatistics(
            DataOutcome.Success(
                monthSnapshot(
                    categorias = listOf("a" to 5_000L, "b" to 2_000L, "c" to 2_000L, "d" to 1_000L)
                )
            )
        )
    val vm = readyHome(statistics)
    runCurrent()

    val card = assertNotNull(vm.uiState.value.statistics)
    assertEquals(listOf("a", "b", "c"), card.top.map { it.categoriaId })
    assertEquals(1_000, card.otherBasisPoints)
  }

  @Test
  fun withoutAPreviousMonthWithGastoTheCardHasNoPace() = runTest {
    val statistics =
        FakeHomeStatistics(DataOutcome.Success(monthSnapshot(categorias = listOf("a" to 2_400L))))
    val vm = readyHome(statistics)
    runCurrent()

    val card = assertNotNull(vm.uiState.value.statistics)
    assertEquals(listOf(HomeCategoryShare("a", 10_000)), card.top)
    assertEquals(0, card.otherBasisPoints)
    assertNull(card.pace)
  }

  @Test
  fun aMonthWithoutGastoHidesTheStatisticsCard() = runTest {
    val statistics = FakeHomeStatistics()
    val vm = readyHome(statistics)
    runCurrent()
    assertTrue(vm.uiState.value.hasSnapshot)
    assertNull(vm.uiState.value.statistics)

    statistics.emit(DataOutcome.Success(monthSnapshot(categorias = listOf("a" to 100L))))
    runCurrent()
    assertNotNull(vm.uiState.value.statistics)

    statistics.emit(DataOutcome.Success(monthSnapshot(categorias = emptyList())))
    runCurrent()
    assertNull(vm.uiState.value.statistics)
  }

  @Test
  fun aStatisticsReadFailureHidesOnlyTheCard() = runTest {
    val statistics =
        FakeHomeStatistics(DataOutcome.Success(monthSnapshot(categorias = listOf("a" to 100L))))
    val vm =
        createViewModel(
            FakePreferencesRepository(readyPreferences(Moneda.COP)),
            FakeMovimientoRepository(
                recent = DataOutcome.Success(emptyList()),
                summary = DataOutcome.Success(summary(Moneda.COP)),
            ),
            FakeCategoriaRepository(DataOutcome.Success(emptyList())),
            backgroundScope,
            statistics = statistics,
        )
    runCurrent()
    assertNotNull(vm.uiState.value.statistics)

    statistics.emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()

    assertNull(vm.uiState.value.statistics)
    assertNull(vm.uiState.value.error)
    assertTrue(vm.uiState.value.hasSnapshot)
    assertFalse(vm.uiState.value.isLoading)
    assertEquals(summary(Moneda.COP), vm.uiState.value.summary)
  }

  private fun TestScope.readyHome(statistics: FakeHomeStatistics): HomeViewModel =
      createViewModel(
          FakePreferencesRepository(readyPreferences(Moneda.COP)),
          FakeMovimientoRepository(
              recent = DataOutcome.Success(emptyList()),
              summary = DataOutcome.Success(summary(Moneda.COP)),
          ),
          FakeCategoriaRepository(DataOutcome.Success(emptyList())),
          backgroundScope,
          statistics = statistics,
      )

  private fun createViewModel(
      preferences: AppPreferencesRepository,
      movimientos: MovimientoRepository = FakeMovimientoRepository(),
      categorias: CategoriaRepository = FakeCategoriaRepository(),
      scope: CoroutineScope,
      saldos: SaldoRepository = primarySaldos(),
      today: CivilToday = today(),
      statistics: StatisticsRepository = FakeHomeStatistics(),
  ): HomeViewModel {
    val viewModel =
        HomeViewModel(
            repositories =
                HomeRepositories(preferences, movimientos, categorias, saldos, statistics),
            today = today,
            scope = scope,
        )
    scope.launch { viewModel.uiState.collect {} }
    return viewModel
  }

  /** Answers every read with the latest [emit]; the default is a month without gasto. */
  private class FakeHomeStatistics(
      initial: DataOutcome<StatisticsSnapshot>? = null,
  ) : StatisticsRepository {
    private val values = MutableStateFlow(initial)
    val requests = mutableListOf<StatisticsRequest>()

    override fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>> = flow {
      requests += request
      emitAll(values.map { it ?: DataOutcome.Success(monthSnapshot(request.moneda, emptyList())) })
    }

    override fun observeCategory(
        request: CategoryStatisticsRequest
    ): Flow<DataOutcome<CategoryStatisticsSnapshot>> = error("Home reads no category statistics")

    fun emit(value: DataOutcome<StatisticsSnapshot>) {
      values.value = value
    }
  }

  private class FakePreferencesRepository(
      initial: DataOutcome<AppPreferencesState>? = null,
  ) : AppPreferencesRepository {
    private val values = MutableStateFlow(initial)
    var subscriptionCount = 0
      private set

    override val preferences: Flow<DataOutcome<AppPreferencesState>> = flow {
      subscriptionCount += 1
      emitAll(values.filterNotNull())
    }

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    fun emit(value: DataOutcome<AppPreferencesState>) {
      values.value = value
    }
  }

  private class FakeMovimientoRepository(
      recent: DataOutcome<List<Movimiento>>? = null,
      summary: DataOutcome<MovimientoSummary>? = null,
  ) : MovimientoRepository {
    override suspend fun defaultSelection(
        primary: Moneda,
        tipo: TipoMovimiento,
        medio: MedioPago?,
    ): DataOutcome<CuentaSelection?> = error("Default selections are not expected in this fixture")

    private val recentValues = MutableStateFlow(recent)
    private val summaryValues = MutableStateFlow(summary)
    val recentRequests = mutableListOf<Pair<Moneda, Int>>()
    var insertCalls = 0
      private set

    var activeRecent = 0
      private set

    var recentSubscriptions = 0
      private set

    var summarySubscriptions = 0
      private set

    override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
        flow {
          recentRequests += moneda to limit
          recentSubscriptions += 1
          activeRecent += 1
          try {
            emitAll(
                recentValues.filterNotNull().map { outcome ->
                  when (outcome) {
                    is DataOutcome.Success ->
                        DataOutcome.Success(
                            outcome.data.filter { it.monto.moneda == moneda }.take(limit)
                        )
                    is DataOutcome.Failure -> outcome
                  }
                }
            )
          } finally {
            activeRecent -= 1
          }
        }

    val summaryRanges = mutableListOf<Pair<Moneda, MovimientoDateRange>>()

    override fun observeSummary(
        moneda: Moneda,
        range: MovimientoDateRange,
    ): Flow<DataOutcome<MovimientoSummary>> = flow {
      summaryRanges += moneda to range
      summarySubscriptions += 1
      emitAll(summaryValues.filterNotNull())
    }

    override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
        error("Category history reads are not expected in this fixture")

    override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
      insertCalls += 1
      return DataOutcome.Success(Unit)
    }

    override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(emptyList())

    override suspend fun getById(id: String): DataOutcome<Movimiento?> = DataOutcome.Success(null)

    override suspend fun update(
        id: String,
        changes: MovimientoChanges,
    ): DataOutcome<MovimientoUpdateResult> = DataOutcome.Success(MovimientoUpdateResult.NotFound)

    override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> =
        DataOutcome.Success(null)

    override suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult> =
        DataOutcome.Failure(
            DataError.InvalidInput("This Home fixture has no persistent movimiento store")
        )

    fun emitRecent(value: DataOutcome<List<Movimiento>>) {
      recentValues.value = value
    }

    fun emitSummary(value: DataOutcome<MovimientoSummary>) {
      summaryValues.value = value
    }

    fun awaitNextSummary() {
      summaryValues.value = null
    }

    fun invalidateAfterInsert(movimiento: Movimiento) {
      recentValues.value = DataOutcome.Success(listOf(movimiento))
      summaryValues.value = DataOutcome.Success(summary(movimiento.monto.moneda))
    }
  }

  private class FakeCategoriaRepository(initial: DataOutcome<List<Categoria>>? = null) :
      CategoriaRepository by UnsupportedCategoriaRepository {
    private val values = MutableStateFlow(initial)
    var subscriptionCount = 0
      private set

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = flow {
      subscriptionCount += 1
      emitAll(values.filterNotNull())
    }

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

    override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    fun emit(value: DataOutcome<List<Categoria>>) {
      values.value = value
    }
  }

  private companion object {
    val OCTOBER = MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
    val OCTOBER_24 = LocalDate(2026, 10, 24)

    /**
     * October up to the 24th in COP: every gasto falls on the 1st, so the pace projects the month
     * at 31/24 of it.
     */
    fun monthSnapshot(
        moneda: Moneda = Moneda.COP,
        categorias: List<Pair<String, Long>>,
        previousMonthGastos: Long = 0,
    ): StatisticsSnapshot {
      val gastos = Money(categorias.sumOf { it.second }, moneda)
      val daily =
          (1..OCTOBER_24.day).map { day ->
            DayGasto(LocalDate(2026, 10, day), if (day == 1) gastos else Money(0, moneda))
          }
      return StatisticsSnapshot(
          current = MovimientoSummary(Money(0, moneda), gastos),
          previous = null,
          categorias =
              categorias.map { (id, amount) ->
                DimensionTotal(StatisticsKey.Categoria(id), Money(amount, moneda), null)
              },
          etiquetas = emptyList(),
          cuentas = emptyList(),
          medios = emptyList(),
          months =
              listOf(
                  MonthTotals(
                      YearMonth(2026, 9),
                      MovimientoSummary(Money(0, moneda), Money(previousMonthGastos, moneda)),
                  ),
                  MonthTotals(YearMonth(2026, 10), MovimientoSummary(Money(0, moneda), gastos)),
              ),
          daily = daily,
          hasAnyMovimiento = categorias.isNotEmpty(),
      )
    }

    fun today(at: MutableClock = MutableClock(Instant.parse("2026-10-24T12:00:00Z"))) =
        CivilToday(at, TimeZoneProvider { TimeZone.UTC })

    /** Every currency the tests read has one live account, so totals are never missing. */
    fun primarySaldos(vararg saldos: Money): FakeSaldoRepository =
        FakeSaldoRepository(
            Moneda.entries.map { moneda ->
              CuentaSaldo(
                  Cuenta(
                      id = Cuenta.defaultCashId(moneda),
                      nombre = "Efectivo",
                      tipo = TipoCuenta.EFECTIVO,
                      saldoInicial = saldos.firstOrNull { it.moneda == moneda } ?: Money(0, moneda),
                      diaCorte = null,
                      archivada = false,
                      createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                  ),
                  saldos.firstOrNull { it.moneda == moneda } ?: Money(0, moneda),
              )
            }
        )

    fun readyPreferences(moneda: Moneda) =
        DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(moneda)))

    fun summary(moneda: Moneda) =
        MovimientoSummary(ingresos = Money(1_000, moneda), gastos = Money(250, moneda))

    fun categoria(id: String) =
        Categoria(
            id = id,
            nombre = "Food",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )

    fun movimiento(id: String, moneda: Moneda, categoriaId: String) =
        Movimiento(
            id = id,
            tipo = TipoMovimiento.GASTO,
            monto = Money(100, moneda),
            fecha = LocalDate(2026, 9, 15),
            categoriaId = categoriaId,
            titulo = null,
            descripcion = null,
            createdAt = Instant.parse("2026-09-15T00:00:00Z"),
            cuentaId = Cuenta.defaultCashId(moneda),
            medio = MedioPago.EFECTIVO,
        )
  }
}
