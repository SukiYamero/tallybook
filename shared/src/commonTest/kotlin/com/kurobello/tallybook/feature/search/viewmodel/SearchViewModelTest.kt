package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
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
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
  @Test
  fun rapidTextRoundtripCannotLeaveSettledResultsPending() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    for (query in listOf("c", "ca", "caf", "cafe")) vm.onQueryChanged(query)
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun rapidRoundtripToPendingTextResetsItsLastEditDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(200)
    vm.onQueryChanged("other")
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun textTimerBecomesOnlyPendingWorkAfterFirstCandidatesArePrepared() = runTest {
    val fixture = SearchFixture(this)
    fixture.movimientos.source.value = null
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(150)
    runCurrent()
    assertFalse(vm.uiState.value.work == SearchWork.WaitingText)
    assertNull(vm.uiState.value.displayResults)
    fixture.movimientos.restore()
    runCurrent()
    assertEquals(SearchWork.WaitingText, vm.uiState.value.work)
    assertNull(vm.uiState.value.currentCount)
    assertNull(vm.uiState.value.displayResults)
    advanceTimeBy(99)
    runCurrent()
    assertEquals(SearchWork.WaitingText, vm.uiState.value.work)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun debouncesAt250MillisecondsAndClearIsImmediateWithoutSqlRestarts() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged("café")
    assertNull(vm.uiState.value.currentCount)
    assertEquals(2, vm.uiState.value.displayResults?.count)
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onQueryChanged(" CAFE ")
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onQueryChanged("not found")
    runCurrent()
    vm.onClearQuery()
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    advanceTimeBy(500)
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun draftEditsAreIsolatedAndApplyingPreservesPendingTextDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(100)
    val id = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(id, vm.uiState.value.filters.copy(tipo = TipoMovimiento.GASTO))
    runCurrent()
    assertEquals(1, fixture.movimientos.starts)
    assertNull(vm.uiState.value.filters.tipo)
    vm.onApplyFilters(id)
    assertNull(vm.uiState.value.displayResults)
    runCurrent()
    assertEquals(2, fixture.movimientos.starts)
    advanceTimeBy(149)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    assertTrue(vm.hasActiveFilterSession)
    vm.onFiltersHidden(id)
    val nextId = requireNotNull(vm.onOpenFilters())
    vm.onFiltersHidden(id)
    assertTrue(vm.hasActiveFilterSession)
    vm.onApplyFilters(nextId)
    runCurrent()
    assertEquals(2, fixture.movimientos.starts)
  }

  @Test
  fun clearFiltersCommitsDefaultsClosesAndKeepsTheTextDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val first = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(
        first,
        vm.uiState.value.filters.copy(
            preset = SearchDatePreset.Last7Days,
            tipo = TipoMovimiento.INGRESO,
            categoryIds = setOf("selected"),
        ),
    )
    vm.onApplyFilters(first)
    vm.onFiltersHidden(first)
    runCurrent()
    val id = requireNotNull(vm.onOpenFilters())
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(100)
    fixture.clock.instant = Instant.parse("2026-10-07T12:00:00Z")
    vm.onClearFilters(id)
    val defaults = SearchFilters.initial(LocalDate(2026, 10, 7))
    assertEquals(defaults, vm.uiState.value.filters)
    assertEquals(
        defaults,
        assertIs<SearchFilterSession.Closing>(vm.uiState.value.filterSession).renderSnapshot,
    )
    assertEquals("cafe", vm.uiState.value.rawQuery)
    assertTrue(vm.hasActiveFilterSession)
    vm.onApplyFilters(id)
    vm.onClearFilters(id)
    runCurrent()
    assertEquals(3, fixture.movimientos.starts)
    advanceTimeBy(149)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onFiltersHidden(id)
    val nextId = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(nextId, defaults.copy(tipo = TipoMovimiento.GASTO))
    val currentSession = vm.uiState.value.filterSession
    vm.onClearFilters(id)
    assertEquals(currentSession, vm.uiState.value.filterSession)
    assertEquals(defaults, vm.uiState.value.filters)
  }

  @Test
  fun categoryFailureSurfacesWhilePreferencesAndMovementsAreSilent() = runTest {
    val fixture = SearchFixture(this)
    fixture.preferences.source.value = null
    fixture.categorias.source.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.results)
    assertEquals(0, fixture.movimientos.starts)
    assertFalse(vm.uiState.value.canOpenFilters)
  }

  @Test
  fun distinguishesNoCurrencyDataFromFilteredEmptyAndRetainsMetadataForCorrection() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val id = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(
        id,
        vm.uiState.value.filters.copy(
            preset = SearchDatePreset.Custom,
            customStart = TODAY,
            customEnd = TODAY,
        ),
    )
    vm.onApplyFilters(id)
    vm.onFiltersHidden(id)
    runCurrent()
    fixture.clock.instant = Instant.parse("2026-10-05T12:00:00Z")
    val starts = fixture.movimientos.starts
    vm.onResume()
    runCurrent()
    assertIs<SearchFailure.InvalidDates>(vm.uiState.value.results.failure)
    assertNull(vm.uiState.value.displayResults)
    assertEquals(starts, fixture.movimientos.starts)
    assertEquals(listOf(2026), vm.uiState.value.years?.persistedYears)
    assertTrue(vm.uiState.value.canOpenFilters)
    assertNotNull(vm.onOpenFilters())
  }

  @Test
  fun categoryRenameRebuildsMatchesWithoutSqlAndWhitespaceTextSettlesImmediately() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("snacks")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals(0, vm.uiState.value.currentCount)
    fixture.categorias.source.value =
        DataOutcome.Success(
            listOf(Categoria("food", "Snacks", null, CategoryIcon.UTENSILS, CategoryTint.AMBER))
        )
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged(" \t ")
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun sourceChangesInvalidateHeldMatchingAndDoNotPublishOldLabels() = runTest {
    val fixture = SearchFixture(this)
    val computation = HoldingDispatcher()
    val vm = fixture.create(computation)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    repeat(4) {
      computation.releaseAll()
      runCurrent()
    }
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged("snacks")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    fixture.categorias.source.value =
        DataOutcome.Success(
            listOf(Categoria("food", "Snacks", null, CategoryIcon.UTENSILS, CategoryTint.AMBER))
        )
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    repeat(4) {
      computation.releaseAll()
      runCurrent()
    }
    assertEquals(2, vm.uiState.value.currentCount)
    assertTrue(vm.uiState.value.displayResults!!.rows.all { it.categoryLabel == "Snacks" })
  }

  @Test
  fun readFailureKeepsOnlySameTextAndRetryPreservesAppliedInput() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    fixture.movimientos.source.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    runCurrent()
    assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.results)
    assertEquals(2, vm.uiState.value.displayResults?.count)
    vm.onQueryChanged("cafe")
    assertNull(vm.uiState.value.displayResults)
    runCurrent()
    fixture.movimientos.restore()
    vm.onRetry()
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals("cafe", vm.uiState.value.rawQuery)
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun coherentYearsDetermineNoDataAndCurrencyChangesDiscardOldMetadata() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), listOf(1900)))
    runCurrent()
    assertEquals(0, vm.uiState.value.currentCount)
    assertTrue(vm.uiState.value.displayResults!!.years.hasCurrencyData)
    assertTrue(1900 in vm.uiState.value.availableYears)
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList()))
    runCurrent()
    assertFalse(vm.uiState.value.displayResults!!.years.hasCurrencyData)
    fixture.preferences.source.value =
        DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP)))
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.years?.moneda)
    assertFalse(vm.uiState.value.availableYears.contains(1900))
  }

  @Test
  fun editingSessionKeepsEarliestYearUntilItsMatchingHiddenCallback() = runTest {
    val fixture = SearchFixture(this)
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), listOf(1900)))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val session = requireNotNull(vm.onOpenFilters())
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList()))
    runCurrent()
    assertTrue(1900 in vm.uiState.value.availableYears)
    vm.onCancelFilters(session)
    assertTrue(1900 in vm.uiState.value.availableYears)
    vm.onFiltersHidden(session)
    assertFalse(1900 in vm.uiState.value.availableYears)
  }

  @Test
  fun listenersStopAtFiveSecondsAndOwnerClearKeepsInjectedSiblingAlive() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    runCurrent()
    assertEquals(0, fixture.movimientos.starts)
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    val sibling = backgroundScope.launch { awaitCancellation() }
    runCurrent()
    collector.cancel()
    runCurrent()
    advanceTimeBy(4999)
    runCurrent()
    assertEquals(1, fixture.movimientos.active)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(0, fixture.movimientos.active)
    assertNull(vm.uiState.value.currentCount)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    ViewModelStore().apply {
      put("search", vm)
      clear()
    }
    runCurrent()
    assertEquals(0, fixture.movimientos.active)
    assertTrue(sibling.isActive)
  }
}

private val TODAY = LocalDate(2026, 10, 6)

private class SearchFixture(private val scope: TestScope) {
  val preferences = SearchPreferences()
  val categorias = SearchCategorias()
  val movimientos = SearchMovimientos()
  val clock = SearchClock()

  fun create(dispatcher: CoroutineDispatcher = StandardTestDispatcher(scope.testScheduler)) =
      SearchViewModel(
          SearchRepositories(preferences, movimientos, categorias),
          SavedStateHandle(),
          "Unknown",
          SearchDateProvider(clock, TimeZoneProvider { TimeZone.UTC }),
          dispatcher,
          scope.backgroundScope,
      )
}

private class SearchClock(var instant: Instant = Instant.parse("2026-10-06T12:00:00Z")) : Clock {
  override fun now() = instant
}

private class SearchPreferences : AppPreferencesRepository {
  val source =
      MutableStateFlow<DataOutcome<AppPreferencesState>?>(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
      )
  override val preferences = source.filterNotNull()

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) = DataOutcome.Success(Unit)
}

private class SearchCategorias : CategoriaRepository {
  val source = MutableStateFlow<DataOutcome<List<Categoria>>?>(DataOutcome.Success(emptyList()))

  override fun observeAll() = source.filterNotNull()

  override suspend fun getAll() = DataOutcome.Success(emptyList<Categoria>())

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>) = DataOutcome.Success(Unit)
}

private class SearchMovimientos : MovimientoRepository {
  var starts = 0
  var active = 0
  private var rows =
      listOf("Café", "Lunch").mapIndexed { index, text ->
        Movimiento(
            "$index",
            TipoMovimiento.GASTO,
            Money(100, Moneda.USD),
            TODAY,
            listOf("food"),
            text,
            Instant.parse("2026-10-06T12:00:00Z"),
        )
      }
  val source =
      MutableStateFlow<DataOutcome<MovimientoSearchSnapshot>?>(
          DataOutcome.Success(MovimientoSearchSnapshot(rows, listOf(2026)))
      )

  fun restore() {
    source.value = DataOutcome.Success(MovimientoSearchSnapshot(rows, listOf(2026)))
  }

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = flow {
    starts += 1
    active += 1
    try {
      emitAll(
          source.filterNotNull().map { result ->
            result.map { snapshot ->
              snapshot.copy(
                  movimientos =
                      snapshot.movimientos.filter {
                        it.monto.moneda == criteria.moneda &&
                            (criteria.range?.let { range ->
                              it.fecha >= range.start && it.fecha <= range.end
                            } != false) &&
                            (criteria.tipo == null || it.tipo == criteria.tipo) &&
                            (criteria.categoryIds.isEmpty() ||
                                it.categoriaIds.any { id -> id in criteria.categoryIds })
                      }
              )
            }
          }
      )
    } finally {
      active -= 1
    }
  }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Unexpected read")

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Unexpected read")

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> =
      error("Unexpected read")

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = error("Unexpected read")

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = error("Unexpected read")

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      error("Unexpected read")

  override suspend fun insert(movimiento: Movimiento) = DataOutcome.Success(Unit)

  override suspend fun getAll() = DataOutcome.Success(emptyList<Movimiento>())

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
    restore()
    return DataOutcome.Success(MovimientoUpdateResult.Updated)
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    val captured = rows.firstOrNull { it.id == id }
    val values = rows.filter { it.id != id }
    rows = values
    restore()
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
    restore()
    return DataOutcome.Success(MovimientoRestoreResult.Restored)
  }

  override suspend fun delete(id: String) = DataOutcome.Success(Unit)
}

private class HoldingDispatcher : CoroutineDispatcher() {
  private val pending = mutableListOf<Runnable>()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    pending.add(block)
  }

  fun releaseAll() {
    while (pending.isNotEmpty()) pending.removeAt(0).run()
  }
}
