package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.AmountInputError
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
    val summary = state.summary
    assertEquals(AddMovimientoStep.SUMMARY, state.step)
    assertEquals(TipoMovimiento.GASTO, summary.tipo)
    assertEquals(Money(2_000_000, Moneda.COP), summary.monto)
    assertEquals(LocalDate(2026, 9, 14), summary.fecha)
    assertEquals("Gasté 20.000 en cine ayer", summary.titulo)
    assertEquals("", state.descripcion)
    assertEquals("cat_cine", summary.categoria?.id)
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
    assertEquals("cat_sueldo", viewModel.uiState.value.categoriaId)
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

    assertTrue(viewModel.uiState.value.summary.amountNeedsReview)

    viewModel.onAmountChanged("50000,00")

    assertFalse(viewModel.uiState.value.summary.amountNeedsReview)
  }

  @Test
  fun mediumSuggestionsAreOfferedWithoutPreselection() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Aceite 30 mil")
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    val summary = state.summary
    assertNull(state.categoriaId)
    assertTrue(summary.categoriaIsPending)
    assertEquals(CATEGORIA_OTROS_ID, summary.categoria?.id)
    assertEquals(
        setOf("cat_supermercado", "cat_transporte"),
        summary.suggestions.map { it.id }.toSet(),
    )
  }

  @Test
  fun selectingASuggestionSwitchesTheCategoryAndOffersThePreviousOne() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Aceite 30 mil")
    viewModel.onPhraseContinue()

    viewModel.onSuggestionSelected("cat_supermercado")
    assertEquals("cat_supermercado", viewModel.uiState.value.categoriaId)
    assertEquals(listOf("cat_transporte"), viewModel.uiState.value.suggestions.map { it.id })

    viewModel.onSuggestionSelected("cat_transporte")
    assertEquals("cat_transporte", viewModel.uiState.value.categoriaId)
    assertEquals(listOf("cat_supermercado"), viewModel.uiState.value.suggestions.map { it.id })

    viewModel.onSuggestionSelected("cat_unknown")
    assertEquals("cat_transporte", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun clearingASuggestedCategoryDismissesItOnlyForTheSameSubject() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)

    viewModel.onCategoriaCleared()

    assertNull(viewModel.uiState.value.categoriaId)
    assertTrue(viewModel.uiState.value.categoriaIsPending)
    assertEquals(setOf("cat_cine"), viewModel.uiState.value.dismissedSuggestionIds)
    assertTrue(viewModel.uiState.value.suggestions.none { it.id == "cat_cine" })

    viewModel.onStepBack()
    viewModel.onPhraseContinue()

    assertEquals(setOf("cat_cine"), viewModel.uiState.value.dismissedSuggestionIds)
    assertNull(viewModel.uiState.value.categoriaId)
    assertTrue(viewModel.uiState.value.suggestions.none { it.id == "cat_cine" })

    viewModel.onStepBack()
    viewModel.onPhraseChanged("Gasté 15.000 en la película")
    viewModel.onPhraseContinue()

    assertTrue(viewModel.uiState.value.dismissedSuggestionIds.isEmpty())
    assertTrue("cat_cine" in viewModel.uiState.value.suggestions.map { it.id })
  }

  @Test
  fun clearingTheCategoryFallsBackToTheOtrosPendingState() = runTest {
    val viewModel = entryViewModel()
    viewModel.onCategoriaSelected("cat_comida")

    assertEquals("cat_comida", viewModel.uiState.value.categoriaId)

    viewModel.onCategoriaCleared()

    assertNull(viewModel.uiState.value.categoriaId)
    assertEquals(CATEGORIA_OTROS_ID, viewModel.uiState.value.displayedCategoriaId)
  }

  @Test
  fun aNewPickReplacesTheCategoryAndUnknownOrOtrosPicksAreIgnored() = runTest {
    val viewModel = entryViewModel()
    viewModel.onCategoriaSelected("cat_comida")

    viewModel.onCategoriaSelected("cat_transporte")
    assertEquals("cat_transporte", viewModel.uiState.value.categoriaId)

    viewModel.onCategoriaSelected("cat_unknown")
    viewModel.onCategoriaSelected(CATEGORIA_OTROS_ID)
    assertEquals("cat_transporte", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aChildPickReplacesItsParent() = runTest {
    val viewModel = entryViewModel()
    viewModel.onCategoriaSelected("cat_comida")

    viewModel.onCategoriaSelected("cat_supermercado")

    assertEquals("cat_supermercado", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aRealCategoryReplacesTheOtrosFallbackAndTheLatestPickIsSaved() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Gasté 5.000 en algo raro")
    viewModel.onPhraseContinue()
    assertEquals(CATEGORIA_OTROS_ID, viewModel.uiState.value.displayedCategoriaId)

    viewModel.onCategoriaSelected("cat_comida")
    viewModel.onCategoriaSelected("cat_salud")
    assertEquals("cat_salud", viewModel.uiState.value.displayedCategoriaId)
    viewModel.onSubmit()
    runCurrent()

    assertEquals("cat_salud", movimientos.inserted.single().categoriaId)
    assertEquals(emptyList(), movimientos.inserted.single().etiquetaIds)
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
    assertEquals("cat_cine", state.categoriaId)
    assertEquals("Gasté 20.000 en cine", state.titulo)
  }

  @Test
  fun savingWithoutACategoryPersistsOtros() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Gasté 5.000 en algo raro")
    viewModel.onPhraseContinue()
    assertTrue(viewModel.uiState.value.summary.categoriaIsPending)

    viewModel.onSubmit()
    runCurrent()

    val saved = movimientos.inserted.single()
    assertEquals(CATEGORIA_OTROS_ID, saved.categoriaId)
    assertEquals("Gasté 5.000 en algo raro", saved.titulo)
    assertNull(saved.descripcion)
    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
  }

  @Test
  fun aSummaryDescriptionSurvivesBackAndContinueAndIsSavedNextToTheTitle() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    viewModel.onDescripcionChanged("  Estreno   con Ana ")
    viewModel.onStepBack()
    viewModel.onPhraseChanged("Gasté 25.000 en cine")
    viewModel.onPhraseContinue()

    viewModel.onSubmit()
    runCurrent()

    val saved = movimientos.inserted.single()
    assertEquals("Gasté 25.000 en cine", saved.titulo)
    assertEquals("Estreno con Ana", saved.descripcion)
  }

  @Test
  fun theFormTitleIsCappedAtTheTitleLimit() = runTest {
    val viewModel = entryViewModel()
    viewModel.onFillManually()

    viewModel.onTituloChanged("a".repeat(MOVIMIENTO_TITLE_MAX_LENGTH + 5))

    assertEquals(MOVIMIENTO_TITLE_MAX_LENGTH, viewModel.uiState.value.titulo.length)
  }

  @Test
  fun aManualEntryWithoutATitleSavesNoTitle() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onFillManually()
    viewModel.onAmountChanged("5000")
    viewModel.onTituloChanged("   ")

    viewModel.onSubmit()
    runCurrent()

    assertNull(movimientos.inserted.single().titulo)
  }

  @Test
  fun savingFromTheSummaryWithoutAnAmountOpensTheFormWithItsError() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)
    viewModel.onPhraseChanged("Algo raro")
    viewModel.onPhraseContinue()
    assertNull(viewModel.uiState.value.summary.monto)

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
    assertEquals(LocalDate(2026, 9, 1), viewModel.uiState.value.summary.fecha)

    viewModel.onStepBack()
    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.PHRASE, state.step)
    assertEquals("Gasté 20.000 en cine", state.phrase)
    assertEquals("cat_cine", state.categoriaId)
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
  fun fillingManuallyAfterContinueKeepsOnlyTheCategoryPickedByHand() = runTest {
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
    assertEquals("", state.titulo)
    assertEquals("", state.descripcion)
    assertEquals("", state.phrase)
    assertEquals(state.today, state.fecha)
    assertEquals("cat_comida", state.categoriaId)
    assertTrue(state.categoriasEdited)
    assertTrue(state.suggestions.isEmpty())
    assertTrue(state.dismissedSuggestionIds.isEmpty())
    assertFalse(state.amountNeedsReview)
    assertNull(state.stepBackTarget)
  }

  @Test
  fun fillingManuallyDropsTheCategoryTheSentenceDerived() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    viewModel.onStepBack()
    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)

    viewModel.onFillManually()

    val state = viewModel.uiState.value
    assertNull(state.categoriaId)
    assertEquals(CATEGORIA_OTROS_ID, state.displayedCategoriaId)
    assertFalse(state.categoriasEdited)
    assertFalse(state.hasUnsavedChanges)
  }

  @Test
  fun fillingManuallyKeepsTheCategoryPickedByHandInTheDialog() = runTest {
    val viewModel = entryViewModel()
    viewModel.onCategoriaSelected("cat_salud")
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onFillManually()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertEquals("cat_salud", state.categoriaId)
    assertEquals("", state.amountInput)
    assertEquals("", state.phrase)
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
    assertNull(state.categoriaId)
    assertTrue(state.suggestions.isEmpty())
    assertTrue(state.dismissedSuggestionIds.isEmpty())
    assertFalse(state.hasUnsavedChanges)

    viewModel.onPhraseChanged("   ")
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun theDialogAlwaysShowsDetectedDefaultsUntilTheSentenceSaysMore() = runTest {
    val viewModel = entryViewModel()

    val empty = viewModel.uiState.value.detected
    assertEquals(TipoMovimiento.GASTO, empty.tipo)
    assertEquals(viewModel.uiState.value.today, empty.fecha)
    assertNull(empty.monto)

    viewModel.onPhraseChanged("Sueldo 3 millones ayer")
    advanceTimeBy(250)
    runCurrent()

    val detected = viewModel.uiState.value.detected
    assertEquals(TipoMovimiento.INGRESO, detected.tipo)
    assertEquals(Money(300_000_000, Moneda.COP), detected.monto)
    assertEquals(LocalDate(2026, 9, 14), detected.fecha)
    assertEquals("cat_sueldo", viewModel.uiState.value.categoriaId)

    viewModel.onPhraseChanged("")
    assertEquals(TipoMovimiento.GASTO, viewModel.uiState.value.detected.tipo)
    assertNull(viewModel.uiState.value.categoriaId)
  }

  @Test
  fun theSentenceOwnsTheCategoryUntilTheUserChangesIt() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    advanceTimeBy(250)
    runCurrent()
    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)

    viewModel.onPhraseChanged("Sueldo 3 millones")
    advanceTimeBy(250)
    runCurrent()
    assertEquals("cat_sueldo", viewModel.uiState.value.categoriaId)

    viewModel.onCategoriaSelected("cat_salud")
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    advanceTimeBy(250)
    runCurrent()
    viewModel.onPhraseContinue()

    assertEquals("cat_salud", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun backFromTheSummaryReturnsToTheDialogWithTheTextAndTheDraft() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    viewModel.onCategoriaSelected("cat_comida")

    viewModel.onStepBack()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.PHRASE, state.step)
    assertEquals("Gasté 20.000 en cine", state.phrase)
    assertEquals("cat_comida", state.categoriaId)
    assertEquals(Money(2_000_000, Moneda.COP), state.detected.monto)
  }

  @Test
  fun onlyASentenceMakesTheDialogDirty() = runTest {
    val viewModel = entryViewModel()

    viewModel.onCategoriaSelected("cat_comida")
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)

    viewModel.onPhraseChanged("Gasté 20.000")
    assertTrue(viewModel.uiState.value.hasUnsavedChanges)

    viewModel.onPhraseChanged("  ")
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
    assertNull(viewModel.uiState.value.categoriaId)

    movimientos.history.value =
        DataOutcome.Success(
            listOf(CategoryHistoryRow("Aceite 30 mil", "cat_supermercado", ADD_TEST_NOW - 1.days))
        )
    runCurrent()
    viewModel.onDiscardChanges()
    viewModel.onPhraseChanged("Aceite 15 mil")
    viewModel.onPhraseContinue()

    assertEquals("cat_supermercado", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aHistoryFailureFallsBackToDictionaryOnlySuggestions() = runTest {
    val movimientos = AddFakeMovimientos()
    movimientos.history.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val viewModel = entryViewModel(movimientos = movimientos)

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.SUMMARY, viewModel.uiState.value.step)
    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun clearingTheSentenceAfterBackLeavesTheDialogClean() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Sueldo 3 millones ayer")
    viewModel.onPhraseContinue()
    viewModel.onStepBack()
    assertTrue(viewModel.uiState.value.hasUnsavedChanges)

    viewModel.onPhraseChanged("")

    val state = viewModel.uiState.value
    assertFalse(state.hasUnsavedChanges)
    assertEquals(TipoMovimiento.GASTO, state.tipo)
    assertEquals("", state.amountInput)
    assertEquals("", state.titulo)
    assertEquals("", state.descripcion)
    assertEquals(state.today, state.fecha)
    assertFalse(state.amountNeedsReview)
    assertNull(state.categoriaId)
  }

  @Test
  fun aCategoryPickedByHandSurvivesClearingTheSentenceWithoutMakingTheDialogDirty() = runTest {
    val viewModel = entryViewModel()
    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()
    viewModel.onCategoriaSelected("cat_comida")
    viewModel.onStepBack()

    viewModel.onPhraseChanged("  ")

    val state = viewModel.uiState.value
    assertEquals("cat_comida", state.categoriaId)
    assertEquals("", state.amountInput)
    assertFalse(state.hasUnsavedChanges)
  }

  @Test
  fun newlinesInTheSentenceBecomeSingleSpaces() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("Gasté 20.000\nen cine")
    assertEquals("Gasté 20.000 en cine", viewModel.uiState.value.phrase)

    viewModel.onPhraseChanged("Gasté\r\n\r\n20.000\n")
    assertEquals("Gasté 20.000 ", viewModel.uiState.value.phrase)
  }

  @Test
  fun aSentenceMadeOfNewlinesIsBlank() = runTest {
    val viewModel = entryViewModel()

    viewModel.onPhraseChanged("\n\n")
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.PHRASE, viewModel.uiState.value.step)
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun aSubjectMostlySavedInAnotherCategoryPreselectsTheLearnedOne() = runTest {
    val movimientos = AddFakeMovimientos()
    movimientos.history.value =
        DataOutcome.Success(
            List(3) { CategoryHistoryRow("cine", "cat_comida", ADD_TEST_NOW - (it + 1).days) } +
                CategoryHistoryRow("cine", "cat_cine", ADD_TEST_NOW - 4.days)
        )
    val viewModel = entryViewModel(movimientos = movimientos)

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    assertEquals("cat_comida", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aSubjectSavedWithOneCategoryPreselectsItAlone() = runTest {
    val movimientos = AddFakeMovimientos()
    movimientos.history.value =
        DataOutcome.Success(
            List(3) { CategoryHistoryRow("cine", "cat_cine", ADD_TEST_NOW - (it + 1).days) }
        )
    val viewModel = entryViewModel(movimientos = movimientos)

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onPhraseContinue()

    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)
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
