package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.feedback.UndoSnapshot
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
class ManageMovimientoDirectEditorTest {
  @Test
  fun successfulReadStartsTheExactEditorWithOneCivilDateCapture() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    val session = assertNotNull(f.viewModel.uiState.value.session)
    assertEquals(MovimientoManagementPhase.Edit, session.phase)
    assertEquals(managementMovimiento().toDraft(), session.edit?.draft)
    assertEquals(managementMovimiento(), session.edit?.original)
    assertEquals(LocalDate(2026, 9, 15), session.form?.maxSelectableDate)
    assertEquals(1, f.clock.reads)
    f.clock.instant = Instant.parse("2026-09-16T12:00:00Z")
    f.categorias.values.value = DataOutcome.Success(listOf(managementCategoria("other")))
    runCurrent()
    assertEquals(LocalDate(2026, 9, 15), f.viewModel.uiState.value.session?.form?.maxSelectableDate)
    assertEquals(1, f.clock.reads)
  }

  @Test
  fun backClosesCleanEditorAndUsesTheDraftGuardWhenDirty() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onBack()
    assertEquals(MovimientoManagementPhase.Closing, f.viewModel.uiState.value.session?.phase)
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onDescripcionChanged("Unsaved text")
    val draft = f.viewModel.uiState.value.session?.edit?.draft
    f.viewModel.onBack()
    assertEquals(MovimientoDiscardTarget.Close, f.viewModel.uiState.value.session?.discardTarget)
    f.viewModel.onBack()
    assertNull(f.viewModel.uiState.value.session?.discardTarget)
    assertEquals(draft, f.viewModel.uiState.value.session?.edit?.draft)
    assertEquals(2, f.repository.reads)
  }

  @Test
  fun dirtyInvalidEditorCanDeleteWithUnavailableCategoriesAndCancelKeepsExactDraft() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("invalid")
    f.viewModel.onDescripcionChanged(" Keep\nthis ")
    f.categorias.values.value = managementFailure()
    runCurrent()
    val draft = assertNotNull(f.viewModel.uiState.value.session?.edit?.draft)
    assertEquals("invalid", draft.amountInput)
    assertEquals(" Keep\nthis ", draft.descripcion)
    f.viewModel.onRequestDelete()
    assertTrue(assertNotNull(f.viewModel.uiState.value.session).isDeleteConfirmationVisible)
    f.viewModel.onCancelDelete()
    assertFalse(assertNotNull(f.viewModel.uiState.value.session).isDeleteConfirmationVisible)
    assertEquals(draft, f.viewModel.uiState.value.session?.edit?.draft)
    assertEquals(0, f.repository.deletes)
  }

  @Test
  fun deleteConfirmationRejectsStaleFieldSaveAndCancelCallbacks() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onRequestDelete()
    val confirmation = assertNotNull(f.viewModel.uiState.value.session)
    assertTrue(confirmation.isDeleteConfirmationVisible)
    f.viewModel.onAmountChanged("30.00")
    f.viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    f.viewModel.onFechaChanged(LocalDate(2026, 9, 14))
    f.viewModel.onDescripcionChanged("Stale text")
    f.viewModel.onCategoriaSelected("food")
    f.viewModel.onSubmit()
    f.viewModel.onCancelEdit()
    f.viewModel.onContinueEditing()
    f.viewModel.onDiscardChanges()
    f.viewModel.onRetryLoad()
    runCurrent()
    assertEquals(confirmation, f.viewModel.uiState.value.session)
    assertEquals(0, f.repository.updates)
    assertEquals(0, f.repository.deletes)
    assertEquals(1, f.repository.reads)
  }

  @Test
  fun discardConfirmationRejectsStaleDeleteSaveAndDraftCallbacks() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onCancelEdit()
    val confirmation = assertNotNull(f.viewModel.uiState.value.session)
    assertEquals(MovimientoDiscardTarget.Close, confirmation.discardTarget)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    f.viewModel.onCancelDelete()
    f.viewModel.onCancelEdit()
    f.viewModel.onSubmit()
    f.viewModel.onAmountChanged("30.00")
    f.viewModel.onRetryLoad()
    runCurrent()
    assertEquals(confirmation, f.viewModel.uiState.value.session)
    assertEquals(0, f.repository.updates)
    assertEquals(0, f.repository.deletes)
  }

  @Test
  fun deleteFailureRetainsExactEditorAndRetryUsesTheTransactionalCapture() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.deleteResult = managementFailure()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("invalid")
    f.viewModel.onDescripcionChanged("Unsaved version")
    val draft = f.viewModel.uiState.value.session?.edit?.draft
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    val failed = assertNotNull(f.viewModel.uiState.value.session)
    assertEquals(MovimientoManagementPhase.Edit, failed.phase)
    assertEquals(MovimientoManagementError.DeleteFailed, failed.error)
    assertEquals(draft, failed.edit?.draft)
    assertFalse(failed.isSubmitting)
    val captured = managementMovimiento().copy(descripcion = "Persisted transaction version")
    f.repository.deleteResult = DataOutcome.Success(captured)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(failed.token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(UndoSnapshot.OfMovimiento(captured), feedback.undo)
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    assertEquals(captured, f.repository.restored)
    assertEquals(2, f.repository.deletes)
    assertEquals(0, f.repository.updates)
  }

  @Test
  fun suspendedDeleteRejectsDuplicatesDismissAndDraftChanges() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.deleteGate = CompletableDeferred()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("invalid")
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    val submitting = assertNotNull(f.viewModel.uiState.value.session)
    assertTrue(submitting.isSubmitting)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    f.viewModel.onCancelDelete()
    f.viewModel.onAmountChanged("30.00")
    f.viewModel.onCancelEdit()
    f.viewModel.onSubmit()
    f.viewModel.onBack()
    assertFalse(f.viewModel.onRequestDismiss())
    assertEquals(submitting, f.viewModel.uiState.value.session)
    assertEquals(1, f.repository.deletes)
    f.repository.deleteGate?.complete(Unit)
    runCurrent()
  }
}
