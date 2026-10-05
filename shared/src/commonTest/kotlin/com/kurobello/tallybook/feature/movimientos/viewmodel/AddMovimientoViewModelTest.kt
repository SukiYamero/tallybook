package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoViewModelTest {

  @Test
  fun defaultsTheDateInTheInjectedLocalTimeZoneInsteadOfUtc() = runTest {
    val clock = FixedClock(Instant.parse("2026-09-15T02:30:00Z"))
    val viewModel =
        createViewModel(
            clock = clock,
            timeZone = TimeZone.of("America/Bogota"),
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(LocalDate(2026, 9, 14), viewModel.uiState.value.fecha)
  }

  @Test
  fun loadsThePreferredMonedaAndCategoriasAndOnlySelectsLoadedCategoriaIds() = runTest {
    val category = testCategoria("food")
    val categories = FakeCategoriaRepository(DataOutcome.Success(listOf(category)))
    val viewModel = createViewModel(categoriaRepository = categories, scope = backgroundScope)

    runCurrent()
    viewModel.onCategoriaSelected("missing")

    assertNull(viewModel.uiState.value.selectedCategoriaId)
    assertEquals(Moneda.USD, viewModel.uiState.value.moneda)
    assertEquals(listOf(category), viewModel.uiState.value.categorias)
    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.onCategoriaSelected(category.id)

    assertEquals(category.id, viewModel.uiState.value.selectedCategoriaId)
  }

  @Test
  fun canSubmitRequiresASelectedCategoriaFromTheLoadedList() = runTest {
    val category = testCategoria("food")
    val viewModel =
        createViewModel(
            categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
            scope = backgroundScope,
        )

    runCurrent()
    viewModel.onAmountChanged("1.00")

    assertFalse(viewModel.uiState.value.canSubmit)

    viewModel.onCategoriaSelected(category.id)

    assertTrue(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun changingADraftFieldMarksTheFormAsHavingUnsavedChanges() = runTest {
    val viewModel = createViewModel(scope = backgroundScope)
    runCurrent()

    viewModel.onAmountChanged("12.34")

    assertTrue(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun discardingChangesRestoresTheOpeningDraft() = runTest {
    val viewModel = createViewModel(scope = backgroundScope)
    runCurrent()
    viewModel.onAmountChanged("12.34")
    viewModel.onDescripcionChanged("Lunch")

    viewModel.onDiscardChanges()

    assertEquals("", viewModel.uiState.value.amountInput)
    assertEquals("", viewModel.uiState.value.descripcion)
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun validationFailuresNeverInsertAMovimiento() = runTest {
    val repository = FakeMovimientoRepository()
    val category = testCategoria("food")
    val categories = FakeCategoriaRepository(DataOutcome.Success(listOf(category)))
    val viewModel =
        createViewModel(
            movimientoRepository = repository,
            categoriaRepository = categories,
            scope = backgroundScope,
        )

    runCurrent()
    viewModel.onSubmit()
    assertTrue(repository.inserted.isEmpty())
    assertEquals(AmountInputError.EMPTY, viewModel.uiState.value.amountError)
    assertTrue(viewModel.uiState.value.isCategoriaMissing)
    viewModel.onAmountChanged("0")
    viewModel.onCategoriaSelected(category.id)
    viewModel.onSubmit()
    assertTrue(repository.inserted.isEmpty())
    assertEquals(AmountInputError.NOT_POSITIVE, viewModel.uiState.value.amountError)
    viewModel.onAmountChanged("1.00")
    viewModel.onFechaChanged(LocalDate(2026, 9, 16))
    viewModel.onSubmit()
    assertTrue(repository.inserted.isEmpty())
    viewModel.onFechaChanged(LocalDate(2026, 9, 15))
    categories.emit(DataOutcome.Success(emptyList()))
    runCurrent()
    viewModel.onSubmit()
    runCurrent()

    assertTrue(repository.inserted.isEmpty())
    assertTrue(viewModel.uiState.value.isCategoriaMissing)
    assertTrue(viewModel.uiState.value.submitAttempted)
  }

  @Test
  fun normalizesDescriptionAndBuildsTheExactMovimiento() = runTest {
    val repository = FakeMovimientoRepository()
    val category = testCategoria("food")
    val clock = FixedClock(Instant.parse("2026-09-15T02:30:00Z"))
    val viewModel =
        createViewModel(
            movimientoRepository = repository,
            categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
            clock = clock,
            timeZone = TimeZone.of("America/Bogota"),
            scope = backgroundScope,
        )

    runCurrent()
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onAmountChanged("12.34")
    viewModel.onCategoriaSelected(category.id)
    viewModel.onDescripcionChanged("  Salary\n\t  September  ")
    viewModel.onSubmit()
    runCurrent()

    assertEquals(
        Movimiento(
            id = "movement-id",
            tipo = TipoMovimiento.INGRESO,
            monto = com.kurobello.tallybook.core.model.Money(1234, Moneda.USD),
            fecha = LocalDate(2026, 9, 14),
            categoriaId = category.id,
            descripcion = "Salary September",
            createdAt = Instant.parse("2026-09-15T02:30:00Z"),
        ),
        repository.inserted.single(),
    )
  }

  @Test
  fun capsDescriptionInputAndNormalizesBlankDescriptionToNull() = runTest {
    val repository = FakeMovimientoRepository()
    val category = testCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onDescripcionChanged("x".repeat(181))
    assertEquals(180, viewModel.uiState.value.descripcion.length)

    viewModel.onDescripcionChanged(" \n\t ")
    viewModel.onSubmit()
    runCurrent()

    assertNull(repository.inserted.single().descripcion)
  }

  @Test
  fun ignoresASecondSubmitWhileTheFirstInsertIsSuspended() = runTest {
    val repository = SuspendedMovimientoRepository()
    val category = testCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onSubmit()
    runCurrent()
    repository.started.await()
    viewModel.onSubmit()
    repository.result.complete(DataOutcome.Success(Unit))
    runCurrent()

    assertEquals(1, repository.insertCalls)
    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
  }

  @Test
  fun completedSaveCannotInsertAgainBeforeItsSavedEffectIsConsumed() = runTest {
    val repository = FakeMovimientoRepository()
    val category = testCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onSubmit()
    runCurrent()
    viewModel.onSubmit()
    runCurrent()

    assertEquals(1, repository.inserted.size)
    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
    assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
  }

  @Test
  fun saveFailurePreservesFieldsAndClearsAfterAnotherEdit() = runTest {
    val repository =
        FakeMovimientoRepository(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    val category = testCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onDescripcionChanged("Keep this")
    viewModel.onSubmit()
    runCurrent()

    assertEquals(MovimientoFormError.SAVE_FAILED, viewModel.uiState.value.error)
    assertEquals("12.34", viewModel.uiState.value.amountInput)
    assertEquals(category.id, viewModel.uiState.value.selectedCategoriaId)
    assertEquals("Keep this", viewModel.uiState.value.descripcion)
    assertTrue(viewModel.uiState.value.submitAttempted)

    viewModel.onAmountChanged("20.00")

    assertNull(viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.submitAttempted)
  }

  @Test
  fun retryAfterSaveFailureUsesTheUnchangedDraft() = runTest {
    val repository =
        FakeMovimientoRepository(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    val category = testCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)
    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onDescripcionChanged("Keep this")
    viewModel.onSubmit()
    runCurrent()
    assertEquals(MovimientoFormError.SAVE_FAILED, viewModel.uiState.value.error)

    repository.insertResult = DataOutcome.Success(Unit)
    viewModel.onSubmit()
    runCurrent()

    assertEquals(2, repository.inserted.size)
    assertEquals(repository.inserted.first(), repository.inserted.last())
    assertNull(viewModel.uiState.value.error)
    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
  }

  @Test
  fun categoryLoadFailureIsDurableUntilTheRepositoryEmitsCategoriasAgain() = runTest {
    val categories = FakeCategoriaRepository(DataOutcome.Success(emptyList()))
    val viewModel = createViewModel(categoriaRepository = categories, scope = backgroundScope)

    runCurrent()
    categories.emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()

    assertEquals(MovimientoFormError.CATEGORIES_UNAVAILABLE, viewModel.uiState.value.error)

    categories.emit(DataOutcome.Success(listOf(testCategoria("food"))))
    runCurrent()

    assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun preferenceLoadFailureIsDurableAndPreventsSubmittingWithTheFallbackMoneda() = runTest {
    val viewModel =
        createViewModel(
            preferencesRepository =
                FakePreferencesRepository(
                    DataOutcome.Failure(DataError.Local(IllegalStateException()))
                ),
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(MovimientoFormError.PREFERENCES_UNAVAILABLE, viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun cancelledInsertDoesNotBecomeSaveFailureOrEmitSaved() = runTest {
    val repository = SuspendedMovimientoRepository()
    val category = testCategoria("food")
    val formScope = CoroutineScope(StandardTestDispatcher(testScheduler))
    val viewModel = createValidViewModel(repository, category, formScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onSubmit()
    runCurrent()
    repository.started.await()
    formScope.cancel()
    runCurrent()

    assertNull(viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
  }

  private fun createValidViewModel(
      movimientoRepository: MovimientoRepository,
      category: Categoria,
      scope: CoroutineScope,
  ): AddMovimientoViewModel =
      createViewModel(
          movimientoRepository = movimientoRepository,
          categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(listOf(category))),
          scope = scope,
      )

  private fun populateValidFields(viewModel: AddMovimientoViewModel, category: Categoria) {
    viewModel.onAmountChanged("12.34")
    viewModel.onCategoriaSelected(category.id)
  }

  private fun createViewModel(
      movimientoRepository: MovimientoRepository = FakeMovimientoRepository(),
      categoriaRepository: CategoriaRepository =
          FakeCategoriaRepository(DataOutcome.Success(emptyList())),
      preferencesRepository: AppPreferencesRepository = FakePreferencesRepository(),
      clock: Clock = FixedClock(Instant.parse("2026-09-15T02:30:00Z")),
      timeZone: TimeZone = TimeZone.UTC,
      scope: CoroutineScope,
  ): AddMovimientoViewModel =
      AddMovimientoViewModel(
          repositories =
              AddMovimientoRepositories(
                  preferences = preferencesRepository,
                  categorias = categoriaRepository,
              ),
          movimientoRepository = movimientoRepository,
          idGenerator = IdGenerator { "movement-id" },
          clock = clock,
          timeZoneProvider = TimeZoneProvider { timeZone },
          scope = scope,
      )

  private class FakePreferencesRepository(
      initial: DataOutcome<AppPreferencesState> =
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
  ) : AppPreferencesRepository {
    override val preferences = MutableStateFlow<DataOutcome<AppPreferencesState>>(initial)

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class FakeCategoriaRepository(initial: DataOutcome<List<Categoria>>) :
      CategoriaRepository {
    private val values = MutableStateFlow(initial)

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = values

    override suspend fun getAll(): DataOutcome<List<Categoria>> = values.value

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    fun emit(value: DataOutcome<List<Categoria>>) {
      values.value = value
    }
  }

  private open class FakeMovimientoRepository(
      var insertResult: DataOutcome<Unit> = DataOutcome.Success(Unit),
  ) : MovimientoRepository {
    val inserted = mutableListOf<Movimiento>()

    override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = emptyFlow()

    override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

    override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
      inserted += movimiento
      return insertResult
    }

    override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(inserted)

    override suspend fun delete(id: String): DataOutcome<Unit> = DataOutcome.Success(Unit)
  }

  private class SuspendedMovimientoRepository : FakeMovimientoRepository() {
    val started = CompletableDeferred<Unit>()
    val result = CompletableDeferred<DataOutcome<Unit>>()
    var insertCalls = 0
      private set

    override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
      insertCalls++
      started.complete(Unit)
      return result.await()
    }
  }

  private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
  }

  private companion object {
    fun testCategoria(id: String): Categoria =
        Categoria(
            id = id,
            nombre = "Food",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
  }
}
