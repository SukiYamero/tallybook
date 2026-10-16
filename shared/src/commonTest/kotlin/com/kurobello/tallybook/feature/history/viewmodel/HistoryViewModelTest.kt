package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
  @Test
  fun exposesCompleteSelectedPeriodIdentityAlongsideBoundedReadRange() {
    val month = HistoryUiState(HistorySelection(HistoryScope.Month, TODAY), TODAY)
    val fullMonth = MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))

    assertEquals(fullMonth, month.selectedPeriod)
    assertEquals(MovimientoDateRange(LocalDate(2026, 10, 1), TODAY), month.range)
    assertEquals(fullMonth, month.periods.last())
    assertEquals(10, month.periods.size)

    val week = month.copy(selection = HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 24)))
    assertEquals(
        MovimientoDateRange(LocalDate(2024, 2, 22), LocalDate(2024, 2, 28)),
        week.selectedPeriod,
    )
    assertEquals(5, week.periods.size)

    val day = week.copy(selection = week.selection.selectScope(HistoryScope.Day))
    val selectedDay = LocalDate(2024, 2, 24)
    assertEquals(MovimientoDateRange(selectedDay, selectedDay), day.selectedPeriod)
    assertEquals(29, day.periods.size)
  }

  @Test
  fun startsOneReadGraphOnlyWhilePublicConsumersExist() = runTest {
    val fixture = HistoryFixture(this, listOf(movimiento("today", TODAY)))
    val vm = fixture.create()
    runCurrent()
    assertEquals(0, fixture.preferences.source.starts)
    val first = backgroundScope.launch { vm.uiState.collect {} }
    val second = backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val content =
        assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    assertEquals(1, content.value.movementCount)
    assertEquals(Money(-100, Moneda.USD), content.value.summary.balance)
    assertEquals(1, fixture.preferences.source.starts)
    assertEquals(0, fixture.movimientos.summary(Moneda.USD).starts)
    assertEquals(1, fixture.movimientos.activeRanges)
    first.cancel()
    runCurrent()
    assertEquals(1, fixture.movimientos.activeRanges)
    second.cancel()
  }

  @Test
  fun retainsHistoricalControlsButNeverReplaysOldSuccessAfterTimeout() = runTest {
    val fixture = HistoryFixture(this, listOf(movimiento("past", LocalDate(2024, 2, 22))))
    val vm = fixture.create()
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onYearSelected(2024)
    vm.onScopeChanged(HistoryScope.Month)
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2024, 2, 1), LocalDate(2024, 2, 29)))
    vm.onScopeChanged(HistoryScope.Week)
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2024, 2, 22), LocalDate(2024, 2, 28)))
    runCurrent()
    assertEquals(
        HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)),
        vm.uiState.value.selection,
    )
    collector.cancel()
    runCurrent()
    advanceTimeBy(4_999)
    runCurrent()
    assertEquals(1, fixture.movimientos.activeRanges)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(0, fixture.movimientos.activeRanges)
    assertEquals(0, fixture.preferences.source.active)
    assertEquals(0, fixture.categorias.source.active)
    assertEquals(0, fixture.movimientos.activeSummaries)
    assertEquals(0, fixture.movimientos.activeYears)
    fixture.movimientos.insert(movimiento("hidden", LocalDate(2024, 2, 23)))
    var firstState: HistoryUiState? = null
    backgroundScope.launch { vm.uiState.take(1).collect { firstState = it } }
    runCurrent()
    val first = requireNotNull(firstState)
    assertEquals(HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)), first.selection)
    assertIs<RetainedLoadState.Loading<HistoryProjection.Content>>(first.period)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val content =
        assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    assertEquals(2, content.value.movementCount)
  }

  @Test
  fun reportsEarlyReadFailuresAndRecoversPeriodContent() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val fixture = HistoryFixture(this, listOf(movimiento("today", TODAY)))
    fixture.categorias.source.emit(null)
    fixture.movimientos
        .range(Moneda.USD, MovimientoDateRange(TODAY, TODAY))
        .emit(DataOutcome.Failure(failure))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(
        HistoryFailure.Repository(failure),
        assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.period).failure,
    )
    fixture.categorias.source.emit(DataOutcome.Success(emptyList()))
    fixture.movimientos
        .range(Moneda.USD, MovimientoDateRange(TODAY, TODAY))
        .emit(DataOutcome.Success(listOf(movimiento("today", TODAY))))
    fixture.movimientos.summary(Moneda.USD).emit(DataOutcome.Failure(failure))
    runCurrent()
    assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    assertEquals(0, fixture.movimientos.summary(Moneda.USD).starts)
  }

  @Test
  fun changesFallbackWithoutSqlRestartAndKeepsSelectedYearsAfterDeletion() = runTest {
    val fixture = HistoryFixture(this, listOf(movimiento("past", LocalDate(2024, 10, 5))))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onYearSelected(2024)
    runCurrent()
    val starts = fixture.movimientos.totalRangeStarts
    vm.onPresentationChanged("Sin categoría")
    assertTrue(vm.uiState.value.period.hasSnapshot)
    runCurrent()
    val content =
        assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    assertEquals("Sin categoría", content.value.groups.single().rows.single().title)
    assertEquals(starts, fixture.movimientos.totalRangeStarts)
    fixture.movimientos.years(Moneda.USD).emit(DataOutcome.Success(emptyList()))
    vm.onYearSelected(2026)
    runCurrent()
    assertTrue(2024 in vm.uiState.value.availableYears)
    vm.onYearSelected(2024)
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2025, 1, 1), LocalDate(2025, 1, 7)))
    vm.onYearSelected(999_999)
    assertEquals(2024, vm.uiState.value.selection.anchor.year)
  }

  @Test
  fun cancelsOldRangesAndCurrencyWhileKeepingMetadataStableAcrossPeriods() = runTest {
    val prior = LocalDate(2026, 10, 4)
    val fixture =
        HistoryFixture(
            this,
            listOf(
                movimiento("usd-today", TODAY),
                movimiento("usd-prior", prior),
                movimiento("cop-today", TODAY, Moneda.COP),
            ),
        )
    val vm = fixture.create()
    val states = mutableListOf<HistoryUiState>()
    backgroundScope.launch { vm.uiState.collect { states.add(it) } }
    runCurrent()
    vm.onScopeChanged(HistoryScope.Week)
    runCurrent()
    val week = MovimientoDateRange(LocalDate(2026, 10, 1), TODAY)
    assertEquals(
        2,
        assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
            .value
            .movementCount,
    )
    assertEquals(0, fixture.movimientos.summary(Moneda.USD).starts)
    assertEquals(1, fixture.movimientos.years(Moneda.USD).starts)
    assertEquals(1, fixture.categorias.source.starts)
    val afterSelection = states.size
    vm.onScopeChanged(HistoryScope.Day)
    fixture.movimientos
        .range(Moneda.USD, week)
        .emit(DataOutcome.Success(listOf(movimiento("late-week", prior))))
    runCurrent()
    assertEquals(0, fixture.movimientos.range(Moneda.USD, week).active)
    assertTrue(
        states.drop(afterSelection).all { state ->
          val period = state.period
          period !is RetainedLoadState.Ready || period.value.groups.all { it.fecha == TODAY }
        }
    )
    fixture.preferences.setMonedaPrincipal(Moneda.COP)
    fixture.movimientos
        .range(Moneda.USD, MovimientoDateRange(TODAY, TODAY))
        .emit(DataOutcome.Success(listOf(movimiento("late-usd", TODAY))))
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.moneda)
    val content =
        assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    assertEquals("cop-today", content.value.groups.single().rows.single().id)
    assertTrue(
        states
            .filter { it.moneda == Moneda.COP }
            .all { state ->
              state.period !is RetainedLoadState.Ready ||
                  state.period.value.summary.balance.moneda == Moneda.COP
            }
    )
  }

  @Test
  fun retryPreservesInputsAndSubtotalOverflowNeverBecomesAnEmptyPeriod() = runTest {
    val date = LocalDate(2024, 10, 5)
    val fixture = HistoryFixture(this, listOf(movimiento("past", date)))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onYearSelected(2024)
    runCurrent()
    val source = fixture.movimientos.range(Moneda.USD, MovimientoDateRange(date, date))
    source.emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()
    assertIs<RetainedLoadState.Failed<HistoryProjection.Content, HistoryFailure>>(
        vm.uiState.value.period
    )
    source.emit(DataOutcome.Success(listOf(movimiento("past", date))))
    vm.onRetry()
    assertEquals(HistorySelection(HistoryScope.Day, date), vm.uiState.value.selection)
    runCurrent()
    assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    source.emit(
        DataOutcome.Success(
            listOf(
                movimiento("max", date, amount = Long.MAX_VALUE),
                movimiento("extra", date, amount = 1),
            )
        )
    )
    fixture.movimientos
        .summary(Moneda.USD)
        .emit(DataOutcome.Failure(DataError.Local(ArithmeticException())))
    runCurrent()
    assertEquals(
        HistoryFailure.CalculationOverflow,
        assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.period).failure,
    )
    assertFalse(vm.uiState.value.isEmpty)
    assertEquals(HistorySelection(HistoryScope.Day, date), vm.uiState.value.selection)
  }

  @Test
  fun startupCategoryAndYearFailuresAreVisibleBeforeSilentPeers() = runTest {
    val failure = DataError.Local(IllegalStateException())
    val categoryFixture = HistoryFixture(this)
    categoryFixture.movimientos.range(Moneda.USD, MovimientoDateRange(TODAY, TODAY)).emit(null)
    categoryFixture.movimientos.summary(Moneda.USD).emit(null)
    categoryFixture.categorias.source.emit(DataOutcome.Failure(failure))
    val categoryVm = categoryFixture.create()
    backgroundScope.launch { categoryVm.uiState.collect {} }
    runCurrent()
    assertEquals(
        HistoryFailure.Repository(failure),
        assertIs<RetainedLoadState.Failed<*, *>>(categoryVm.uiState.value.period).failure,
    )

    val yearsFixture = HistoryFixture(this)
    yearsFixture.movimientos.range(Moneda.USD, MovimientoDateRange(TODAY, TODAY)).emit(null)
    yearsFixture.categorias.source.emit(null)
    yearsFixture.movimientos.years(Moneda.USD).emit(DataOutcome.Failure(failure))
    val yearsVm = yearsFixture.create()
    backgroundScope.launch { yearsVm.uiState.collect {} }
    runCurrent()
    assertEquals(failure, yearsVm.uiState.value.years.failure)
    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
        yearsVm.uiState.value.availableYears,
    )

    val startupFixture = HistoryFixture(this)
    startupFixture.preferences.source.emit(DataOutcome.Success(AppPreferencesState.Uninitialized))
    val startupVm = startupFixture.create()
    backgroundScope.launch { startupVm.uiState.collect {} }
    runCurrent()
    assertIs<DataError.InvalidInput>(startupVm.uiState.value.preferenceFailure)
    assertNull(startupVm.uiState.value.moneda)
    assertEquals(0, startupFixture.movimientos.activeRanges)
  }

  @Test
  fun marksReplayStaleBeforeWaitingForSlowReadCancellation() = runTest {
    val fixture = HistoryFixture(this, listOf(movimiento("today", TODAY)))
    val source = fixture.movimientos.range(Moneda.USD, MovimientoDateRange(TODAY, TODAY))
    source.closeDelayMillis = 1_000
    val vm = fixture.create()
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(vm.uiState.value.period)
    collector.cancel()
    runCurrent()
    advanceTimeBy(5_000)
    runCurrent()
    assertEquals(1, source.active)
    assertIs<RetainedLoadState.Loading<HistoryProjection.Content>>(vm.uiState.value.period)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(0, source.active)
    assertIs<RetainedLoadState.Loading<HistoryProjection.Content>>(vm.uiState.value.period)
  }

  @Test
  fun resumeUsesCivilTimezoneAndPreservesHistoricalAnchorAcrossYearRollover() = runTest {
    val fixture = HistoryFixture(this, listOf(movimiento("past", LocalDate(2024, 10, 5))))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    fixture.clock.instant = Instant.parse("2026-10-06T00:30:00Z")
    fixture.zone = TimeZone.of("America/Bogota")
    vm.onResume()
    runCurrent()
    assertEquals(TODAY, vm.uiState.value.today)
    fixture.zone = TimeZone.UTC
    vm.onResume()
    runCurrent()
    assertEquals(LocalDate(2026, 10, 6), vm.uiState.value.selection.anchor)
    vm.onYearSelected(2024)
    vm.onPeriodSelected(MovimientoDateRange(LocalDate(2024, 10, 5), LocalDate(2024, 10, 5)))
    fixture.clock.instant = Instant.parse("2027-01-01T12:00:00Z")
    vm.onResume()
    runCurrent()
    assertEquals(LocalDate(2024, 10, 5), vm.uiState.value.selection.anchor)
    assertTrue(2027 in vm.uiState.value.availableYears)
    assertTrue(2024 in vm.uiState.value.availableYears)
  }

  @Test
  fun refreshesOnceAndClearsOnlyOwnedProducerFromInjectedScope() = runTest {
    val fixture = HistoryFixture(this)
    val vm = fixture.create()
    val sibling = backgroundScope.launch { awaitCancellation() }
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val clockReads = fixture.clock.reads
    val zoneReads = fixture.zoneReads
    fixture.clock.instant = Instant.parse("2026-10-06T12:00:00Z")
    vm.onResume()
    runCurrent()
    assertEquals(LocalDate(2026, 10, 6), vm.uiState.value.selection.anchor)
    assertEquals(clockReads + 1, fixture.clock.reads)
    assertEquals(zoneReads + 1, fixture.zoneReads)
    ViewModelStore().apply {
      put("history", vm)
      clear()
    }
    runCurrent()
    assertEquals(0, fixture.movimientos.activeRanges)
    assertEquals(0, fixture.preferences.source.active)
    assertTrue(sibling.isActive)
  }
}

private val TODAY = LocalDate(2026, 10, 5)

@OptIn(ExperimentalCoroutinesApi::class)
private class HistoryFixture(private val scope: TestScope, rows: List<Movimiento> = emptyList()) {
  val preferences = HistoryPreferences()
  val movimientos = HistoryMovimientos(rows)
  val categorias = HistoryCategorias()
  val clock = HistoryClock()
  var zoneReads = 0
  var zone: TimeZone = TimeZone.UTC

  fun create(handle: SavedStateHandle = SavedStateHandle()): HistoryViewModel =
      HistoryViewModel(
          HistoryRepositories(preferences, movimientos, categorias),
          handle,
          "Missing category",
          HistoryDateProvider(
              clock,
              TimeZoneProvider {
                zoneReads += 1
                zone
              },
          ),
          StandardTestDispatcher(scope.testScheduler),
          scope.backgroundScope,
      )
}

private class HistoryClock(var instant: Instant = Instant.parse("2026-10-05T12:00:00Z")) : Clock {
  var reads = 0

  override fun now(): Instant {
    reads += 1
    return instant
  }
}

private class HistorySource<T>(initial: DataOutcome<T>?) {
  private val state = MutableStateFlow(initial)
  var starts = 0
  var active = 0
  var closeDelayMillis = 0L

  fun emit(value: DataOutcome<T>?) {
    state.value = value
  }

  fun observe(): Flow<DataOutcome<T>> = flow {
    starts += 1
    active += 1
    try {
      emitAll(state.filterNotNull())
    } finally {
      if (closeDelayMillis > 0) withContext(NonCancellable) { delay(closeDelayMillis) }
      active -= 1
    }
  }
}

private class HistoryPreferences : AppPreferencesRepository {
  val source =
      HistorySource<AppPreferencesState>(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
      )
  override val preferences
    get() = source.observe()

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> {
    source.emit(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(moneda))))
    return DataOutcome.Success(Unit)
  }
}

private class HistoryCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  val source = HistorySource<List<Categoria>>(DataOutcome.Success(emptyList()))

  override fun observeAll() = source.observe()

  override suspend fun getAll() = DataOutcome.Success(emptyList<Categoria>())

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>) = DataOutcome.Success(Unit)
}

private data class ReadKey(val moneda: Moneda, val range: MovimientoDateRange)

private class HistoryMovimientos(private var rows: List<Movimiento>) : MovimientoRepository {
  private val ranges = mutableMapOf<ReadKey, HistorySource<List<Movimiento>>>()
  private val summaries = mutableMapOf<Moneda, HistorySource<MovimientoSummary>>()
  private val metadata = mutableMapOf<Moneda, HistorySource<List<Int>>>()
  val activeRanges
    get() = ranges.values.sumOf { it.active }

  val activeSummaries
    get() = summaries.values.sumOf { it.active }

  val activeYears
    get() = metadata.values.sumOf { it.active }

  val totalRangeStarts
    get() = ranges.values.sumOf { it.starts }

  fun range(moneda: Moneda, range: MovimientoDateRange) =
      ranges.getOrPut(ReadKey(moneda, range)) {
        HistorySource(DataOutcome.Success(filtered(moneda, range)))
      }

  fun summary(moneda: Moneda) =
      summaries.getOrPut(moneda) { HistorySource(DataOutcome.Success(summaryFor(moneda))) }

  fun years(moneda: Moneda) =
      metadata.getOrPut(moneda) {
        HistorySource(
            DataOutcome.Success(
                rows.filter { it.monto.moneda == moneda }.map { it.fecha.year }.distinct()
            )
        )
      }

  private fun filtered(moneda: Moneda, range: MovimientoDateRange) =
      rows
          .filter { it.monto.moneda == moneda && it.fecha >= range.start && it.fecha <= range.end }
          .sortedByDescending { it.fecha }

  private fun summaryFor(moneda: Moneda) =
      MovimientoSummary(
          Money(
              rows
                  .filter { it.monto.moneda == moneda && it.tipo == TipoMovimiento.INGRESO }
                  .sumOf { it.monto.minorUnits },
              moneda,
          ),
          Money(
              rows
                  .filter { it.monto.moneda == moneda && it.tipo == TipoMovimiento.GASTO }
                  .sumOf { it.monto.minorUnits },
              moneda,
          ),
      )

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> =
      error("Search reads are not expected in this fixture")

  override fun observeRange(moneda: Moneda, range: MovimientoDateRange) =
      range(moneda, range).observe()

  override fun observeYears(moneda: Moneda) = years(moneda).observe()

  override fun observeSummary(moneda: Moneda) = summary(moneda).observe()

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Scoped recent reads are not expected in this fixture")

  override fun observeRecent(limit: Int) = flow { emit(DataOutcome.Success(rows.take(limit))) }

  override suspend fun getAll() = DataOutcome.Success(rows)

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      error("Category history reads are not expected in this fixture")

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    rows = rows + movimiento
    publishRows()
    return DataOutcome.Success(Unit)
  }

  private fun publishRows() {
    ranges.forEach { (key, source) ->
      source.emit(DataOutcome.Success(filtered(key.moneda, key.range)))
    }
    summaries.forEach { (moneda, source) -> source.emit(DataOutcome.Success(summaryFor(moneda))) }
    metadata.forEach { (moneda, source) ->
      source.emit(
          DataOutcome.Success(
              rows.filter { it.monto.moneda == moneda }.map { it.fecha.year }.distinct()
          )
      )
    }
  }

  override suspend fun getById(id: String): DataOutcome<Movimiento?> =
      DataOutcome.Success(rows.firstOrNull { it.id == id })

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    val current =
        rows.firstOrNull { it.id == id }
            ?: return DataOutcome.Success(MovimientoUpdateResult.NotFound)
    if (current.monto.moneda != changes.monto.moneda)
        return DataOutcome.Failure(DataError.InvalidInput("Currency cannot change"))
    val updated =
        current.copy(
            tipo = changes.tipo,
            monto = changes.monto,
            fecha = changes.fecha,
            categoriaIds = changes.categoriaIds,
            descripcion = changes.descripcion,
        )
    val values = rows.map { if (it.id == id) updated else it }
    rows = values
    publishRows()
    return DataOutcome.Success(MovimientoUpdateResult.Updated)
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    val captured = rows.firstOrNull { it.id == id }
    val values = rows.filter { it.id != id }
    rows = values
    publishRows()
    return DataOutcome.Success(captured)
  }

  override suspend fun restoreDeleted(
      movimiento: Movimiento
  ): DataOutcome<MovimientoRestoreResult> {
    val current = rows.firstOrNull { it.id == movimiento.id }
    if (current != null)
        return DataOutcome.Success(
            if (current == movimiento) MovimientoRestoreResult.AlreadyPresent
            else MovimientoRestoreResult.Conflict
        )
    val values = rows + movimiento
    rows = values
    publishRows()
    return DataOutcome.Success(MovimientoRestoreResult.Restored)
  }

  override suspend fun delete(id: String) = DataOutcome.Success(Unit)
}

private fun movimiento(
    id: String,
    fecha: LocalDate,
    moneda: Moneda = Moneda.USD,
    amount: Long = 100,
) =
    Movimiento(
        id,
        TipoMovimiento.GASTO,
        Money(amount, moneda),
        fecha,
        listOf("missing"),
        null,
        Instant.parse("2026-10-05T00:00:00Z"),
    )
