package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The entry engine's medio and cuenta detection as the creation flow layers it. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoSentenceCuentaTest {

  private val clpCard = testCuenta("clp-card", TipoCuenta.CREDITO, Moneda.CLP, nombre = "Falabella")

  @Test
  fun aNamedCuentaSelectsItAndItsMedioWhileTyping() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 50 mil en cine con la NU")

    val field = viewModel.uiState.value.sentence.cuenta
    assertEquals(NU, field.selected)
    assertEquals(MedioPago.CREDITO, field.medio)
    assertEquals(SelectionOwner.SENTENCE, viewModel.uiState.value.cuentaChoice.owner)
  }

  @Test
  fun aMedioWordAloneSelectsTheDefaultCuentaForIt() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 en mercado con débito")

    val field = viewModel.uiState.value.sentence.cuenta
    assertEquals(MedioPago.DEBITO, field.medio)
    assertEquals(NEQUI, field.selected)
  }

  @Test
  fun anAmbiguousCardKeepsTheDefault() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 en mercado con tarjeta")

    val field = viewModel.uiState.value.sentence.cuenta
    assertEquals(MedioPago.EFECTIVO, field.medio)
    assertEquals(COP_CASH, field.selected)
  }

  @Test
  fun aSecondNamedCuentaIsOfferedAsASuggestion() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 con la NU o Nequi")

    val field = viewModel.uiState.value.sentence.cuenta
    assertEquals(NU, field.selected)
    assertEquals(listOf(NEQUI), field.suggestions)
  }

  @Test
  fun continueWritesTheSentenceSelectionAndAnIngresoNeverKeepsDebito() = runTest {
    val movimientos = sentenceMovimientos()
    val viewModel = sentenceViewModel(movimientos)

    type(viewModel, "Recibí 20.000 con débito")
    viewModel.onPhraseContinue()
    viewModel.onSubmit()
    runCurrent()

    val inserted = movimientos.inserted.single()
    assertEquals(TipoMovimiento.INGRESO, inserted.tipo)
    assertEquals(MedioPago.TRANSFERENCIA, inserted.medio)
    assertEquals(NEQUI.id, inserted.cuentaId)
  }

  @Test
  fun aManualChoiceTransfersOwnershipToTheUser() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 con la NU")
    viewModel.onMedioSelected(MedioPago.EFECTIVO)
    type(viewModel, "Gasté 30.000 con la NU")
    viewModel.onPhraseContinue()

    val field = viewModel.uiState.value.cuentaField
    assertEquals(MedioPago.EFECTIVO, field.medio)
    assertEquals(COP_CASH, field.selected)
  }

  @Test
  fun clearingTheSentenceClearsOnlyTheSentenceSelection() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 con la NU")
    viewModel.onPhraseChanged("")
    assertEquals(COP_CASH, viewModel.uiState.value.sentence.cuenta.selected)

    viewModel.onCuentaSelected(NEQUI.id)
    type(viewModel, "Gasté 20.000 con la NU")
    viewModel.onPhraseChanged("")
    assertEquals(NEQUI, viewModel.uiState.value.sentence.cuenta.selected)
  }

  @Test
  fun backKeepsTheSentenceSelectionAndAnotherContinueReplacesIt() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 con la NU")
    viewModel.onPhraseContinue()
    viewModel.onStepBack()
    assertEquals(NU, viewModel.uiState.value.sentence.cuenta.selected)

    type(viewModel, "Gasté 20.000 en efectivo")
    viewModel.onPhraseContinue()
    assertEquals(COP_CASH, viewModel.uiState.value.cuentaField.selected)
  }

  @Test
  fun theAmountIsReadWithTheNamedCuentasCurrency() = runTest {
    val viewModel = sentenceViewModel(cuentas = sentenceCuentas() + clpCard)

    type(viewModel, "Gasté 12.500 con Falabella")
    viewModel.onPhraseContinue()

    assertEquals(Money(12_500, Moneda.CLP), viewModel.uiState.value.summary.monto)
  }

  @Test
  fun continueBeforeAnyPreviewStillReadsTheNamedCuentasCurrency() = runTest {
    val viewModel = sentenceViewModel(cuentas = sentenceCuentas() + clpCard)

    viewModel.onPhraseChanged("Gasté 12.500 con Falabella")
    viewModel.onPhraseContinue()

    assertEquals(Money(12_500, Moneda.CLP), viewModel.uiState.value.summary.monto)
    assertEquals(clpCard, viewModel.uiState.value.cuentaField.selected)
  }

  @Test
  fun changingTheCuentaRederivesASentenceOwnedAmount() = runTest {
    val viewModel = sentenceViewModel(cuentas = sentenceCuentas() + clpCard)

    type(viewModel, "Gasté 12.500 en cine")
    viewModel.onPhraseContinue()
    assertEquals(Money(1_250_000, Moneda.COP), viewModel.uiState.value.summary.monto)

    viewModel.onCuentaSelected(clpCard.id)

    assertEquals(Money(12_500, Moneda.CLP), viewModel.uiState.value.summary.monto)
    assertEquals("12500", viewModel.uiState.value.amountInput)
  }

  @Test
  fun aTypedAmountIsNotRederived() = runTest {
    val viewModel = sentenceViewModel(cuentas = sentenceCuentas() + clpCard)

    type(viewModel, "Gasté 12.500 en cine")
    viewModel.onPhraseContinue()
    viewModel.onEditMovimiento()
    viewModel.onAmountChanged("12,50")
    viewModel.onCuentaSelected(clpCard.id)

    assertEquals("12,50", viewModel.uiState.value.amountInput)
    assertNull(viewModel.uiState.value.summary.monto)
  }

  @Test
  fun aCuentaNameLeavesTheCategorySubject() = runTest {
    val viewModel = sentenceViewModel()

    type(viewModel, "Gasté 20.000 en cine con la NU")

    assertEquals("cat_cine", viewModel.uiState.value.sentence.categoria?.id)
  }

  private fun sentenceCuentas(): List<Cuenta> = listOf(COP_CASH, NEQUI, NU, USD_CASH)

  private fun sentenceMovimientos() =
      AddFakeMovimientos().apply {
        medioSelections[MedioPago.DEBITO] = NEQUI.selection(MedioPago.DEBITO)
        medioSelections[MedioPago.TRANSFERENCIA] = NEQUI.selection(MedioPago.TRANSFERENCIA)
        medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO)
      }

  private fun TestScope.sentenceViewModel(
      movimientos: AddFakeMovimientos = sentenceMovimientos(),
      cuentas: List<Cuenta> = sentenceCuentas(),
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
    testScheduler.advanceTimeBy(PREVIEW_DEBOUNCE_FOR_TESTS)
    runCurrent()
  }
}

private const val PREVIEW_DEBOUNCE_FOR_TESTS = 300L
