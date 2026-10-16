package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
    val clock = AddFixedClock(ADD_TEST_NOW)
    val viewModel =
        createAddViewModel(
            clock = clock,
            timeZone = TimeZone.of("America/Bogota"),
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(LocalDate(2026, 9, 14), viewModel.uiState.value.fecha)
  }

  @Test
  fun loadsThePreferredMonedaAndCategoriasAndOnlySelectsLoadedCategoriaIds() = runTest {
    val category = addTestCategoria("food")
    val categories = AddFakeCategorias(DataOutcome.Success(listOf(category)))
    val viewModel = createAddViewModel(categoriaRepository = categories, scope = backgroundScope)

    runCurrent()
    viewModel.onCategoriasChanged(listOf("missing"))

    assertEquals(null, viewModel.uiState.value.categoriaId)
    assertEquals(Moneda.USD, viewModel.uiState.value.moneda)
    assertEquals(listOf(category), viewModel.uiState.value.categorias)
    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.onCategoriasChanged(listOf(category.id))

    assertEquals(category.id, viewModel.uiState.value.categoriaId)
  }

  @Test
  fun canSubmitRequiresASelectedCategoriaFromTheLoadedList() = runTest {
    val category = addTestCategoria("food")
    val viewModel =
        createAddViewModel(
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(listOf(category))),
            scope = backgroundScope,
        )

    runCurrent()
    viewModel.onAmountChanged("1.00")

    assertFalse(viewModel.uiState.value.canSubmit)

    viewModel.onCategoriasChanged(listOf(category.id))

    assertTrue(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun changingADraftFieldMarksTheFormAsHavingUnsavedChanges() = runTest {
    val viewModel = createAddViewModel(scope = backgroundScope)
    runCurrent()

    viewModel.onAmountChanged("12.34")

    assertTrue(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun discardingChangesRestoresTheOpeningDraft() = runTest {
    val viewModel = createAddViewModel(scope = backgroundScope)
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
    val repository = AddFakeMovimientos()
    val category = addTestCategoria("food")
    val categories = AddFakeCategorias(DataOutcome.Success(listOf(category)))
    val viewModel =
        createAddViewModel(
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
    viewModel.onCategoriasChanged(listOf(category.id))
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
    val repository = AddFakeMovimientos()
    val category = addTestCategoria("food")
    val clock = AddFixedClock(ADD_TEST_NOW)
    val viewModel =
        createAddViewModel(
            movimientoRepository = repository,
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(listOf(category))),
            clock = clock,
            timeZone = TimeZone.of("America/Bogota"),
            scope = backgroundScope,
        )

    runCurrent()
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onAmountChanged("12.34")
    viewModel.onCategoriasChanged(listOf(category.id))
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
            titulo = null,
            descripcion = "Salary September",
            createdAt = ADD_TEST_NOW,
        ),
        repository.inserted.single(),
    )
  }

  @Test
  fun capsDescriptionInputAndNormalizesBlankDescriptionToNull() = runTest {
    val repository = AddFakeMovimientos()
    val category = addTestCategoria("food")
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
    val repository = AddSuspendedMovimientos()
    val category = addTestCategoria("food")
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
    val repository = AddFakeMovimientos()
    val category = addTestCategoria("food")
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
        AddFakeMovimientos(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    val category = addTestCategoria("food")
    val viewModel = createValidViewModel(repository, category, backgroundScope)

    runCurrent()
    populateValidFields(viewModel, category)
    viewModel.onDescripcionChanged("Keep this")
    viewModel.onSubmit()
    runCurrent()

    assertEquals(MovimientoFormError.SAVE_FAILED, viewModel.uiState.value.error)
    assertEquals("12.34", viewModel.uiState.value.amountInput)
    assertEquals(category.id, viewModel.uiState.value.categoriaId)
    assertEquals("Keep this", viewModel.uiState.value.descripcion)
    assertTrue(viewModel.uiState.value.submitAttempted)

    viewModel.onAmountChanged("20.00")

    assertNull(viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.submitAttempted)
  }

  @Test
  fun retryAfterSaveFailureUsesTheUnchangedDraft() = runTest {
    val repository =
        AddFakeMovimientos(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    val category = addTestCategoria("food")
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
    val categories = AddFakeCategorias(DataOutcome.Success(emptyList()))
    val viewModel = createAddViewModel(categoriaRepository = categories, scope = backgroundScope)

    runCurrent()
    categories.emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()

    assertEquals(MovimientoFormError.CATEGORIES_UNAVAILABLE, viewModel.uiState.value.error)

    categories.emit(DataOutcome.Success(listOf(addTestCategoria("food"))))
    runCurrent()

    assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun preferenceLoadFailureIsDurableAndPreventsSubmittingWithTheFallbackMoneda() = runTest {
    val viewModel =
        createAddViewModel(
            preferencesRepository =
                AddFakePreferences(DataOutcome.Failure(DataError.Local(IllegalStateException()))),
            scope = backgroundScope,
        )

    runCurrent()

    assertEquals(MovimientoFormError.PREFERENCES_UNAVAILABLE, viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun cancelledInsertDoesNotBecomeSaveFailureOrEmitSaved() = runTest {
    val repository = AddSuspendedMovimientos()
    val category = addTestCategoria("food")
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

  @Test
  fun hiddenCategoriesAreNeverSuggestedOrPreselected() = runTest {
    val categorias =
        addSeedCategorias().map { if (it.id == "cat_cine") it.copy(oculta = true) else it }
    val viewModel =
        createAddViewModel(
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(categorias)),
            preferencesRepository = addPreferences(Moneda.COP),
            scope = backgroundScope,
        )
    runCurrent()

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    assertFalse(state.categoriaId == "cat_cine")
    assertTrue(state.suggestions.none { it.id == "cat_cine" })
  }

  private fun createValidViewModel(
      movimientoRepository: MovimientoRepository,
      category: Categoria,
      scope: CoroutineScope,
  ): AddMovimientoViewModel =
      createAddViewModel(
          movimientoRepository = movimientoRepository,
          categoriaRepository = AddFakeCategorias(DataOutcome.Success(listOf(category))),
          scope = scope,
      )

  private fun populateValidFields(viewModel: AddMovimientoViewModel, category: Categoria) {
    viewModel.onAmountChanged("12.34")
    viewModel.onCategoriasChanged(listOf(category.id))
  }
}
