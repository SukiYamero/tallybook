package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoCuentaTest {

  @Test
  fun theEditorShowsTheStoredMedioAndCuenta() = runTest {
    val f = openedFixture()

    val field = assertNotNull(f.viewModel.uiState.value.session?.form).cuentaField
    assertEquals(MedioPago.EFECTIVO, field.medio)
    assertEquals(USD_CASH, field.selected)
    assertNull(field.primaryMoneda)
  }

  @Test
  fun onlyCuentasInTheStoredCurrencyAreOffered() = runTest {
    val f = openedFixture { medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO) }

    f.viewModel.onMedioSelected(MedioPago.CREDITO)

    val field = assertNotNull(f.viewModel.uiState.value.session?.form).cuentaField
    assertEquals(listOf(USD_CARD), field.options)
    assertEquals(USD_CARD, field.selected)
    assertTrue(f.repository.selectionRequests.all { it.first == Moneda.USD })
  }

  @Test
  fun aChangedMedioAndCuentaAreSaved() = runTest {
    val f = openedFixture()

    f.viewModel.onCuentaSelected(USD_BANK.id)
    f.viewModel.onSubmit()
    runCurrent()

    val changes = assertNotNull(f.repository.changes)
    assertEquals(USD_BANK.id, changes.cuentaId)
    assertEquals(MedioPago.DEBITO, changes.medio)
  }

  @Test
  fun anUntouchedArchivedCuentaStaysVisibleAndIsSavedAsIs() = runTest {
    val archived = USD_BANK.copy(archivada = true)
    val f =
        openedFixture(
            movimiento =
                managementMovimiento().copy(cuentaId = archived.id, medio = MedioPago.DEBITO),
            cuentas = listOf(USD_CASH, archived, USD_CARD),
        )

    assertEquals(archived, f.viewModel.uiState.value.session?.form?.cuentaField?.selected)
    f.viewModel.onTituloChanged("Groceries")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(archived.id, assertNotNull(f.repository.changes).cuentaId)
  }

  @Test
  fun leavingAnArchivedCuentaDropsItFromTheOptions() = runTest {
    val archived = USD_BANK.copy(archivada = true)
    val f =
        openedFixture(
            movimiento =
                managementMovimiento().copy(cuentaId = archived.id, medio = MedioPago.DEBITO),
            cuentas = listOf(USD_CASH, archived, USD_CARD),
        )

    f.viewModel.onMedioSelected(MedioPago.EFECTIVO)
    f.viewModel.onMedioSelected(MedioPago.DEBITO)

    val field = assertNotNull(f.viewModel.uiState.value.session?.form).cuentaField
    assertEquals(emptyList(), field.options)
    assertNull(field.selected)
  }

  @Test
  fun switchingToIngresoTurnsDebitoIntoTransferencia() = runTest {
    val f =
        openedFixture(
            movimiento =
                managementMovimiento().copy(cuentaId = USD_BANK.id, medio = MedioPago.DEBITO)
        )

    f.viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    f.viewModel.onSubmit()
    runCurrent()

    val changes = assertNotNull(f.repository.changes)
    assertEquals(MedioPago.TRANSFERENCIA, changes.medio)
    assertEquals(USD_BANK.id, changes.cuentaId)
  }

  @Test
  fun reselectingTheStoredMedioIsANoOp() = runTest {
    val f = openedFixture()

    f.viewModel.onMedioSelected(MedioPago.EFECTIVO)
    f.viewModel.onCuentaSelected(USD_CASH.id)
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(0, f.repository.updates)
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
  }

  @Test
  fun aCuentaTheRepositoryRejectsBecomesTheCuentaError() = runTest {
    val f = openedFixture()
    f.viewModel.onCuentaSelected(USD_CARD.id)
    f.repository.updateResult = DataOutcome.Failure(DataError.InvalidInput("Cuenta is archived"))
    f.cuentas.cuentas.value =
        f.cuentas.cuentas.value.map { if (it.id == USD_CARD.id) it.copy(archivada = true) else it }

    f.viewModel.onSubmit()
    runCurrent()

    val form = assertNotNull(f.viewModel.uiState.value.session?.form)
    assertEquals(CuentaFieldError.UNAVAILABLE, form.cuentaField.cuentaError)
    assertNull(form.error)
  }

  @Test
  fun anotherRejectionKeepsTheSharedSaveError() = runTest {
    val f = openedFixture()
    f.viewModel.onCuentaSelected(USD_CARD.id)
    f.repository.updateResult = DataOutcome.Failure(DataError.InvalidInput("Unknown categoria"))

    f.viewModel.onSubmit()
    runCurrent()

    val form = assertNotNull(f.viewModel.uiState.value.session?.form)
    assertNull(form.cuentaField.cuentaError)
    assertEquals(MovimientoFormError.SAVE_FAILED, form.error)
  }

  @Test
  fun aSuccessfulSaveAfterARejectionCloses() = runTest {
    val f = openedFixture()
    f.viewModel.onCuentaSelected(USD_CARD.id)
    f.repository.updateResult = DataOutcome.Success(MovimientoUpdateResult.Updated)

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
  }

  private fun TestScope.openedFixture(
      movimiento: com.kurobello.tallybook.core.model.Movimiento = managementMovimiento(),
      cuentas: List<com.kurobello.tallybook.core.model.Cuenta>? = null,
      repository: ManagementRepository.() -> Unit = {},
  ): ManageMovimientoFixture {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.current = movimiento
    f.repository.repository()
    cuentas?.let { f.cuentas.cuentas.value = it }
    f.viewModel.open(movimiento.id)
    runCurrent()
    return f
  }
}
