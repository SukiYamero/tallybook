package com.kurobello.tallybook.feature.home

import androidx.lifecycle.ViewModelStore
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

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
    val vm = HomeViewModel(preferences, movimientos, categorias, backgroundScope)
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
            preferences,
            movimientos,
            FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
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

  private fun createViewModel(
      preferences: AppPreferencesRepository,
      movimientos: MovimientoRepository = FakeMovimientoRepository(),
      categorias: CategoriaRepository = FakeCategoriaRepository(),
      scope: CoroutineScope,
  ): HomeViewModel {
    val viewModel =
        HomeViewModel(
            preferencesRepository = preferences,
            movimientoRepository = movimientos,
            categoriaRepository = categorias,
            scope = scope,
        )
    scope.launch { viewModel.uiState.collect {} }
    return viewModel
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

    override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> =
        error("Home recent reads must include principal currency")

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

    override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = flow {
      summarySubscriptions += 1
      emitAll(summaryValues.filterNotNull())
    }

    override fun observeFiltered(
        criteria: MovimientoReadCriteria
    ): Flow<DataOutcome<MovimientoSearchSnapshot>> =
        error("Search reads are not expected in this fixture")

    override fun observeRange(
        moneda: Moneda,
        range: MovimientoDateRange,
    ): Flow<DataOutcome<List<Movimiento>>> =
        error("History range reads are not expected in this fixture")

    override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
        error("History year reads are not expected in this fixture")

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

    override suspend fun restoreDeleted(
        movimiento: Movimiento
    ): DataOutcome<MovimientoRestoreResult> =
        DataOutcome.Failure(
            DataError.InvalidInput("This Home fixture has no persistent movimiento store")
        )

    override suspend fun delete(id: String): DataOutcome<Unit> = DataOutcome.Success(Unit)

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
