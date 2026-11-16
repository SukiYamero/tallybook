package com.kurobello.tallybook.feature.transfers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult
import com.kurobello.tallybook.core.model.primaryMoneda
import com.kurobello.tallybook.core.ui.component.DismissalDecision
import com.kurobello.tallybook.core.ui.component.dismissalDecision
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackQueue
import com.kurobello.tallybook.core.ui.feedback.ManagementOutcome
import com.kurobello.tallybook.core.ui.feedback.ManagementSessionHandle
import com.kurobello.tallybook.core.ui.feedback.ManagementSubject
import com.kurobello.tallybook.core.ui.feedback.UndoSnapshot
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn

internal data class ManageTransferenciaRepositories(
    val transferencias: TransferenciaRepository,
    val cuentas: CuentaRepository,
    val preferences: AppPreferencesRepository,
)

/**
 * Creates, edits and deletes one transfer at a time. Like movimiento management it is admitted by
 * the shell's [ManagementFeedbackQueue] and hands its committed outcome to that queue only once the
 * sheet has hidden, so Undo never appears over the sheet that caused it.
 */
@Suppress("TooManyFunctions")
internal class ManageTransferenciaViewModel(
    repositories: ManageTransferenciaRepositories,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    private val feedback: ManagementFeedbackQueue,
    scope: CoroutineScope? = null,
) : ViewModel(), TransferActions {
  private val transferencias = repositories.transferencias
  private val cuentas = repositories.cuentas
  private val preferences = repositories.preferences
  private val coroutineScope = scope ?: viewModelScope
  private var nextToken = 0L
  private var readGeneration = 0L
  private var readJob: Job? = null
  private var pendingOutcome: ManagementOutcome? = null
  private var feedbackSession: ManagementSessionHandle? = null

  val uiState: StateFlow<TransferUiState>
    field = MutableStateFlow(TransferUiState())

  fun open(entry: TransferEntry): Boolean {
    val admission =
        if (uiState.value.session != null || entry.isBlank) null else feedback.openSession()
    feedbackSession = admission ?: return false
    val session = TransferSession(token = ++nextToken, entry = entry)
    uiState.update { it.copy(session = session) }
    load(session.token)
    return true
  }

  override fun onOrigenSelected(id: String) = updateForm { withOrigen(id) }

  override fun onDestinoSelected(id: String) = updateForm { withDestino(id) }

  override fun onMontoOrigenChanged(value: String) = updateForm { withMontoOrigen(value) }

  override fun onMontoDestinoChanged(value: String) = updateForm { withMontoDestino(value) }

  override fun onFechaChanged(value: LocalDate) = updateForm { withFecha(value) }

  override fun onDescripcionChanged(value: String) = updateForm { withDescripcion(value) }

  override fun onSubmit() {
    val session = uiState.value.session?.takeIf { it.canSubmit } ?: return
    val form = session.form?.copy(submitAttempted = true, today = today()) ?: return
    updateSession(session.token) { copy(form = form, error = null) }
    val original = form.original
    val transferencia =
        form.toTransferencia(
            id = original?.id ?: idGenerator.next(),
            createdAt = original?.createdAt ?: clock.now(),
        )
    when {
      transferencia == null -> Unit
      transferencia == original -> close(session.token)
      else -> {
        updateSession(session.token) { copy(isSubmitting = true) }
        coroutineScope.launch {
          if (original == null) insert(session.token, transferencia)
          else update(session.token, transferencia)
        }
      }
    }
  }

  private suspend fun insert(token: Long, transferencia: Transferencia) {
    when (transferencias.insert(transferencia)) {
      is DataOutcome.Failure -> failSubmission(token, TransferError.SaveFailed)
      is DataOutcome.Success -> close(token)
    }
  }

  private suspend fun update(token: Long, transferencia: Transferencia) {
    when (val outcome = transferencias.update(transferencia.id, transferencia)) {
      is DataOutcome.Failure -> failSubmission(token, TransferError.SaveFailed)
      is DataOutcome.Success ->
          when (outcome.data) {
            TransferenciaUpdateResult.Updated ->
                close(token, ManagementOutcome(SUBJECT, ManagementFeedbackKind.Saved))
            TransferenciaUpdateResult.NotFound -> failSubmission(token, TransferError.MissingOnSave)
          }
    }
  }

  private fun failSubmission(token: Long, error: TransferError) =
      updateSession(token) { copy(isSubmitting = false, error = error) }

  override fun onRequestDelete() {
    val session = uiState.value.session?.takeIf { it.canDelete } ?: return
    updateSession(session.token) { copy(isDeleteConfirmationVisible = true, error = null) }
  }

  override fun onCancelDelete() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.isDeleteConfirmationVisible)
        updateSession(session.token) { copy(isDeleteConfirmationVisible = false) }
  }

  override fun onConfirmDelete() {
    val session =
        uiState.value.session?.takeIf { it.isDeleteConfirmationVisible && it.canInteract } ?: return
    val entry = session.entry as? TransferEntry.Existing ?: return
    updateSession(session.token) { copy(isSubmitting = true, isDeleteConfirmationVisible = false) }
    coroutineScope.launch {
      when (val outcome = transferencias.deleteAndCapture(entry.id)) {
        is DataOutcome.Failure -> failSubmission(session.token, TransferError.DeleteFailed)
        is DataOutcome.Success ->
            close(
                session.token,
                ManagementOutcome(
                    subject = SUBJECT,
                    kind =
                        if (outcome.data == null) ManagementFeedbackKind.Missing
                        else ManagementFeedbackKind.Deleted,
                    undo = outcome.data?.let(UndoSnapshot::OfTransferencia),
                ),
            )
      }
    }
  }

  override fun onRequestDismiss(): Boolean {
    val session =
        uiState.value.session?.takeIf { it.canInteract && !it.isDiscardConfirmationVisible }
            ?: return false
    return if (session.isDeleteConfirmationVisible) {
      onCancelDelete()
      false
    } else {
      requestClose(session)
    }
  }

  private fun requestClose(session: TransferSession): Boolean {
    val unsaved = session.phase == TransferPhase.Edit && session.form?.hasUnsavedChanges == true
    return when (dismissalDecision(unsaved)) {
      DismissalDecision.RequestConfirmation -> {
        updateSession(session.token) { copy(isDiscardConfirmationVisible = true) }
        false
      }
      DismissalDecision.Dismiss -> {
        close(session.token)
        true
      }
    }
  }

  override fun onBack() {
    val session = uiState.value.session ?: return
    when {
      session.isSubmitting -> Unit
      session.isDiscardConfirmationVisible -> onContinueEditing()
      else -> onRequestDismiss()
    }
  }

  override fun onContinueEditing() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.isDiscardConfirmationVisible)
        updateSession(session.token) { copy(isDiscardConfirmationVisible = false) }
  }

  override fun onDiscardChanges() {
    val session =
        uiState.value.session?.takeIf { it.canInteract && it.isDiscardConfirmationVisible }
            ?: return
    close(session.token)
  }

  override fun onRetryLoad() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.phase == TransferPhase.LoadFailed) load(session.token)
  }

  override fun onSheetHidden(origin: Long) {
    val session = uiState.value.session ?: return
    if (session.token != origin || session.phase != TransferPhase.Closing) return
    stopReads()
    val pending = pendingOutcome
    pendingOutcome = null
    uiState.update { it.copy(session = null) }
    feedbackSession?.close(pending)
    feedbackSession = null
  }

  private fun load(token: Long) {
    val session = uiState.value.session?.takeIf { it.token == token } ?: return
    readJob?.cancel()
    val generation = ++readGeneration
    updateSession(token) { copy(phase = TransferPhase.Loading, form = null, error = null) }
    readJob = coroutineScope.launch {
      val loaded = read(session.entry)
      if (generation != readGeneration) return@launch
      updateSession(token) {
        when (loaded) {
          ReadResult.Failed -> copy(phase = TransferPhase.LoadFailed)
          ReadResult.Missing -> copy(phase = TransferPhase.Missing)
          is ReadResult.Ready -> copy(phase = TransferPhase.Edit, form = loaded.form)
        }
      }
    }
  }

  private suspend fun read(entry: TransferEntry): ReadResult {
    val primary = (preferences.preferences.first().primaryMoneda() as? DataOutcome.Success)?.data
    val live = (cuentas.observeAll().first() as? DataOutcome.Success)?.data
    return if (primary == null || live == null) ReadResult.Failed
    else
        when (entry) {
          is TransferEntry.New -> readNew(entry, live, primary)
          is TransferEntry.Existing -> readExisting(entry, live, primary)
        }
  }

  private suspend fun readExisting(
      entry: TransferEntry.Existing,
      live: List<Cuenta>,
      primary: Moneda,
  ): ReadResult =
      when (val stored = transferencias.getById(entry.id)) {
        is DataOutcome.Failure -> ReadResult.Failed
        is DataOutcome.Success ->
            stored.data?.let {
              ReadResult.Ready(TransferDraftState.forEdit(live, primary, it, today()))
            } ?: ReadResult.Missing
      }

  private suspend fun readNew(
      entry: TransferEntry.New,
      live: List<Cuenta>,
      primary: Moneda,
  ): ReadResult {
    val origenId =
        entry.origenId
            ?: when (val default = transferencias.defaultOrigin(primary)) {
              is DataOutcome.Failure -> return ReadResult.Failed
              is DataOutcome.Success -> default.data?.takeIf { it != entry.destinoId }
            }
    val resolved = entry.copy(origenId = origenId)
    return ReadResult.Ready(TransferDraftState.forNew(live, primary, resolved, today()))
  }

  private fun updateForm(transform: TransferDraftState.() -> TransferDraftState) {
    val session = uiState.value.session?.takeIf { it.canEdit } ?: return
    updateSession(session.token) {
      copy(
          form = form?.transform(),
          error = error?.takeIf { it == TransferError.MissingOnSave },
      )
    }
  }

  private fun close(token: Long, outcome: ManagementOutcome? = null) {
    pendingOutcome = outcome
    readJob?.cancel()
    ++readGeneration
    updateSession(token) {
      copy(
          phase = TransferPhase.Closing,
          isSubmitting = false,
          isDeleteConfirmationVisible = false,
          isDiscardConfirmationVisible = false,
      )
    }
  }

  private fun updateSession(token: Long, transform: TransferSession.() -> TransferSession) {
    uiState.update { current ->
      current.session?.takeIf { it.token == token }?.let { current.copy(session = transform(it)) }
          ?: current
    }
  }

  override fun onCleared() {
    stopReads()
    pendingOutcome = null
    feedbackSession?.close()
    feedbackSession = null
    uiState.value = TransferUiState()
  }

  private fun today(): LocalDate = clock.todayIn(timeZoneProvider.current())

  private fun stopReads() {
    readJob?.cancel()
    ++readGeneration
  }
}

private val SUBJECT = ManagementSubject.Transferencia

private val TransferEntry.isBlank: Boolean
  get() = this is TransferEntry.Existing && id.isBlank()

private sealed interface ReadResult {
  data object Failed : ReadResult

  data object Missing : ReadResult

  data class Ready(val form: TransferDraftState) : ReadResult
}
