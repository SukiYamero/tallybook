package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

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
  fun submitAndEditDoNothingInTheReview() = runTest {
    val movimientos = partsMovimientos()
    val viewModel = partsViewModel(movimientos = movimientos)
    type(viewModel, CONTEXT_SENTENCE)
    viewModel.onPhraseContinue()
    val review = viewModel.uiState.value

    viewModel.onEditMovimiento()
    viewModel.onSubmit()
    runCurrent()

    assertEquals(review, viewModel.uiState.value)
    assertTrue(movimientos.inserted.isEmpty())
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
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
              preferencesRepository = addPreferences(Moneda.COP),
              cuentaRepository = FakeCuentaRepository(cuentas),
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
  }
}
