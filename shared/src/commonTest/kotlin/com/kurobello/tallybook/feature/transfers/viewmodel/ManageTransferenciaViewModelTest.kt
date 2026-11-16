package com.kurobello.tallybook.feature.transfers.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult
import com.kurobello.tallybook.core.ui.feedback.DeletedSnapshot
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackViewModel
import com.kurobello.tallybook.core.ui.feedback.ManagementSubject
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsPreferences
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManagementRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ManageTransferenciaViewModelTest {

  @Test
  fun payCardStartsFromTheDefaultOriginWithTheDebtAsTheOnlyAmount() = runTest {
    val f = TransferFixture(backgroundScope)
    assertTrue(f.viewModel.open(TransferEntry.New(origenId = null, NU.id, DEBT)))
    runCurrent()

    val form = f.form()
    assertEquals(EFECTIVO.id, form.draft.origenId)
    assertEquals(NU.id, form.draft.destinoId)
    assertFalse(form.isCrossCurrency)
    assertEquals("3500,00", form.draft.montoOrigen)
  }

  @Test
  fun payCardFromAnotherCurrencyOriginLeavesTheSentAmountEmpty() = runTest {
    val f = TransferFixture(backgroundScope, initial = listOf(stored(DOLARES, NEQUI)))
    f.viewModel.open(TransferEntry.New(origenId = null, NU.id, DEBT))
    runCurrent()

    val form = f.form()
    assertEquals(DOLARES.id, form.draft.origenId)
    assertEquals("", form.draft.montoOrigen)
    assertEquals("3500,00", form.draft.montoDestino)
  }

  @Test
  fun creatingInsertsOnceAndClosesWithoutQueuedFeedback() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.New(NEQUI.id, EFECTIVO.id, prefill = null))
    runCurrent()
    f.viewModel.onMontoOrigenChanged("150")
    f.viewModel.onDescripcionChanged(" Retiro ")
    val gate = CompletableDeferred<Unit>()
    f.transferencias.insertGate = gate

    f.viewModel.onSubmit()
    f.viewModel.onSubmit()
    runCurrent()
    assertTrue(f.session().isSubmitting)
    gate.complete(Unit)
    runCurrent()

    assertEquals(1, f.transferencias.inserts)
    assertEquals(TransferPhase.Closing, f.session().phase)
    f.viewModel.onSheetHidden(f.session().token)
    assertNull(f.viewModel.uiState.value.session)
    assertTrue(f.feedback.state.value.feedbackQueue.isEmpty())
    assertEquals(0, f.feedback.state.value.openSessions)
    val created = f.transferencias.fake.transferencias.value.single()
    assertEquals("generated-1", created.id)
    assertEquals(Money(15_000, Moneda.COP), created.montoDestino)
    assertEquals("Retiro", created.descripcion)
    assertEquals(NOW, created.createdAt)
  }

  @Test
  fun invalidSubmissionShowsEveryErrorAndWritesNothing() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.New(DOLARES.id, destinoId = null, prefill = null))
    runCurrent()

    f.viewModel.onSubmit()

    val errors = f.form().visibleErrors
    assertNotNull(errors.destino)
    assertNotNull(errors.montoOrigen)
    assertEquals(0, f.transferencias.inserts)
    assertEquals(TransferPhase.Edit, f.session().phase)
  }

  @Test
  fun aFailedSaveKeepsTheDraftAndCanBeRetried() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.New(NEQUI.id, EFECTIVO.id, prefill = null))
    runCurrent()
    f.viewModel.onMontoOrigenChanged("150")
    f.transferencias.fake.failure = DataError.Local(IllegalStateException())

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(TransferError.SaveFailed, f.session().error)
    assertFalse(f.session().isSubmitting)
    assertEquals("150", f.form().draft.montoOrigen)
    f.transferencias.fake.failure = null
    f.viewModel.onSubmit()
    runCurrent()
    assertEquals(TransferPhase.Closing, f.session().phase)
  }

  @Test
  fun editingSavesAndQueuesFeedbackOnlyOnceTheSheetHides() = runTest {
    val existing = stored(NEQUI, EFECTIVO)
    val f = TransferFixture(backgroundScope, initial = listOf(existing))
    f.viewModel.open(TransferEntry.Existing(existing.id))
    runCurrent()
    f.viewModel.onDestinoSelected(NU.id)
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(NU.id, f.transferencias.fake.transferencias.value.single().cuentaDestinoId)
    assertTrue(f.feedback.state.value.feedbackQueue.isEmpty())
    f.viewModel.onSheetHidden(f.session().token)

    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementSubject.Transferencia, feedback.subject)
    assertEquals(ManagementFeedbackKind.Saved, feedback.kind)
  }

  @Test
  fun anUnchangedEditClosesWithoutWriting() = runTest {
    val existing = stored(NEQUI, EFECTIVO)
    val f = TransferFixture(backgroundScope, initial = listOf(existing))
    f.viewModel.open(TransferEntry.Existing(existing.id))
    runCurrent()

    f.viewModel.onSubmit()
    runCurrent()
    f.viewModel.onSheetHidden(f.session().token)

    assertEquals(0, f.transferencias.updates)
    assertTrue(f.feedback.state.value.feedbackQueue.isEmpty())
  }

  @Test
  fun anEditOfATransferDeletedMeanwhileKeepsTheDraftWithAMissingError() = runTest {
    val existing = stored(NEQUI, EFECTIVO)
    val f = TransferFixture(backgroundScope, initial = listOf(existing))
    f.viewModel.open(TransferEntry.Existing(existing.id))
    runCurrent()
    f.viewModel.onDescripcionChanged("Arriendo")
    f.transferencias.fake.transferencias.value = emptyList()

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(TransferError.MissingOnSave, f.session().error)
    assertEquals("Arriendo", f.form().draft.descripcion)
    f.viewModel.onSubmit()
    assertEquals(1, f.transferencias.updates)
  }

  @Test
  fun deletingNeedsConfirmationAndUndoRestoresThroughTheSharedQueue() = runTest {
    val existing = stored(DOLARES, NEQUI)
    val f = TransferFixture(backgroundScope, initial = listOf(existing))
    f.viewModel.open(TransferEntry.Existing(existing.id))
    runCurrent()

    f.viewModel.onRequestDelete()
    assertTrue(f.session().isDeleteConfirmationVisible)
    f.viewModel.onCancelDelete()
    assertFalse(f.session().isDeleteConfirmationVisible)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()

    assertTrue(f.transferencias.fake.transferencias.value.isEmpty())
    assertTrue(f.feedback.state.value.feedbackQueue.isEmpty())
    f.viewModel.onSheetHidden(f.session().token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Deleted, feedback.kind)
    assertEquals(ManagementSubject.Transferencia, feedback.subject)
    assertEquals(
        existing,
        assertIs<DeletedSnapshot.OfTransferencia>(feedback.deleted).transferencia,
    )

    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    assertEquals(ManagementFeedbackKind.Restored, f.feedback.state.value.feedback?.kind)
    assertEquals(listOf(existing), f.transferencias.fake.transferencias.value)
  }

  @Test
  fun aStaleHiddenCallbackCannotEndTheNextSession() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.New(NEQUI.id, EFECTIVO.id, prefill = null))
    runCurrent()
    val first = f.session().token
    assertTrue(f.viewModel.onRequestDismiss())
    f.viewModel.onSheetHidden(first)
    f.viewModel.open(TransferEntry.New(NEQUI.id, EFECTIVO.id, prefill = null))
    runCurrent()

    f.viewModel.onSheetHidden(first)
    assertEquals(TransferPhase.Edit, f.session().phase)
    assertEquals(1, f.feedback.state.value.openSessions)
  }

  @Test
  fun dismissingUnsavedChangesAsksFirstAndContinueKeepsTheDraft() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.New(NEQUI.id, EFECTIVO.id, prefill = null))
    runCurrent()
    f.viewModel.onMontoOrigenChanged("10")

    assertFalse(f.viewModel.onRequestDismiss())
    assertTrue(f.session().isDiscardConfirmationVisible)
    f.viewModel.onContinueEditing()
    assertFalse(f.session().isDiscardConfirmationVisible)
    assertEquals("10", f.form().draft.montoOrigen)

    f.viewModel.onBack()
    assertTrue(f.session().isDiscardConfirmationVisible)
    f.viewModel.onDiscardChanges()
    assertEquals(TransferPhase.Closing, f.session().phase)
  }

  @Test
  fun aMissingTransferShowsMissingAndAFailedReadCanBeRetried() = runTest {
    val f = TransferFixture(backgroundScope)
    f.viewModel.open(TransferEntry.Existing("gone"))
    runCurrent()
    assertEquals(TransferPhase.Missing, f.session().phase)
    f.viewModel.onRequestDismiss()
    f.viewModel.onSheetHidden(f.session().token)

    f.cuentas.failure = DataError.Local(IllegalStateException())
    f.viewModel.open(TransferEntry.New(NEQUI.id, null, null))
    runCurrent()
    assertEquals(TransferPhase.LoadFailed, f.session().phase)
    f.cuentas.failure = null
    f.viewModel.onRetryLoad()
    runCurrent()
    assertEquals(TransferPhase.Edit, f.session().phase)
  }

  @Test
  fun onlyOneSessionOpensAndNoneWhileARestoreRuns() = runTest {
    val existing = stored(NEQUI, EFECTIVO)
    val f = TransferFixture(backgroundScope, initial = listOf(existing))
    assertTrue(f.viewModel.open(TransferEntry.Existing(existing.id)))
    assertFalse(f.viewModel.open(TransferEntry.New(null, null, null)))
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(f.session().token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    f.transferencias.restoreGate = CompletableDeferred()

    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    assertFalse(f.viewModel.open(TransferEntry.New(NEQUI.id, null, null)))
    assertNull(f.viewModel.uiState.value.session)
  }
}

private class TransferFixture(scope: CoroutineScope, initial: List<Transferencia> = emptyList()) {
  val cuentas = FakeCuentaRepository(listOf(EFECTIVO, NEQUI, NU, DOLARES))
  val transferencias =
      GatedTransferencias(FakeTransferenciaRepository(cuentas, TODAY, initial = initial))
  val feedback = ManagementFeedbackViewModel(ManagementRepository(), transferencias, scope).queue
  private var nextId = 0
  val viewModel =
      ManageTransferenciaViewModel(
          repositories =
              ManageTransferenciaRepositories(
                  transferencias = transferencias,
                  cuentas = cuentas,
                  preferences = AccountsPreferences(),
              ),
          idGenerator = IdGenerator { "generated-${++nextId}" },
          clock = MutableClock(NOW),
          timeZoneProvider = TimeZoneProvider { TimeZone.UTC },
          feedback = feedback,
          scope = scope,
      )

  fun session(): TransferSession = assertNotNull(viewModel.uiState.value.session)

  fun form(): TransferDraftState = assertNotNull(session().form)
}

private class GatedTransferencias(val fake: FakeTransferenciaRepository) :
    TransferenciaRepository by fake {
  var insertGate: CompletableDeferred<Unit>? = null
  var restoreGate: CompletableDeferred<Unit>? = null
  var inserts = 0
  var updates = 0

  override suspend fun insert(transferencia: Transferencia): DataOutcome<Unit> {
    inserts++
    insertGate?.await()
    return fake.insert(transferencia)
  }

  override suspend fun update(
      id: String,
      changes: Transferencia,
  ): DataOutcome<TransferenciaUpdateResult> {
    updates++
    return fake.update(id, changes)
  }

  override suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult> {
    restoreGate?.await()
    return fake.restoreDeleted(transferencia)
  }
}

private val TODAY = LocalDate(2026, 10, 16)
private val NOW = Instant.parse("2026-10-16T15:00:00Z")
private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")
private val DEBT = Money(350_000, Moneda.COP)

private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
    Cuenta(
        id,
        id,
        tipo,
        Money(0, moneda),
        null,
        archivada = false,
        createdAt = CREATED_AT,
        color = tipo.defaultColor,
    )

private val EFECTIVO = cuenta(Cuenta.defaultCashId(Moneda.COP), TipoCuenta.EFECTIVO, Moneda.COP)
private val NEQUI = cuenta("nequi", TipoCuenta.BANCO, Moneda.COP)
private val NU = cuenta("nu", TipoCuenta.CREDITO, Moneda.COP)
private val DOLARES = cuenta("dolares", TipoCuenta.BANCO, Moneda.USD)

private fun stored(origen: Cuenta, destino: Cuenta) =
    Transferencia(
        id = "stored-${origen.id}",
        cuentaOrigenId = origen.id,
        cuentaDestinoId = destino.id,
        montoOrigen = Money(10_000, origen.moneda),
        montoDestino =
            if (origen.moneda == destino.moneda) Money(10_000, destino.moneda)
            else Money(40_000_000, destino.moneda),
        fecha = LocalDate(2026, 10, 1),
        descripcion = null,
        createdAt = Instant.parse("2026-10-01T09:00:00Z"),
    )
