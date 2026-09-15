package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
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
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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

  private fun createViewModel(
      preferences: AppPreferencesRepository,
      movimientos: MovimientoRepository = FakeMovimientoRepository(),
      categorias: CategoriaRepository = FakeCategoriaRepository(),
      scope: CoroutineScope,
  ) =
      HomeViewModel(
          preferencesRepository = preferences,
          movimientoRepository = movimientos,
          categoriaRepository = categorias,
          scope = scope,
      )

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
    private val recentValues = MutableStateFlow(recent)
    private val summaryValues = MutableStateFlow(summary)
    var insertCalls = 0
      private set

    var recentSubscriptions = 0
      private set

    var summarySubscriptions = 0
      private set

    override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = flow {
      recentSubscriptions += 1
      emitAll(recentValues.filterNotNull())
    }

    override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = flow {
      summarySubscriptions += 1
      emitAll(summaryValues.filterNotNull())
    }

    override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
      insertCalls += 1
      return DataOutcome.Success(Unit)
    }

    override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(emptyList())

    override suspend fun delete(id: String): DataOutcome<Unit> = DataOutcome.Success(Unit)

    fun emitRecent(value: DataOutcome<List<Movimiento>>) {
      recentValues.value = value
    }

    fun emitSummary(value: DataOutcome<MovimientoSummary>) {
      summaryValues.value = value
    }

    fun invalidateAfterInsert(movimiento: Movimiento) {
      recentValues.value = DataOutcome.Success(listOf(movimiento))
      summaryValues.value = DataOutcome.Success(summary(movimiento.monto.moneda))
    }
  }

  private class FakeCategoriaRepository(initial: DataOutcome<List<Categoria>>? = null) :
      CategoriaRepository {
    private val values = MutableStateFlow(initial)
    var subscriptionCount = 0
      private set

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = flow {
      subscriptionCount += 1
      emitAll(values.filterNotNull())
    }

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

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
            descripcion = null,
            createdAt = Instant.parse("2026-09-15T00:00:00Z"),
        )
  }
}
