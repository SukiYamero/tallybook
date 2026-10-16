package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoEntryFlowTest {

  @Test
  fun continueDerivesTheSummaryFromTheSentence() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("  Gasté 20.000 en cine ayer  ")
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    val summary = assertNotNull(state.summary)
    assertEquals(AddMovimientoStep.SUMMARY, state.step)
    assertEquals(TipoMovimiento.GASTO, summary.tipo)
    assertEquals(Money(2_000_000, Moneda.COP), summary.monto)
    assertEquals(LocalDate(2026, 9, 14), summary.fecha)
    assertEquals("Gasté 20.000 en cine ayer", summary.descripcion)
    assertEquals("cat_cine", summary.categoria.id)
    assertFalse(summary.categoriaIsPending)
    assertFalse(summary.amountNeedsReview)
  }

  @Test
  fun aWeakIncomeSuggestionDoesNotImplyIngreso() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Regalo 50 mil")
    viewModel.onPhraseContinue()

    assertEquals(TipoMovimiento.GASTO, viewModel.uiState.value.tipo)
  }

  @Test
  fun anIncomeSuggestionImpliesIngresoWhenTheSentenceHasNoVerb() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Sueldo 3 millones")
    viewModel.onPhraseContinue()

    assertEquals(TipoMovimiento.INGRESO, viewModel.uiState.value.tipo)
    assertEquals("cat_sueldo", viewModel.uiState.value.selectedCategoriaId)
  }

  @Test
  fun continueCapturesTodayFromTheInjectedClockAndTimeZone() = runTest {
    val viewModel = entryViewModel(timeZone = TimeZone.of("America/Bogota"))

    viewModel.onPhraseChanged("Gasté 10.000")
    viewModel.onPhraseContinue()

    val expected = LocalDate(2026, 9, 14)
    assertEquals(expected, viewModel.uiState.value.fecha)
    assertEquals(expected, viewModel.uiState.value.today)
  }

  @Test
  fun anAmbiguousAmountIsFlaggedUntilItIsEdited() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Cena con 2 amigos 50000")
    viewModel.onPhraseContinue()

    assertTrue(assertNotNull(viewModel.uiState.value.summary).amountNeedsReview)

    viewModel.onAmountChanged("50000,00")

    assertFalse(assertNotNull(viewModel.uiState.value.summary).amountNeedsReview)
  }

  @Test
  fun mediumSuggestionsAreOfferedWithoutPreselection() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Aceite 30 mil")
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    val summary = assertNotNull(state.summary)
    assertNull(state.selectedCategoriaId)
    assertTrue(summary.categoriaIsPending)
    assertEquals(CATEGORIA_OTROS_ID, summary.categoria.id)
    assertEquals(
        setOf("cat_supermercado", "cat_transporte"),
        summary.suggestions.map { it.id }.toSet(),
    )
  }

  @Test
  fun selectingASuggestionSwitchesTheSingleSelectionAndReturnsTheOldOneToTheList() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Aceite 30 mil")
    viewModel.onPhraseContinue()

    viewModel.onSuggestionSelected("cat_supermercado")
    assertEquals("cat_supermercado", viewModel.uiState.value.selectedCategoriaId)
    assertEquals(listOf("cat_transporte"), viewModel.uiState.value.suggestions.map { it.id })

    viewModel.onSuggestionSelected("cat_transporte")
    assertEquals("cat_transporte", viewModel.uiState.value.selectedCategoriaId)
    assertEquals(listOf("cat_supermercado"), viewModel.uiState.value.suggestions.map { it.id })

    viewModel.onSuggestionSelected("cat_unknown")
    assertEquals("cat_transporte", viewModel.uiState.value.selectedCategoriaId)
  }

  @Test
  fun aDismissedSuggestionStaysDismissedOnlyForTheSameSubject() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    assertEquals("cat_cine", viewModel.uiState.value.selectedCategoriaId)

    viewModel.onCategoriaSelected("cat_comida")

    assertEquals(setOf("cat_cine"), viewModel.uiState.value.dismissedSuggestionIds)
    assertTrue(viewModel.uiState.value.suggestions.none { it.id == "cat_cine" })

    viewModel.onStepBack()
    viewModel.onPhraseContinue()

    assertEquals(setOf("cat_cine"), viewModel.uiState.value.dismissedSuggestionIds)
    assertTrue(viewModel.uiState.value.selectedCategoriaId != "cat_cine")
    assertTrue(viewModel.uiState.value.suggestions.none { it.id == "cat_cine" })

    viewModel.onStepBack()
    viewModel.onPhraseChanged("Gasté 15.000 en la película")
    viewModel.onPhraseContinue()

    assertTrue(viewModel.uiState.value.dismissedSuggestionIds.isEmpty())
    assertEquals("cat_cine", viewModel.uiState.value.selectedCategoriaId)
  }

  @Test
  fun continuingAgainReplacesTheValuesDerivedFromTheEarlierSentence() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Sueldo 3 millones")
    viewModel.onPhraseContinue()
    viewModel.onStepBack()

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    assertEquals(TipoMovimiento.GASTO, state.tipo)
    assertEquals("cat_cine", state.selectedCategoriaId)
    assertEquals("Gasté 20.000 en cine", state.descripcion)
  }

  @Test
  fun savingWithoutACategoryPersistsOtros() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Gasté 5.000 en algo raro")
    viewModel.onPhraseContinue()
    assertTrue(assertNotNull(viewModel.uiState.value.summary).categoriaIsPending)

    viewModel.onSubmit()
    runCurrent()

    val saved = movimientos.inserted.single()
    assertEquals(listOf(CATEGORIA_OTROS_ID), saved.categoriaIds)
    assertEquals("Gasté 5.000 en algo raro", saved.descripcion)
    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
  }

  @Test
  fun savingFromTheSummaryWithoutAnAmountOpensTheFormWithItsError() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Algo raro")
    viewModel.onPhraseContinue()
    assertNull(assertNotNull(viewModel.uiState.value.summary).monto)

    viewModel.onSubmit()
    runCurrent()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertTrue(state.submitAttempted)
    assertEquals(AmountInputError.EMPTY, state.amountError)
    assertEquals(AddMovimientoStep.SUMMARY, state.stepBackTarget)
    assertTrue(movimientos.inserted.isEmpty())
  }

  @Test
  fun backWalksFormToSummaryToPhraseKeepingTheDraft() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    viewModel.onEditMovimiento()
    viewModel.onFechaChanged(LocalDate(2026, 9, 1))

    viewModel.onStepBack()
    assertEquals(AddMovimientoStep.SUMMARY, viewModel.uiState.value.step)
    assertEquals(LocalDate(2026, 9, 1), assertNotNull(viewModel.uiState.value.summary).fecha)

    viewModel.onStepBack()
    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.PHRASE, state.step)
    assertEquals("Gasté 20.000 en cine", state.phrase)
    assertEquals("cat_cine", state.selectedCategoriaId)
    assertNull(state.stepBackTarget)

    viewModel.onStepBack()
    assertEquals(AddMovimientoStep.PHRASE, viewModel.uiState.value.step)
  }

  @Test
  fun fillingManuallyOpensTheFormWithNoWayBackToTheSentence() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("algo")

    viewModel.onFillManually()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertNull(state.stepBackTarget)
    assertEquals("", state.phrase)
    assertFalse(state.hasUnsavedChanges)
  }

  @Test
  fun fillingManuallyAfterContinueOpensAnEmptyForm() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Sueldo 3 millones ayer")
    viewModel.onPhraseContinue()
    viewModel.onCategoriaSelected("cat_comida")
    assertEquals(TipoMovimiento.INGRESO, viewModel.uiState.value.tipo)

    viewModel.onFillManually()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertEquals(TipoMovimiento.GASTO, state.tipo)
    assertEquals("", state.amountInput)
    assertEquals("", state.descripcion)
    assertEquals("", state.phrase)
    assertEquals(state.today, state.fecha)
    assertNull(state.selectedCategoriaId)
    assertEquals(CATEGORIA_OTROS_ID, state.displayedCategoriaId)
    assertTrue(state.suggestions.isEmpty())
    assertTrue(state.dismissedSuggestionIds.isEmpty())
    assertFalse(state.amountNeedsReview)
    assertFalse(state.hasUnsavedChanges)
    assertNull(state.stepBackTarget)
  }

  @Test
  fun theCategoryFieldFallsBackToOtrosUntilACategoryIsChosen() = runTest {
    val viewModel = entryViewModel()
    assertEquals(CATEGORIA_OTROS_ID, viewModel.uiState.value.displayedCategoriaId)

    viewModel.onCategoriaSelected("cat_comida")

    assertEquals("cat_comida", viewModel.uiState.value.displayedCategoriaId)
  }

  @Test
  fun aPhraseAloneCountsAsUnsavedAndDiscardRestoresTheEntry() = runTest {
    val viewModel = entryViewModel()
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    assertTrue(viewModel.uiState.value.hasUnsavedChanges)

    viewModel.onPhraseContinue()
    viewModel.onDiscardChanges()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.PHRASE, state.step)
    assertEquals("", state.phrase)
    assertEquals("", state.amountInput)
    assertNull(state.selectedCategoriaId)
    assertTrue(state.suggestions.isEmpty())
    assertTrue(state.dismissedSuggestionIds.isEmpty())
    assertFalse(state.hasUnsavedChanges)

    viewModel.onPhraseChanged("   ")
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun theLivePreviewWaitsForTheDebounceAndClearsImmediately() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Gasté 20.000 ayer")
    advanceTimeBy(249)
    runCurrent()
    assertNull(viewModel.uiState.value.preview)

    advanceTimeBy(1)
    runCurrent()
    val preview = assertNotNull(viewModel.uiState.value.preview)
    assertEquals(Money(2_000_000, Moneda.COP), preview.monto)
    assertEquals(LocalDate(2026, 9, 14), preview.fecha)
    assertEquals(TipoMovimiento.GASTO, preview.tipo)

    viewModel.onPhraseChanged("")
    assertNull(viewModel.uiState.value.preview)
  }

  @Test
  fun aPreviewResultFromEarlierInputNeverReplacesTheLatestOne() = runTest {
    val computation = HeldDispatcher()
    val viewModel = entryViewModel(computationDispatcher = computation)
    computation.releaseAll()

    viewModel.onPhraseChanged("Gasté 20.000")
    advanceTimeBy(250)
    runCurrent()
    viewModel.onPhraseChanged("Gasté 30.000")
    computation.releaseAll()
    runCurrent()

    assertNull(viewModel.uiState.value.preview)

    advanceTimeBy(250)
    runCurrent()
    computation.releaseAll()
    runCurrent()

    assertEquals(Money(3_000_000, Moneda.COP), assertNotNull(viewModel.uiState.value.preview).monto)
  }

  @Test
  fun thePreviewDoesNotRunOutsideTheSentenceStep() = runTest {
    val clock = AddFixedClock(ADD_TEST_NOW)
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos, clock = clock)
    viewModel.onPhraseChanged("Gasté 20.000 ayer")
    advanceTimeBy(250)
    runCurrent()
    viewModel.onPhraseContinue()
    val summaryState = viewModel.uiState.value

    clock.instant = ADD_TEST_NOW + 1.days
    movimientos.history.value =
        DataOutcome.Success(
            listOf(CategoryHistoryRow("Gasté 20.000 ayer", "cat_comida", ADD_TEST_NOW - 1.days))
        )
    runCurrent()
    advanceTimeBy(250)
    runCurrent()

    assertEquals(summaryState.today, viewModel.uiState.value.today)
    assertEquals(summaryState.preview, viewModel.uiState.value.preview)
  }

  @Test
  fun continueIsIgnoredForABlankSentence() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("   ")
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.PHRASE, viewModel.uiState.value.step)
  }

  @Test
  fun aSavedCorrectionMakesTheNextIdenticalSubjectAHighSuggestion() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Aceite 30 mil")
    viewModel.onPhraseContinue()
    assertNull(viewModel.uiState.value.selectedCategoriaId)

    movimientos.history.value =
        DataOutcome.Success(
            listOf(CategoryHistoryRow("Aceite 30 mil", "cat_supermercado", ADD_TEST_NOW - 1.days))
        )
    runCurrent()
    viewModel.onDiscardChanges()
    viewModel.onPhraseChanged("Aceite 15 mil")
    viewModel.onPhraseContinue()

    assertEquals("cat_supermercado", viewModel.uiState.value.selectedCategoriaId)
  }

  @Test
  fun aHistoryFailureFallsBackToDictionaryOnlySuggestions() = runTest {
    val movimientos = AddFakeMovimientos()
    movimientos.history.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val viewModel = entryViewModel(movimientos = movimientos)

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.SUMMARY, viewModel.uiState.value.step)
    assertEquals("cat_cine", viewModel.uiState.value.selectedCategoriaId)
  }

  private fun TestScope.entryViewModel(
      movimientos: AddFakeMovimientos = AddFakeMovimientos(),
      timeZone: TimeZone = TimeZone.UTC,
      clock: AddFixedClock = AddFixedClock(ADD_TEST_NOW),
      computationDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
              preferencesRepository = addPreferences(Moneda.COP),
              clock = clock,
              timeZone = timeZone,
              computationDispatcher = computationDispatcher,
              scope = backgroundScope,
          )
          .also { runCurrent() }
}

// Work dispatched to it only runs when the test releases it, which holds a computation mid-flight.
private class HeldDispatcher : CoroutineDispatcher() {
  private val pending = ArrayDeque<Runnable>()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    pending.addLast(block)
  }

  fun releaseAll() {
    while (pending.isNotEmpty()) pending.removeFirst().run()
  }
}
