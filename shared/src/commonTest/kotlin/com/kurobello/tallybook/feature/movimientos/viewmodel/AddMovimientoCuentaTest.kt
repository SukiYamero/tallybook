package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoCuentaTest {

  private val food = addTestCategoria("food")

  @Test
  fun aNewDraftStartsWithTheRepositoryDefaultAndItsCurrency() = runTest {
    val movimientos = cuentaMovimientos()
    val viewModel = cuentaViewModel(movimientos)

    val field = viewModel.uiState.value.cuentaField
    assertEquals(MedioPago.EFECTIVO, field.medio)
    assertEquals(COP_CASH, field.selected)
    assertEquals(Moneda.COP, viewModel.uiState.value.moneda)
    assertEquals(
        listOf(null) + MedioPago.entries,
        movimientos.selectionRequests.map { it.third },
    )
    assertTrue(movimientos.selectionRequests.all { it.first == Moneda.COP })
  }

  @Test
  fun theMedioOptionsFollowTheTipo() = runTest {
    val viewModel = cuentaViewModel()

    viewModel.onFillManually()
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)

    assertEquals(
        listOf(MedioPago.EFECTIVO, MedioPago.TRANSFERENCIA, MedioPago.CREDITO),
        viewModel.uiState.value.cuentaField.medioOptions,
    )
  }

  @Test
  fun pickingAMedioSelectsItsDefaultCuentaAndListsTheCompatibleOnes() = runTest {
    val viewModel = cuentaViewModel()

    viewModel.onMedioSelected(MedioPago.CREDITO)

    val field = viewModel.uiState.value.cuentaField
    assertEquals(MedioPago.CREDITO, field.medio)
    assertEquals(NU, field.selected)
    assertEquals(listOf(NU, USD_CARD), field.options)
    assertEquals(SelectionOwner.USER, viewModel.uiState.value.cuentaChoice.owner)
  }

  @Test
  fun aMedioWithoutCompatibleCuentasLeavesTheSelectorEmptyAndBlocksSave() = runTest {
    val movimientos = cuentaMovimientos().apply { medioSelections[MedioPago.CREDITO] = null }
    val viewModel = cuentaViewModel(movimientos, cuentas = listOf(COP_CASH, NEQUI))
    populate(viewModel)

    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onSubmit()
    runCurrent()

    val field = viewModel.uiState.value.cuentaField
    assertNull(field.selected)
    assertEquals(emptyList(), field.options)
    assertEquals(CuentaFieldError.MISSING, field.cuentaError)
    assertTrue(movimientos.inserted.isEmpty())
  }

  @Test
  fun theOnlyCompatibleCuentaIsSelectedWithoutAPicker() = runTest {
    val movimientos = cuentaMovimientos().apply { medioSelections[MedioPago.DEBITO] = null }
    val viewModel = cuentaViewModel(movimientos, cuentas = listOf(COP_CASH, NEQUI))

    viewModel.onMedioSelected(MedioPago.DEBITO)

    assertEquals(NEQUI, viewModel.uiState.value.cuentaField.selected)
  }

  @Test
  fun theCurrencyFollowsTheSelectedCuenta() = runTest {
    val viewModel = cuentaViewModel()
    populate(viewModel)

    viewModel.onCuentaSelected(USD_CARD.id)
    viewModel.onAmountChanged("12.50")

    assertEquals(Moneda.USD, viewModel.uiState.value.moneda)
    assertEquals(MedioPago.CREDITO, viewModel.uiState.value.cuentaField.medio)
    assertTrue(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun aClpCuentaRejectsTypedDecimalsOnSave() = runTest {
    val movimientos = cuentaMovimientos()
    val viewModel = cuentaViewModel(movimientos)
    populate(viewModel, amount = "12,50")

    viewModel.onCuentaSelected(CLP_CASH.id)
    viewModel.onSubmit()
    runCurrent()

    assertEquals("12,50", viewModel.uiState.value.amountInput)
    assertEquals(AmountInputError.TOO_MANY_DECIMALS, viewModel.uiState.value.amountError)
    assertTrue(movimientos.inserted.isEmpty())
  }

  @Test
  fun saveStoresTheChosenMedioAndCuenta() = runTest {
    val movimientos = cuentaMovimientos()
    val viewModel = cuentaViewModel(movimientos)
    populate(viewModel)

    viewModel.onMedioSelected(MedioPago.DEBITO)
    viewModel.onSubmit()
    runCurrent()

    val inserted = movimientos.inserted.single()
    assertEquals(NEQUI.id, inserted.cuentaId)
    assertEquals(MedioPago.DEBITO, inserted.medio)
    assertEquals(Money(1_234_00, Moneda.COP), inserted.monto)
  }

  @Test
  fun switchingToIngresoTurnsDebitoIntoTransferenciaOnTheSameCuenta() = runTest {
    val movimientos = cuentaMovimientos()
    val viewModel = cuentaViewModel(movimientos)
    populate(viewModel)

    viewModel.onMedioSelected(MedioPago.DEBITO)
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    viewModel.onSubmit()
    runCurrent()

    val inserted = movimientos.inserted.single()
    assertEquals(MedioPago.TRANSFERENCIA, inserted.medio)
    assertEquals(NEQUI.id, inserted.cuentaId)
  }

  @Test
  fun fillManuallyKeepsAMedioAndCuentaPickedByHand() = runTest {
    val viewModel = cuentaViewModel()

    viewModel.onPhraseChanged("Gasté 20.000 en cine")
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onFillManually()

    val field = viewModel.uiState.value.cuentaField
    assertEquals(MedioPago.CREDITO, field.medio)
    assertEquals(NU, field.selected)
  }

  @Test
  fun aMedioPickedInTheDialogDoesNotGuardIt() = runTest {
    val viewModel = cuentaViewModel()

    viewModel.onMedioSelected(MedioPago.CREDITO)

    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    viewModel.onFillManually()
    assertTrue(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun discardReturnsToTheDefaultSelection() = runTest {
    val viewModel = cuentaViewModel()

    viewModel.onFillManually()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onDiscardChanges()

    assertEquals(COP_CASH, viewModel.uiState.value.cuentaField.selected)
    assertEquals(SelectionOwner.DEFAULT, viewModel.uiState.value.cuentaChoice.owner)
  }

  @Test
  fun aCuentaArchivedWhileSavingBecomesTheCuentaError() = runTest {
    val cuentas = FakeCuentaRepository(listOf(COP_CASH, NEQUI, NU))
    val movimientos =
        object : AddFakeMovimientos() {
          override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
            cuentas.setArchivada(NU.id, archivada = true)
            return DataOutcome.Failure(DataError.InvalidInput("Cuenta nu is archived"))
          }
        }
    movimientos.medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO)
    val viewModel = cuentaViewModel(movimientos, cuentaRepository = cuentas)
    populate(viewModel)
    viewModel.onMedioSelected(MedioPago.CREDITO)

    viewModel.onSubmit()
    runCurrent()

    val state = viewModel.uiState.value
    assertEquals(CuentaFieldError.UNAVAILABLE, state.cuentaField.cuentaError)
    assertNull(state.error)

    viewModel.onMedioSelected(MedioPago.EFECTIVO)
    assertNull(viewModel.uiState.value.cuentaField.cuentaError)
  }

  @Test
  fun anotherSaveFailureKeepsTheSharedSaveError() = runTest {
    val movimientos =
        cuentaMovimientos().apply {
          insertResult = DataOutcome.Failure(DataError.Local(IllegalStateException()))
        }
    val viewModel = cuentaViewModel(movimientos)
    populate(viewModel)

    viewModel.onSubmit()
    runCurrent()

    assertEquals(MovimientoFormError.SAVE_FAILED, viewModel.uiState.value.error)
    assertNull(viewModel.uiState.value.cuentaField.cuentaError)
  }

  @Test
  fun aFailedCuentaReadBlocksEntryWithTheAccountsError() = runTest {
    val cuentas =
        FakeCuentaRepository(listOf(COP_CASH)).apply {
          failure = DataError.Local(IllegalStateException())
        }
    val viewModel = cuentaViewModel(cuentaRepository = cuentas)

    assertEquals(MovimientoFormError.ACCOUNTS_UNAVAILABLE, viewModel.uiState.value.error)

    cuentas.failure = null
    viewModel.onRetryLoad()
    runCurrent()

    assertNull(viewModel.uiState.value.error)
  }

  private fun cuentaMovimientos() =
      AddFakeMovimientos().apply {
        medioSelections[MedioPago.DEBITO] = NEQUI.selection(MedioPago.DEBITO)
        medioSelections[MedioPago.TRANSFERENCIA] = NEQUI.selection(MedioPago.TRANSFERENCIA)
        medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO)
      }

  private fun TestScope.cuentaViewModel(
      movimientos: AddFakeMovimientos = cuentaMovimientos(),
      cuentas: List<com.kurobello.tallybook.core.model.Cuenta> =
          listOf(COP_CASH, NEQUI, NU, OLD_BANK, USD_CASH, USD_CARD, CLP_CASH),
      cuentaRepository: FakeCuentaRepository = FakeCuentaRepository(cuentas),
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(listOf(food))),
              preferencesRepository = addPreferences(Moneda.COP),
              cuentaRepository = cuentaRepository,
              scope = backgroundScope,
          )
          .also { runCurrent() }

  private fun populate(viewModel: AddMovimientoViewModel, amount: String = "1234") {
    viewModel.onFillManually()
    viewModel.onAmountChanged(amount)
    viewModel.onCategoriaSelected(food.id)
  }
}
