package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.ui.component.DismissalDecision
import com.kurobello.tallybook.core.ui.component.dismissalDecision
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn

// One owner serializes the sheet lifecycle and the Undo mutation that outlives that sheet.
@Suppress("TooManyFunctions")
internal class ManageMovimientoViewModel(
    private val movimientoRepository: MovimientoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val etiquetaRepository: EtiquetaRepository,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    scope: CoroutineScope? = null,
) : ViewModel(), ManageMovimientoActions {
  private val coroutineScope = scope ?: viewModelScope
  private var nextToken = 0L
  private var readGeneration = 0L
  private var readJob: Job? = null
  private var categoryJob: Job? = null
  private var etiquetaJob: Job? = null
  private var pendingFeedback: PendingMovimientoFeedback? = null

  val uiState: StateFlow<ManageMovimientoUiState>
    field = MutableStateFlow(ManageMovimientoUiState())

  override fun open(id: String): Boolean {
    if (id.isBlank() || uiState.value.session != null || uiState.value.isRestoring) return false
    val session = MovimientoManagementSession(token = ++nextToken, movimientoId = id)
    uiState.update { it.copy(session = session) }
    loadCategories(session.token)
    loadEtiquetas(session.token)
    loadMovimiento(session.token)
    return true
  }

  override fun onRetryLoad() {
    val session = uiState.value.session ?: return
    if (
        !session.canInteract ||
            session.phase == MovimientoManagementPhase.Loading ||
            session.hasPendingConfirmation
    )
        return
    if (session.phase == MovimientoManagementPhase.Edit) loadCategories(session.token)
    else {
      if (session.isCategoriesFailed) loadCategories(session.token)
      loadMovimiento(session.token)
    }
  }

  override fun onTipoChanged(tipo: TipoMovimiento) = updateDraft { copy(tipo = tipo) }

  override fun onAmountChanged(value: String) = updateDraft { copy(amountInput = value) }

  override fun onFechaChanged(value: LocalDate) = updateDraft { copy(fecha = value) }

  override fun onTituloChanged(value: String) = updateDraft {
    copy(titulo = value.take(MOVIMIENTO_TITLE_MAX_LENGTH))
  }

  override fun onDescripcionChanged(value: String) = updateDraft {
    copy(descripcion = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH))
  }

  override fun onCategoriaSelected(id: String) {
    val catalog = uiState.value.session?.categorias ?: return
    if (isSelectableCategoria(id, catalog)) updateDraft { copy(categoriaId = id) }
  }

  override fun onEtiquetasChanged(ids: List<String>) {
    if (uiState.value.session?.etiquetasLoad != EtiquetasLoad.READY) return
    updateDraft { copy(etiquetaIds = ids.toEtiquetaSelection()) }
  }

  override fun onEtiquetaRemoved(id: String) = updateDraft { copy(etiquetaIds = etiquetaIds - id) }

  override fun onCreateEtiqueta(nombre: String) {
    val session = uiState.value.session?.takeIf { it.canModifyDraft } ?: return
    updateSession(session.token) { copy(edit = edit?.copy(failedEtiquetaNombre = null)) }
    coroutineScope.launch {
      val outcome = etiquetaRepository.create(nombre)
      if (uiState.value.session?.token != session.token) return@launch
      when (outcome) {
        is DataOutcome.Success ->
            updateDraft { copy(etiquetaIds = etiquetaIds.withCreated(outcome.data)) }
        is DataOutcome.Failure ->
            updateSession(session.token) { copy(edit = edit?.copy(failedEtiquetaNombre = nombre)) }
      }
    }
  }

  override fun onRetryEtiquetas() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.etiquetasLoad == EtiquetasLoad.UNAVAILABLE)
        loadEtiquetas(session.token)
  }

  override fun onCancelEdit() {
    val session = uiState.value.session ?: return
    if (!session.canModifyDraft) return
    requestLeaveEdit(session)
  }

  override fun onRequestDismiss(): Boolean {
    val session =
        uiState.value.session?.takeIf { it.canInteract && it.discardTarget == null } ?: return false
    return when {
      session.isDeleteConfirmationVisible -> {
        onCancelDelete()
        false
      }
      session.phase == MovimientoManagementPhase.Edit -> requestLeaveEdit(session)
      else -> {
        close(session.token)
        true
      }
    }
  }

  override fun onBack() {
    val session = uiState.value.session ?: return
    when {
      session.isSubmitting -> Unit
      session.discardTarget != null -> onContinueEditing()
      session.isDeleteConfirmationVisible -> onCancelDelete()
      session.phase == MovimientoManagementPhase.Edit -> onCancelEdit()
      else -> onRequestDismiss()
    }
  }

  override fun onContinueEditing() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.discardTarget != null)
        updateSession(session.token) { copy(discardTarget = null) }
  }

  override fun onDiscardChanges() {
    val session =
        uiState.value.session?.takeIf { it.canInteract && it.discardTarget != null } ?: return
    close(session.token)
  }

  override fun onSubmit() {
    val session =
        uiState.value.session?.takeIf {
          it.canModifyDraft && it.error != MovimientoManagementError.MissingOnSave
        } ?: return
    val draft = session.edit ?: return
    val validated = draft.copy(submitAttempted = true, validationDate = today(), error = null)
    val ready = session.copy(edit = validated, error = null)
    updateSession(session.token) { ready }
    val changes = ready.form?.toChangesOrNull()
    when {
      changes == null -> Unit
      changes == validated.original.toChanges() -> close(session.token)
      else -> {
        updateSession(session.token) { copy(isSubmitting = true) }
        submitUpdate(session, changes)
      }
    }
  }

  private fun submitUpdate(session: MovimientoManagementSession, changes: MovimientoChanges) {
    coroutineScope.launch {
      val update = changes.withoutDeletedEtiquetas()
      when (val outcome = movimientoRepository.update(session.movimientoId, update)) {
        is DataOutcome.Failure ->
            updateSession(session.token) {
              copy(isSubmitting = false, edit = edit?.copy(error = MovimientoFormError.SAVE_FAILED))
            }
        is DataOutcome.Success ->
            when (outcome.data) {
              MovimientoUpdateResult.Updated ->
                  close(session.token, PendingMovimientoFeedback(MovimientoFeedbackKind.Saved))
              MovimientoUpdateResult.NotFound ->
                  updateSession(session.token) {
                    copy(isSubmitting = false, error = MovimientoManagementError.MissingOnSave)
                  }
            }
      }
    }
  }

  // A tag deleted while the editor is open would make the repository refuse the whole save; it
  // simply leaves the movimiento instead. An unreadable catalog defers to the repository's check.
  private suspend fun MovimientoChanges.withoutDeletedEtiquetas(): MovimientoChanges {
    if (etiquetaIds.isEmpty()) return this
    val existing =
        (etiquetaRepository.getAll() as? DataOutcome.Success)
            ?.data
            ?.mapTo(mutableSetOf(), Etiqueta::id)
    return if (existing == null) this else copy(etiquetaIds = etiquetaIds.filter { it in existing })
  }

  override fun onRequestDelete() {
    val session = uiState.value.session ?: return
    if (
        !session.canModifyDraft ||
            session.movimiento == null ||
            session.error == MovimientoManagementError.MissingOnSave
    )
        return
    updateSession(session.token) { copy(isDeleteConfirmationVisible = true, error = null) }
  }

  override fun onCancelDelete() {
    val session = uiState.value.session ?: return
    if (session.canInteract && session.isDeleteConfirmationVisible)
        updateSession(session.token) { copy(isDeleteConfirmationVisible = false) }
  }

  override fun onConfirmDelete() {
    val session = uiState.value.session ?: return
    if (!session.isDeleteConfirmationVisible || !session.canInteract) return
    updateSession(session.token) { copy(isSubmitting = true, isDeleteConfirmationVisible = false) }
    coroutineScope.launch {
      when (val outcome = movimientoRepository.deleteAndCapture(session.movimientoId)) {
        is DataOutcome.Failure ->
            updateSession(session.token) {
              copy(isSubmitting = false, error = MovimientoManagementError.DeleteFailed)
            }
        is DataOutcome.Success ->
            close(
                session.token,
                PendingMovimientoFeedback(
                    kind =
                        if (outcome.data == null) MovimientoFeedbackKind.Missing
                        else MovimientoFeedbackKind.Deleted,
                    deleted = outcome.data,
                ),
            )
      }
    }
  }

  override fun onSheetHidden(origin: Long) {
    val session = uiState.value.session ?: return
    if (session.token != origin || session.phase != MovimientoManagementPhase.Closing) return
    stopReads()
    val pending = pendingFeedback
    pendingFeedback = null
    if (pending != null) {
      val feedback =
          MovimientoFeedback(
              token = ++nextToken,
              kind = pending.kind,
              deletedMovimiento = pending.deleted,
          )
      uiState.update { it.copy(session = null, feedbackQueue = it.feedbackQueue + feedback) }
    } else uiState.update { it.copy(session = null) }
  }

  override fun onFeedbackAction(token: Long, revision: Int) {
    val current = uiState.value
    val feedback =
        feedbackFor(token, revision, current)?.takeIf { it.hasAction && current.session == null }
            ?: return
    val deleted = feedback.deletedMovimiento ?: return
    val claimed = feedback.copy(revision = revision + 1, isRestoring = true)
    if (
        uiState.compareAndSet(
            current,
            current.copy(feedbackQueue = listOf(claimed) + current.feedbackQueue.drop(1)),
        )
    ) {
      coroutineScope.launch {
        val outcome = movimientoRepository.restoreDeleted(deleted)
        val kind =
            when (outcome) {
              is DataOutcome.Failure -> MovimientoFeedbackKind.RestoreFailed
              is DataOutcome.Success ->
                  when (outcome.data) {
                    MovimientoRestoreResult.Restored,
                    MovimientoRestoreResult.AlreadyPresent -> MovimientoFeedbackKind.Restored
                    MovimientoRestoreResult.Conflict -> MovimientoFeedbackKind.RestoreConflict
                  }
            }
        val completed =
            claimed.copy(
                revision = claimed.revision + 1,
                kind = kind,
                isRestoring = false,
                deletedMovimiento = deleted.takeIf { kind == MovimientoFeedbackKind.RestoreFailed },
            )
        uiState.update { latest ->
          if (latest.feedback == claimed)
              latest.copy(feedbackQueue = listOf(completed) + latest.feedbackQueue.drop(1))
          else latest
        }
      }
    }
  }

  override fun onFeedbackDismiss(token: Long, revision: Int) {
    val feedback = feedbackFor(token, revision) ?: return
    uiState.update {
      if (it.feedback == feedback) it.copy(feedbackQueue = it.feedbackQueue.drop(1)) else it
    }
  }

  private fun feedbackFor(
      token: Long,
      revision: Int,
      current: ManageMovimientoUiState = uiState.value,
  ): MovimientoFeedback? =
      current.feedback?.takeIf { it.token == token && it.revision == revision && !it.isRestoring }

  private fun requestLeaveEdit(session: MovimientoManagementSession): Boolean =
      when (dismissalDecision(session.hasUnsavedChanges)) {
        DismissalDecision.RequestConfirmation -> {
          updateSession(session.token) { copy(discardTarget = MovimientoDiscardTarget.Close) }
          false
        }
        DismissalDecision.Dismiss -> {
          close(session.token)
          true
        }
      }

  private fun updateDraft(transform: MovimientoDraft.() -> MovimientoDraft) {
    val session = uiState.value.session ?: return
    if (!session.canModifyDraft) return
    updateSession(session.token) {
      copy(
          edit = edit?.let { it.copy(draft = transform(it.draft), error = null) },
          error = error?.takeIf { it == MovimientoManagementError.MissingOnSave },
      )
    }
  }

  private fun loadMovimiento(token: Long) {
    val session = uiState.value.session ?: return
    if (session.token != token) return
    readJob?.cancel()
    val generation = ++readGeneration
    updateSession(token) {
      copy(
          phase = MovimientoManagementPhase.Loading,
          movimiento = null,
          edit = null,
          error = null,
          discardTarget = null,
      )
    }
    readJob = coroutineScope.launch {
      val result = movimientoRepository.getById(session.movimientoId)
      if (generation != readGeneration) return@launch
      val loadedEdit =
          when (result) {
            is DataOutcome.Failure -> null
            is DataOutcome.Success ->
                result.data?.let {
                  EditMovimientoDraftState(
                      original = it,
                      draft = it.toDraft(),
                      maxSelectableDate = today(),
                  )
                }
          }
      updateSession(token) {
        if (phase == MovimientoManagementPhase.Closing) this
        else
            when (result) {
              is DataOutcome.Failure -> copy(phase = MovimientoManagementPhase.LoadFailed)
              is DataOutcome.Success ->
                  copy(
                      phase =
                          if (loadedEdit == null) MovimientoManagementPhase.Missing
                          else MovimientoManagementPhase.Edit,
                      movimiento = result.data,
                      edit = loadedEdit,
                  )
            }
      }
    }
  }

  private fun loadCategories(token: Long) {
    categoryJob?.cancel()
    updateSession(token) { copy(isCategoriesLoading = true, isCategoriesFailed = false) }
    categoryJob = coroutineScope.launch {
      categoriaRepository.observeAll().collect { result ->
        updateSession(token) {
          if (phase == MovimientoManagementPhase.Closing) this
          else
              when (result) {
                is DataOutcome.Success ->
                    copy(
                        categorias = result.data,
                        isCategoriesLoading = false,
                        isCategoriesFailed = false,
                    )
                is DataOutcome.Failure ->
                    copy(isCategoriesLoading = false, isCategoriesFailed = true)
              }
        }
      }
    }
  }

  private fun loadEtiquetas(token: Long) {
    etiquetaJob?.cancel()
    updateSession(token) { copy(etiquetasLoad = EtiquetasLoad.LOADING) }
    etiquetaJob = coroutineScope.launch {
      etiquetaRepository.observeAll().collect { result ->
        updateSession(token) {
          if (phase == MovimientoManagementPhase.Closing) this
          else
              when (result) {
                is DataOutcome.Success ->
                    copy(etiquetas = result.data, etiquetasLoad = EtiquetasLoad.READY)
                is DataOutcome.Failure -> copy(etiquetasLoad = EtiquetasLoad.UNAVAILABLE)
              }
        }
      }
    }
  }

  private fun close(token: Long, feedback: PendingMovimientoFeedback? = null) {
    pendingFeedback = feedback
    readJob?.cancel()
    ++readGeneration
    updateSession(token) {
      copy(
          phase = MovimientoManagementPhase.Closing,
          isSubmitting = false,
          isDeleteConfirmationVisible = false,
          discardTarget = null,
      )
    }
  }

  private fun updateSession(
      token: Long,
      transform: MovimientoManagementSession.() -> MovimientoManagementSession,
  ) {
    uiState.update { current ->
      current.session?.takeIf { it.token == token }?.let { current.copy(session = transform(it)) }
          ?: current
    }
  }

  override fun onCleared() {
    stopReads()
    pendingFeedback = null
    uiState.value = ManageMovimientoUiState()
  }

  private fun today(): LocalDate = clock.todayIn(timeZoneProvider.current())

  private fun stopReads() {
    readJob?.cancel()
    categoryJob?.cancel()
    etiquetaJob?.cancel()
    ++readGeneration
  }
}

private data class PendingMovimientoFeedback(
    val kind: MovimientoFeedbackKind,
    val deleted: Movimiento? = null,
)

private val MovimientoManagementSession.canInteract: Boolean
  get() = !isSubmitting && phase != MovimientoManagementPhase.Closing

private val MovimientoManagementSession.hasPendingConfirmation: Boolean
  get() = discardTarget != null || isDeleteConfirmationVisible

private val MovimientoManagementSession.canModifyDraft: Boolean
  get() = phase == MovimientoManagementPhase.Edit && canInteract && !hasPendingConfirmation

internal fun Money.toAmountInput(): String {
  val digits = minorUnits.toString().padStart(moneda.scale + 1, '0')
  return if (moneda.scale == 0) digits
  else digits.dropLast(moneda.scale) + moneda.decimalSeparator + digits.takeLast(moneda.scale)
}

internal fun Movimiento.toDraft() =
    MovimientoDraft(
        tipo,
        monto.toAmountInput(),
        fecha,
        categoriaId,
        etiquetaIds,
        titulo.orEmpty(),
        descripcion.orEmpty(),
    )

private fun Movimiento.toChanges() =
    MovimientoChanges(
        tipo,
        monto,
        fecha,
        categoriaId,
        etiquetaIds,
        titulo.normalizeOptionalTextOrNull(),
        descripcion.normalizeOptionalTextOrNull(),
    )

private fun String?.normalizeOptionalTextOrNull(): String? = this?.normalizeOptionalText()

private fun EditMovimientoUiState.toChangesOrNull(): MovimientoChanges? {
  val amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money
  val categoria = categoriaId
  val isReady = !isLoading && error == null && hasValidCategoria && !isFechaInvalid
  return if (amount == null || categoria == null || !isReady) null
  else
      MovimientoChanges(
          tipo,
          amount,
          fecha,
          categoria,
          etiquetaIds,
          titulo.normalizeOptionalText(),
          descripcion.normalizeOptionalText(),
      )
}
