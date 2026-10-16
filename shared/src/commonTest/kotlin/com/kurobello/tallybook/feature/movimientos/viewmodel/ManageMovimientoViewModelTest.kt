package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoViewModelTest {
  @Test
  fun amountPrefillRoundTripsExactCurrencyAndLongBoundaries() {
    listOf(
            Money(1234, Moneda.COP),
            Money(1234, Moneda.USD),
            Money(1234, Moneda.CLP),
            Money(Long.MAX_VALUE, Moneda.COP),
            Money(1, Moneda.USD),
        )
        .forEach { amount ->
          assertEquals(
              AmountInputResult.Valid(amount),
              parseAmountInput(amount.toAmountInput(), amount.moneda),
          )
          assertFalse(amount.toAmountInput().contains(amount.moneda.symbol))
        }
  }

  @Test
  fun opensByIdAndRejectsRepeatedRequestsUntilOriginHidden() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    assertTrue(f.viewModel.open("movement"))
    assertFalse(f.viewModel.open("other"))
    runCurrent()
    val session = assertNotNull(f.viewModel.uiState.value.session)
    assertEquals(MovimientoManagementPhase.Edit, session.phase)
    assertEquals(managementMovimiento(), session.movimiento)
    assertTrue(f.viewModel.onRequestDismiss())
    assertFalse(f.viewModel.open("other"))
    f.viewModel.onSheetHidden(session.token - 1)
    assertNotNull(f.viewModel.uiState.value.session)
    f.viewModel.onSheetHidden(session.token)
    assertNull(f.viewModel.uiState.value.session)
  }

  @Test
  fun readFailureRetriesAndMissingHasNoMutationActions() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.readResult = managementFailure()
    f.viewModel.open("movement")
    runCurrent()
    assertEquals(MovimientoManagementPhase.LoadFailed, f.viewModel.uiState.value.session?.phase)
    f.repository.readResult = DataOutcome.Success(null)
    f.viewModel.onRetryLoad()
    runCurrent()
    assertEquals(MovimientoManagementPhase.Missing, f.viewModel.uiState.value.session?.phase)
    f.viewModel.onRequestDelete()
    assertNull(f.viewModel.uiState.value.session?.form)
    assertEquals(0, f.repository.deletes)
  }

  @Test
  fun exactPrefillAndCleanCancelCloseWithoutReread() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    assertEquals("12.34", f.viewModel.uiState.value.session?.form?.amountInput)
    assertEquals("Lunch", f.viewModel.uiState.value.session?.form?.descripcion)
    f.repository.readResult = managementFailure()
    f.viewModel.onCancelEdit()
    runCurrent()
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
    assertEquals(1, f.repository.reads)
  }

  @Test
  fun dirtyCancelContinueAndDiscardPreserveThenCloseDraft() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("50.00")
    f.viewModel.onCancelEdit()
    assertEquals(MovimientoDiscardTarget.Close, f.viewModel.uiState.value.session?.discardTarget)
    f.viewModel.onContinueEditing()
    assertEquals("50.00", f.viewModel.uiState.value.session?.form?.amountInput)
    assertFalse(f.viewModel.onRequestDismiss())
    assertEquals(MovimientoDiscardTarget.Close, f.viewModel.uiState.value.session?.discardTarget)
    f.viewModel.onDiscardChanges()
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
    assertEquals("50.00", f.viewModel.uiState.value.session?.form?.amountInput)
  }

  @Test
  fun normalizedNoOpClosesWithoutWriteRereadOrFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("012.340".dropLast(1))
    f.viewModel.onDescripcionChanged("  Lunch  ")
    f.repository.current = null
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(0, f.repository.updates)
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
    assertEquals(1, f.repository.reads)
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    assertNull(f.viewModel.uiState.value.feedback)
  }

  @Test
  fun originalCategoryIsPreservedWhenTheCatalogNoLongerHasIt() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.current = managementMovimiento().copy(categoriaIds = listOf("child"))
    f.categorias.values.value =
        DataOutcome.Success(listOf(managementCategoria(), managementCategoria("child", "food")))
    f.viewModel.open("movement")
    runCurrent()
    f.categorias.values.value = DataOutcome.Success(emptyList())
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(listOf("child"), f.repository.changes?.categoriaIds)
    assertEquals(1, f.repository.updates)
  }

  @Test
  fun childrenAreSelectableButOtrosIsNot() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.categorias.values.value =
        DataOutcome.Success(
            listOf(
                managementCategoria(),
                managementCategoria("child", "food"),
                managementCategoria(CATEGORIA_OTROS_ID),
            )
        )
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onCategoriaSelected("child")
    f.viewModel.onCategoriaSelected(CATEGORIA_OTROS_ID)
    assertEquals("child", f.viewModel.uiState.value.session?.form?.selectedCategoriaId)
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(listOf("child"), f.repository.changes?.categoriaIds)
  }

  @Test
  fun backwardClockChangeRejectsFutureDateWithoutClamping() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.clock.instant = Instant.parse("2026-09-14T12:00:00Z")
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(0, f.repository.updates)
    assertEquals(LocalDate(2026, 9, 15), f.viewModel.uiState.value.session?.form?.fecha)
    assertTrue(assertNotNull(f.viewModel.uiState.value.session?.form).submitAttempted)
    assertTrue(assertNotNull(f.viewModel.uiState.value.session?.form).isFechaInvalid)
  }

  @Test
  fun changedSaveClosesAndPublishesExactlyAfterHidden() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    val origin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(1, f.repository.updates)
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
    assertNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onSheetHidden(origin)
    assertEquals(MovimientoFeedbackKind.Saved, f.viewModel.uiState.value.feedback?.kind)
    val feedback = f.viewModel.uiState.value.feedback
    f.viewModel.onSheetHidden(origin)
    assertEquals(feedback, f.viewModel.uiState.value.feedback)
    assertEquals(managementMovimiento().createdAt, f.repository.current?.createdAt)
  }

  @Test
  fun suspendedSaveRejectsEditsDismissAndSecondSubmit() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.updateGate = CompletableDeferred()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    f.viewModel.onAmountChanged("30.00")
    f.viewModel.onCancelEdit()
    f.viewModel.onSubmit()
    assertFalse(f.viewModel.onRequestDismiss())
    assertEquals("20.00", f.viewModel.uiState.value.session?.form?.amountInput)
    assertEquals(1, f.repository.updates)
    f.repository.updateGate?.complete(Unit)
    runCurrent()
  }

  @Test
  fun failedSaveKeepsExactDraftAndMissingUpdateNeverResurrects() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.updateResult = managementFailure()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onDescripcionChanged(" Keep\nthis ")
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(" Keep\nthis ", f.viewModel.uiState.value.session?.form?.descripcion)
    assertEquals(MovimientoFormError.SAVE_FAILED, f.viewModel.uiState.value.session?.form?.error)
    f.repository.updateResult = DataOutcome.Success(MovimientoUpdateResult.NotFound)
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(MovimientoManagementError.MissingOnSave, f.viewModel.uiState.value.session?.error)
    assertEquals("20.00", f.viewModel.uiState.value.session?.form?.amountInput)
  }

  @Test
  fun categoryRecoveryKeepsTheDraftAndUnretriedSaveFailure() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.updateResult = managementFailure()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    f.categorias.values.value = managementFailure()
    runCurrent()
    assertEquals(
        MovimientoFormError.CATEGORIES_UNAVAILABLE,
        f.viewModel.uiState.value.session?.form?.error,
    )
    val recovered = listOf(managementCategoria(), managementCategoria("new-parent"))
    f.categorias.values.value = DataOutcome.Success(recovered)
    runCurrent()
    assertEquals(recovered, f.viewModel.uiState.value.session?.form?.categorias)
    assertEquals("20.00", f.viewModel.uiState.value.session?.form?.amountInput)
    assertEquals(MovimientoFormError.SAVE_FAILED, f.viewModel.uiState.value.session?.form?.error)
    assertEquals(1, f.repository.updates)
  }

  @Test
  fun lateReadCannotReopenClosedSession() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.readGate = CompletableDeferred()
    f.viewModel.open("movement")
    runCurrent()
    val origin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onRequestDismiss()
    f.viewModel.onSheetHidden(origin)
    f.repository.readGate?.complete(Unit)
    runCurrent()
    assertNull(f.viewModel.uiState.value.session)
  }
}
