package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

private const val CONTEXT_SENTENCE =
    "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la tarjeta de " +
        "crédito"
private const val PARTS_DEBOUNCE_FOR_TESTS = 300L

/** A sentence with several amounts reviewed as one movimiento per amount. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoPartsTest {

  private val clpCard = testCuenta("clp-card", TipoCuenta.CREDITO, Moneda.CLP, nombre = "Falabella")

  @Test
  fun continueWithSeveralAmountsOpensTheReview() = runTest {
    val viewModel = partsViewModel()

    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals(listOf(cop(2_000_000), cop(3_000_000)), state.partMontos())
    assertEquals(
        listOf("Compré 2 hamburguesas por 20mil", "3 gaseosas fueron 30mil"),
        state.parts.map { it.titulo },
    )
    assertEquals(listOf(false, false), state.parts.map { it.amountNeedsReview })
    assertEquals(listOf("cat_restaurante", "cat_supermercado"), state.parts.map { it.categoriaId })
    val review = state.review
    assertEquals(MedioPago.CREDITO, review.cuenta.effectiveMedio(review.tipo))
    assertEquals(NU, review.cuenta.selected)
    assertEquals(cop(-5_000_000), review.total)
    assertEquals(0, review.excludedFromTotal)
    assertEquals(listOf(cop(-2_000_000), cop(-3_000_000)), review.parts.map { it.monto })
    assertEquals(listOf(0, 1), review.parts.map { it.index })
    assertTrue(review.canJoin)
  }

  @Test
  fun detectedCardListsThePartsWhileTyping() = runTest {
    val viewModel = partsViewModel()

    type(viewModel, CONTEXT_SENTENCE)

    val parts = viewModel.uiState.value.detected.parts
    assertEquals(2, parts.size)
    assertEquals(listOf(cop(2_000_000), cop(3_000_000)), parts.map { it.monto })
    assertEquals(listOf("cat_restaurante", "cat_supermercado"), parts.map { it.categoria?.id })
    assertEquals(cop(5_000_000), viewModel.uiState.value.detected.partsTotal)

    type(viewModel, "Gasté 20 mil en cine")

    assertTrue(viewModel.uiState.value.detected.parts.isEmpty())
  }

  @Test
  fun pickingAPartCategoryOnlyChangesThatPart() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val singleCategoria = viewModel.uiState.value.categoriaId

    viewModel.onPartCategoriaSelected(0, "cat_cine")

    val picked = viewModel.uiState.value
    assertEquals(listOf("cat_cine", "cat_supermercado"), picked.parts.map { it.categoriaId })
    assertEquals(listOf(true, false), picked.parts.map { it.categoriasEdited })
    assertEquals(singleCategoria, picked.categoriaId)
    assertFalse("cat_cine" in picked.parts[0].suggestions.map { it.id })

    viewModel.onPartCategoriaCleared(1)

    val cleared = viewModel.uiState.value
    assertNull(cleared.parts[1].categoriaId)
    assertTrue(cleared.parts[1].categoriasEdited)
    assertTrue(cleared.review.parts[1].categoriaIsPending)
    assertEquals("cat_cine", cleared.parts[0].categoriaId)

    viewModel.onPartCategoriaSelected(0, "cat_unknown")
    viewModel.onPartCategoriaSelected(5, "cat_cine")

    assertEquals(cleared.parts, viewModel.uiState.value.parts)
  }

  @Test
  fun aHandPickedSingleCategoryIsNotAppliedToParts() = runTest {
    val viewModel = partsViewModel()
    viewModel.onCategoriaSelected("cat_cine")

    type(viewModel, CONTEXT_SENTENCE)
    assertEquals(
        listOf("cat_restaurante", "cat_supermercado"),
        viewModel.uiState.value.detected.parts.map { it.categoria?.id },
    )
    viewModel.onPhraseContinue()

    val review = viewModel.uiState.value
    assertEquals(listOf("cat_restaurante", "cat_supermercado"), review.parts.map { it.categoriaId })
    assertEquals("cat_cine", review.categoriaId)

    viewModel.onStepBack()
    type(viewModel, "Gasté 20 mil en el almuerzo")
    viewModel.onPhraseContinue()

    val single = viewModel.uiState.value
    assertEquals(AddMovimientoStep.SUMMARY, single.step)
    assertEquals("cat_cine", single.categoriaId)
    assertTrue(single.parts.isEmpty())
  }

  @Test
  fun removingDownToOnePartOpensTheSummaryForIt() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onPartRemoved(0)

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.SUMMARY, state.step)
    assertTrue(state.parts.isEmpty())
    assertEquals(cop(3_000_000), state.summary.monto)
    assertEquals("3 gaseosas fueron 30mil", state.titulo)
    assertEquals("cat_supermercado", state.categoriaId)
    assertEquals(MedioPago.CREDITO, state.summary.cuenta.effectiveMedio(state.tipo))
  }

  @Test
  fun removingOneOfThreePartsKeepsTheReview() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, THREE_PARTS)
    viewModel.onPhraseContinue()

    viewModel.onPartRemoved(1)

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals(listOf("Gasté 20 mil en cine", "10 mil en taxi"), state.parts.map { it.titulo })
    assertEquals(cop(-3_000_000), state.review.total)
  }

  @Test
  fun joinSumsThePartsIntoOneSummary() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartCategoriaSelected(1, "cat_restaurante")

    viewModel.onJoinParts()

    val joined = viewModel.uiState.value
    assertEquals(AddMovimientoStep.SUMMARY, joined.step)
    assertTrue(joined.parts.isEmpty())
    assertEquals(cop(5_000_000), joined.summary.monto)
    assertEquals(CONTEXT_SENTENCE, joined.titulo)
    assertEquals("cat_restaurante", joined.categoriaId)
    assertEquals(MedioPago.CREDITO, joined.summary.cuenta.effectiveMedio(joined.tipo))
  }

  @Test
  fun joinAfterARemovalJoinsTheRemainingTitulosWithoutACategory() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, THREE_PARTS)
    viewModel.onPhraseContinue()
    val sentenceQuery = viewModel.uiState.value.suggestionQuery

    viewModel.onPartRemoved(1)
    viewModel.onJoinParts()

    val joined = viewModel.uiState.value
    assertEquals(AddMovimientoStep.SUMMARY, joined.step)
    assertEquals(cop(3_000_000), joined.summary.monto)
    assertEquals("Gasté 20 mil en cine, 10 mil en taxi", joined.titulo)
    assertNull(joined.categoriaId)
    assertEquals(sentenceQuery, joined.suggestionQuery)
  }

  @Test
  fun joinedAmountIsNotRederivedFromTheWholeSentence() = runTest {
    val viewModel = partsViewModel(cuentas = partsCuentas() + clpCard)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()

    viewModel.onCuentaSelected(clpCard.id)

    assertEquals(
        cop(5_000_000).toAmountInput().reformatAmountInput(Moneda.COP, Moneda.CLP),
        viewModel.uiState.value.amountInput,
    )
    assertFalse(viewModel.uiState.value.amountNeedsReview)
  }

  @Test
  fun backFromReviewKeepsTheSentence() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onStepBack()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.PHRASE, state.step)
    assertEquals(CONTEXT_SENTENCE, state.phrase)
    assertEquals(2, state.detected.parts.size)
  }

  @Test
  fun backFromAJoinedSummaryReturnsToTheSentenceAndContinueSplitsAgain() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()

    viewModel.onStepBack()
    assertEquals(AddMovimientoStep.PHRASE, viewModel.uiState.value.step)
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.REVIEW, viewModel.uiState.value.step)
    assertEquals(2, viewModel.uiState.value.parts.size)
  }

  @Test
  fun continueAgainKeepsAHandPickedPartCategoryForTheSameTitulo() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartCategoriaSelected(0, "cat_cine")
    viewModel.onPartCategoriaSelected(1, "cat_transporte")

    viewModel.onStepBack()
    type(viewModel, CONTEXT_SENTENCE.replace("20mil", "25mil"))
    viewModel.onPhraseContinue()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals(listOf("cat_restaurante", "cat_transporte"), state.parts.map { it.categoriaId })
    assertEquals(listOf(false, true), state.parts.map { it.categoriasEdited })
  }

  @Test
  fun sharedCuentaInAnotherCurrencyRederivesEachPartAmount() = runTest {
    val viewModel = partsViewModel(cuentas = partsCuentas() + clpCard)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onCuentaSelected(clpCard.id)

    val state = viewModel.uiState.value
    assertEquals(
        listOf(Money(20_000, Moneda.CLP), Money(30_000, Moneda.CLP)),
        state.partMontos(),
    )
    assertEquals(Money(-50_000, Moneda.CLP), state.review.total)
    assertEquals(0, state.review.excludedFromTotal)
  }

  @Test
  fun aSplitSentenceCanStillBeFilledManually() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onStepBack()

    viewModel.onFillManually()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertTrue(state.parts.isEmpty())
  }

  @Test
  fun aSingleHandPickSurvivesAJoinAndBack() = runTest {
    val viewModel = partsViewModel()
    viewModel.onCategoriaSelected("cat_cine")
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    assertNull(viewModel.uiState.value.categoriaId)

    back(viewModel)
    type(viewModel, "Gasté 20 mil en el almuerzo")
    viewModel.onPhraseContinue()

    assertEquals(AddMovimientoStep.SUMMARY, viewModel.uiState.value.step)
    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aSingleHandPickSurvivesRemovingDownToOnePartAndBack() = runTest {
    val viewModel = partsViewModel()
    viewModel.onCategoriaSelected("cat_cine")
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartRemoved(0)
    assertEquals("cat_supermercado", viewModel.uiState.value.categoriaId)

    back(viewModel)
    type(viewModel, "Gasté 20 mil en el almuerzo")
    viewModel.onPhraseContinue()

    assertEquals("cat_cine", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun aCategoryPickedInTheJoinedSummaryIsKeptOnBack() = runTest {
    val viewModel = partsViewModel()
    viewModel.onCategoriaSelected("cat_cine")
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    viewModel.onCategoriaSelected("cat_transporte")

    back(viewModel)
    type(viewModel, "Gasté 20 mil en el almuerzo")
    viewModel.onPhraseContinue()

    assertEquals("cat_transporte", viewModel.uiState.value.categoriaId)
  }

  @Test
  fun partHandPicksSurviveAJoinAndBack() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartCategoriaSelected(1, "cat_transporte")
    viewModel.onJoinParts()

    back(viewModel)
    viewModel.onPhraseContinue()

    assertEquals(
        listOf("cat_restaurante", "cat_transporte"),
        viewModel.uiState.value.parts.map { it.categoriaId },
    )
  }

  @Test
  fun backShowsThePartCategoriesContinueWillGive() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartCategoriaSelected(0, "cat_cine")

    back(viewModel)

    assertEquals(
        listOf("cat_cine", "cat_supermercado"),
        viewModel.uiState.value.detected.parts.map { it.categoria?.id },
    )
  }

  @Test
  fun editDoesNothingInTheReview() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onEditMovimiento()

    assertEquals(review, viewModel.uiState.value)
  }

  @Test
  fun editingAPartAndDoneReturnsToTheReviewWithItsChanges() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onPartEdit(1)

    val form = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, form.step)
    assertEquals(1, form.editingPart)
    assertEquals(AddMovimientoStep.REVIEW, form.stepBackTarget)
    assertEquals(review.parts[1].amountInput, form.amountInput)
    assertEquals("3 gaseosas fueron 30mil", form.titulo)
    assertEquals("cat_supermercado", form.categoriaId)
    assertEquals(review.frecuencia, form.frecuencia)

    viewModel.onAmountChanged(cop(3_500_000).toAmountInput())
    viewModel.onTituloChanged("3 gaseosas")
    viewModel.onDescripcionChanged("para la fiesta")
    viewModel.onCategoriaSelected("cat_restaurante")
    viewModel.onPartDone()

    val done = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, done.step)
    assertNull(done.editingPart)
    assertEquals(review.parts[0], done.parts[0])
    val part = done.parts[1]
    assertEquals(cop(3_500_000).toAmountInput(), part.amountInput)
    assertTrue(part.amountEdited)
    assertEquals("3 gaseosas", part.titulo)
    assertEquals("para la fiesta", part.descripcion)
    assertEquals("cat_restaurante", part.categoriaId)
    assertTrue(part.categoriasEdited)
    assertEquals(PartOverrides(), part.overrides)
    assertEquals(review.amountInput, done.amountInput)
    assertEquals(review.titulo, done.titulo)
    assertEquals(review.descripcion, done.descripcion)
    assertEquals(review.categoriaId, done.categoriaId)
    assertEquals(review.suggestionQuery, done.suggestionQuery)
    assertEquals(review.suggestions, done.suggestions)
    assertEquals(cop(-5_500_000), done.review.total)
  }

  @Test
  fun backFromAPartFormDiscardsItsEdits() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onPartEdit(0)
    viewModel.onAmountChanged(cop(9_000_000).toAmountInput())
    viewModel.onFechaChanged(YESTERDAY)
    viewModel.onCategoriaCleared()
    viewModel.onEtiquetasChanged(emptyList())
    back(viewModel)

    assertEquals(review, viewModel.uiState.value)
  }

  @Test
  fun aPartWithAnotherFechaShowsItAndIgnoresLaterSharedChanges() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onPartEdit(0)
    viewModel.onFechaChanged(YESTERDAY)
    viewModel.onPartDone()

    val diverged = viewModel.uiState.value
    assertEquals(TODAY, diverged.fecha)
    assertEquals(PartOverrides(fecha = YESTERDAY), diverged.parts[0].overrides)
    assertEquals(listOf(YESTERDAY, null), diverged.review.parts.map { it.fecha })

    viewModel.onFechaChanged(TWO_DAYS_AGO)

    val shared = viewModel.uiState.value
    assertEquals(TWO_DAYS_AGO, shared.review.fecha)
    assertEquals(listOf(YESTERDAY, null), shared.review.parts.map { it.fecha })

    viewModel.onPartEdit(0)
    assertEquals(YESTERDAY, viewModel.uiState.value.fecha)
    back(viewModel)
    viewModel.onPartEdit(1)
    assertEquals(TWO_DAYS_AGO, viewModel.uiState.value.fecha)
  }

  @Test
  fun aPartInAnotherCurrencyRederivesOnlyItsOwnAmount() = runTest {
    val viewModel = partsViewModel(cuentas = partsCuentas() + clpCard)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onPartEdit(0)
    viewModel.onCuentaSelected(clpCard.id)

    val form = viewModel.uiState.value
    assertEquals(Money(20_000, Moneda.CLP).toAmountInput(), form.amountInput)
    assertEquals(review.parts.map { it.amountInput }, form.parts.map { it.amountInput })

    viewModel.onPartDone()

    val done = viewModel.uiState.value
    assertEquals(review.parts[1], done.parts[1])
    assertEquals(clpCard.id, done.parts[0].overrides.cuentaChoice?.cuentaId)
    assertEquals(Money(-20_000, Moneda.CLP), done.review.parts[0].monto)
    assertEquals(clpCard, done.review.parts[0].cuenta)
    assertEquals(cop(-3_000_000), done.review.total)
    assertEquals(1, done.review.excludedFromTotal)
    assertEquals(NU, done.review.cuenta.selected)
  }

  @Test
  fun submitInAPartFormReturnsToTheReviewWithoutSaving() = runTest {
    val movimientos = partsMovimientos()
    val viewModel = partsViewModel(movimientos = movimientos)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onPartEdit(0)
    viewModel.onTituloChanged("hamburguesas")
    viewModel.onSubmit()
    runCurrent()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals("hamburguesas", state.parts[0].titulo)
    assertTrue(movimientos.inserted.isEmpty())
  }

  @Test
  fun saveAllInsertsEveryPartWithSharedValues() = runTest {
    val movimientos = partsMovimientos()
    val recurrencias = FakeRecurrenciaRepository()
    var nextId = 0
    val viewModel =
        partsViewModel(
            movimientos = movimientos,
            recurrencias = recurrencias,
            idGenerator = IdGenerator { "movement-${nextId++}" },
        )
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()

    viewModel.onSubmit()
    runCurrent()

    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
    val saved = movimientos.inserted
    assertEquals(listOf(cop(2_000_000), cop(3_000_000)), saved.map { it.monto })
    assertEquals(
        listOf("Compré 2 hamburguesas por 20mil", "3 gaseosas fueron 30mil"),
        saved.map { it.titulo },
    )
    assertEquals(listOf("cat_restaurante", "cat_supermercado"), saved.map { it.categoriaId })
    assertEquals(listOf(MedioPago.CREDITO, MedioPago.CREDITO), saved.map { it.medio })
    assertEquals(listOf(NU.id, NU.id), saved.map { it.cuentaId })
    assertEquals(listOf(TipoMovimiento.GASTO, TipoMovimiento.GASTO), saved.map { it.tipo })
    assertEquals(listOf(TODAY, TODAY), saved.map { it.fecha })
    assertEquals(2, saved.map { it.id }.distinct().size)
    // History lists the newest first, so the first part is the newest to keep the sentence order.
    assertEquals(listOf(ADD_TEST_NOW, ADD_TEST_NOW - 1.milliseconds), saved.map { it.createdAt })
    assertTrue(recurrencias.created.isEmpty())
  }

  @Test
  fun saveAllWithRepetirCreatesOneRecurrenciaPerPart() = runTest {
    val movimientos = partsMovimientos()
    val recurrencias = FakeRecurrenciaRepository()
    val viewModel = partsViewModel(movimientos = movimientos, recurrencias = recurrencias)
    type(viewModel, "netflix 20 mil y spotify 10 mil cada mes")
    viewModel.onPhraseContinue()
    assertEquals(AddMovimientoStep.REVIEW, viewModel.uiState.value.step)
    assertEquals(Frecuencia.MENSUAL, viewModel.uiState.value.review.frecuencia)

    viewModel.onSubmit()
    runCurrent()

    assertEquals(AddMovimientoEffect.Saved, viewModel.effects.first())
    assertEquals(
        listOf(cop(2_000_000) to Frecuencia.MENSUAL, cop(1_000_000) to Frecuencia.MENSUAL),
        recurrencias.created.map { (movimiento, frecuencia) -> movimiento.monto to frecuencia },
    )
    assertTrue(movimientos.inserted.isEmpty())
  }

  @Test
  fun anInvalidPartOpensItsFormOnSave() = runTest {
    val movimientos = partsMovimientos()
    val viewModel = partsViewModel(cuentas = partsCuentas() + clpCard, movimientos = movimientos)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    // Valid in pesos chilenos, which have no cents, but above the limit once the shared cuenta
    // is in pesos colombianos again.
    viewModel.onCuentaSelected(clpCard.id)
    viewModel.onPartEdit(1)
    viewModel.onAmountChanged(Money(Money.MAX_MINOR_UNITS / 10, Moneda.CLP).toAmountInput())
    viewModel.onPartDone()
    viewModel.onCuentaSelected(NU.id)
    assertEquals(AddMovimientoStep.REVIEW, viewModel.uiState.value.step)
    assertNull(viewModel.uiState.value.review.parts[1].monto)

    viewModel.onSubmit()
    runCurrent()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.FORM, state.step)
    assertEquals(1, state.editingPart)
    assertTrue(state.submitAttempted)
    assertNotNull(state.amountError)
    assertEquals(state.parts[1].suggestions, state.suggestions)
    assertFalse(state.isSubmitting)
    assertTrue(movimientos.inserted.isEmpty())

    viewModel.onPartDone()

    assertEquals(AddMovimientoStep.FORM, viewModel.uiState.value.step)
    assertEquals(1, viewModel.uiState.value.editingPart)
  }

  @Test
  fun saveFailureKeepsTheReviewAndPersistsNothing() = runTest {
    val movimientos = partsMovimientos()
    movimientos.insertResult = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val viewModel = partsViewModel(movimientos = movimientos)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onSubmit()
    runCurrent()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals(MovimientoFormError.SAVE_FAILED, state.error)
    assertFalse(state.isSubmitting)
    assertEquals(review.parts, state.parts)
    assertTrue(movimientos.inserted.isEmpty())
    assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
  }

  @Test
  fun joinIsDisabledWhenAPartHasAnotherTipo() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    assertTrue(viewModel.uiState.value.review.canJoin)

    viewModel.onPartEdit(1)
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onPartDone()

    val state = viewModel.uiState.value
    assertEquals(AddMovimientoStep.REVIEW, state.step)
    assertEquals(TipoMovimiento.GASTO, state.tipo)
    assertEquals(TipoMovimiento.INGRESO, state.parts[1].overrides.tipo)
    assertEquals(TipoMovimiento.INGRESO, state.review.parts[1].tipo)
    assertFalse(state.review.canJoin)

    viewModel.onJoinParts()

    assertEquals(AddMovimientoStep.REVIEW, viewModel.uiState.value.step)
  }

  @Test
  fun aPartTituloEditNeverChangesWhereItsAmountIsReadFrom() = runTest {
    val viewModel = partsViewModel(cuentas = partsCuentas() + clpCard)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    viewModel.onPartEdit(1)
    viewModel.onTituloChanged("3 gaseosas")
    viewModel.onPartDone()

    viewModel.onCuentaSelected(clpCard.id)

    val shared = viewModel.uiState.value
    assertEquals("3 gaseosas", shared.parts[1].titulo)
    assertEquals(
        listOf(Money(20_000, Moneda.CLP), Money(30_000, Moneda.CLP)),
        shared.partMontos(),
    )

    viewModel.onPartEdit(1)
    viewModel.onCuentaSelected(NU.id)

    assertEquals(cop(3_000_000).toAmountInput(), viewModel.uiState.value.amountInput)
  }

  @Test
  fun changingOnlyAPartTipoKeepsFollowingTheSharedCuenta() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, THREE_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.DEBITO)
    viewModel.onCuentaSelected(NEQUI.id)
    assertEquals(
        MedioPago.DEBITO,
        viewModel.uiState.value.review.cuenta.effectiveMedio(TipoMovimiento.GASTO),
    )

    viewModel.onPartEdit(1)
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onPartDone()

    val diverged = viewModel.uiState.value
    assertEquals(PartOverrides(tipo = TipoMovimiento.INGRESO), diverged.parts[1].overrides)
    assertEquals(MedioPago.TRANSFERENCIA, diverged.review.parts[1].medio)
    assertNull(diverged.review.parts[1].cuenta)

    viewModel.onCuentaSelected(COP_CASH.id)
    viewModel.onPartEdit(1)

    assertEquals(COP_CASH, viewModel.uiState.value.cuentaField.selected)
  }

  // MedioPago.forTipo only swaps medios that accept the same cuenta types, so no fixture can give a
  // tipo-only part another currency; this pins that the review and Save resolve one choice.
  @Test
  fun aPartIsCountedWithTheCuentaItIsSavedWith() = runTest {
    val viewModel = partsViewModel()
    type(viewModel, THREE_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.DEBITO)
    viewModel.onCuentaSelected(NEQUI.id)
    viewModel.onPartEdit(1)
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onPartDone()

    val state = viewModel.uiState.value
    val choice = state.partCuentaChoice(state.parts[1])
    assertEquals(MedioPago.TRANSFERENCIA, choice.medio)
    assertEquals(NEQUI.id, choice.cuentaId)
    state.parts.forEachIndexed { index, part ->
      val form = state.withPartForm(index)
      assertEquals(form.cuentaChoice, state.partCuentaChoice(part))
      assertEquals(form.moneda, state.partMoneda(part))
    }
  }

  @Test
  fun aCatalogUpdateWhileAPartFormIsOpenReachesTheSharedCuenta() = runTest {
    val movimientos = partsMovimientos()
    val cuentaRepository = FakeCuentaRepository(partsCuentas())
    val viewModel = partsViewModel(movimientos = movimientos, cuentaRepository = cuentaRepository)
    type(viewModel, THREE_PARTS)
    viewModel.onPhraseContinue()
    assertEquals(COP_CASH, viewModel.uiState.value.review.cuenta.selected)

    viewModel.onPartEdit(0)
    movimientos.selection = DataOutcome.Success(NEQUI.selection(MedioPago.TRANSFERENCIA))
    cuentaRepository.cuentas.value = partsCuentas() + clpCard
    runCurrent()
    back(viewModel)

    assertEquals(NEQUI, viewModel.uiState.value.review.cuenta.selected)

    viewModel.onPartEdit(0)
    movimientos.selection = DataOutcome.Success(COP_CASH.selection(MedioPago.EFECTIVO))
    cuentaRepository.cuentas.value = partsCuentas()
    runCurrent()
    viewModel.onPartDone()

    val done = viewModel.uiState.value
    assertEquals(PartOverrides(), done.parts[0].overrides)
    assertEquals(COP_CASH, done.review.cuenta.selected)
  }

  private fun TestScope.back(viewModel: AddMovimientoViewModel) {
    viewModel.onStepBack()
    runCurrent()
  }

  private fun AddMovimientoUiState.partMontos(): List<Money?> = parts.map { part ->
    (parseAmountInput(part.amountInput, moneda) as? AmountInputResult.Valid)?.money
  }

  private fun cop(minorUnits: Long) = Money(minorUnits, Moneda.COP)

  private fun partsCuentas(): List<Cuenta> = listOf(COP_CASH, NEQUI, NU, USD_CASH)

  private fun partsMovimientos() =
      AddFakeMovimientos().apply {
        medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO)
      }

  private fun TestScope.partsViewModel(
      cuentas: List<Cuenta> = partsCuentas(),
      movimientos: AddFakeMovimientos = partsMovimientos(),
      recurrencias: FakeRecurrenciaRepository = FakeRecurrenciaRepository(),
      idGenerator: IdGenerator = IdGenerator { "movement-id" },
      cuentaRepository: FakeCuentaRepository = FakeCuentaRepository(cuentas),
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
              preferencesRepository = addPreferences(Moneda.COP),
              cuentaRepository = cuentaRepository,
              recurrenciaRepository = recurrencias,
              idGenerator = idGenerator,
              scope = backgroundScope,
          )
          .also { runCurrent() }

  private fun TestScope.type(viewModel: AddMovimientoViewModel, text: String) {
    viewModel.onPhraseChanged(text)
    testScheduler.advanceTimeBy(PARTS_DEBOUNCE_FOR_TESTS)
    runCurrent()
  }

  private companion object {
    const val THREE_PARTS = "Gasté 20 mil en cine y 40 mil en comida y 10 mil en taxi"
    val TODAY = LocalDate(2026, 9, 15)
    val YESTERDAY = LocalDate(2026, 9, 14)
    val TWO_DAYS_AGO = LocalDate(2026, 9, 13)
  }
}
