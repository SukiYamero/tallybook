package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn

@Suppress("TooManyFunctions")
internal class ManageMovimientoViewModel(
    repositories: ManageMovimientoRepositories,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    private val feedback: ManagementFeedbackQueue,
    scope: CoroutineScope? = null,
) : ViewModel(), ManageMovimientoActions {
  private val movimientoRepository = repositories.movimientos
  private val categoriaRepository = repositories.categorias
  private val etiquetaRepository = repositories.etiquetas
  private val cuentaRepository = repositories.cuentas
  private val coroutineScope = scope ?: viewModelScope
  private var nextToken = 0L
  private var readGeneration = 0L
  private var readJob: Job? = null
  private var categoryJob: Job? = null
  private var etiquetaJob: Job? = null
  private var cuentaJob: Job? = null
  private var defaultsJob: Job? = null
  private var pendingOutcome: ManagementOutcome? = null
  private var feedbackSession: ManagementSessionHandle? = null

  val uiState: StateFlow<ManageMovimientoUiState>
    field = MutableStateFlow(ManageMovimientoUiState())

  override fun open(id: String): Boolean {
    val admission =
        if (id.isBlank() || uiState.value.session != null) null else feedback.openSession()
    feedbackSession = admission ?: return false
    val session = MovimientoManagementSession(token = ++nextToken, movimientoId = id)
    uiState.update { it.copy(session = session) }
    loadCategories(session.token)
    loadEtiquetas(session.token)
    loadCuentas(session.token)
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
    if (session.isCuentasFailed) loadCuentas(session.token)
    if (session.phase == MovimientoManagementPhase.Edit) loadCategories(session.token)
    else {
      if (session.isCategoriesFailed) loadCategories(session.token)
      loadMovimiento(session.token)
    }
  }

  override fun onTipoChanged(tipo: TipoMovimiento) = updateDraft {
    copy(tipo = tipo, medio = medio?.forTipo(tipo))
  }

  override fun onMedioSelected(medio: MedioPago) {
    val session = uiState.value.session ?: return
    val form = session.form?.takeIf { medio != it.draft.medio && medio.allows(it.tipo) } ?: return
    val next =
        form.shownChoice.withMedio(
            medio,
            session.cuentas,
            session.cuentaDefaults,
            moneda = form.moneda,
        )
    updateDraft { copy(medio = next.medio, cuentaId = next.cuentaId) }
  }

  override fun onCuentaSelected(id: String) {
    val form = uiState.value.session?.form ?: return
    val cuenta =
        form.cuentas.firstOrNull { it.id == id && !it.archivada && it.moneda == form.moneda }
            ?: return
    val next = form.shownChoice.withCuenta(cuenta, form.tipo)
    updateDraft { copy(medio = next.medio, cuentaId = next.cuentaId) }
  }

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
        is DataOutcome.Failure -> {
          // Every refusal is invalid input; re-reading the cuenta tells a refused one apart.
          val rejected =
              outcome.error is DataError.InvalidInput &&
                  (cuentaRepository.getById(update.cuentaId) as? DataOutcome.Success)
                      ?.data
                      ?.accepts(
                          update.medio,
                          update.monto.moneda,
                          storedCuentaId = session.edit?.original?.cuentaId,
                      ) != true
          updateSession(session.token) {
            copy(
                isSubmitting = false,
                edit =
                    edit?.copy(
                        error = MovimientoFormError.SAVE_FAILED.takeUnless { rejected },
                        cuentaRejected = rejected,
                    ),
            )
          }
        }
        is DataOutcome.Success ->
            when (outcome.data) {
              MovimientoUpdateResult.Updated ->
                  close(
                      session.token,
                      ManagementOutcome(ManagementSubject.Movimiento, ManagementFeedbackKind.Saved),
                  )
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
                ManagementOutcome(
                    subject = ManagementSubject.Movimiento,
                    kind =
                        if (outcome.data == null) ManagementFeedbackKind.Missing
                        else ManagementFeedbackKind.Deleted,
                    undo = outcome.data?.let(UndoSnapshot::OfMovimiento),
                ),
            )
      }
    }
  }

  override fun onSheetHidden(origin: Long) {
    val session = uiState.value.session ?: return
    if (session.token != origin || session.phase != MovimientoManagementPhase.Closing) return
    stopReads()
    val pending = pendingOutcome
    pendingOutcome = null
    uiState.update { it.copy(session = null) }
    feedbackSession?.close(pending)
    feedbackSession = null
  }

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
          edit =
              edit?.let {
                val draft = transform(it.draft)
                it.copy(
                    draft = draft,
                    error = null,
                    cuentaRejected =
                        it.cuentaRejected &&
                            draft.cuentaId == it.draft.cuentaId &&
                            draft.medio == it.draft.medio,
                )
              },
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
      loadedEdit?.let { loadCuentaDefaults(token, it.original.monto.moneda) }
    }
  }

  private fun loadCuentas(token: Long) {
    cuentaJob?.cancel()
    updateSession(token) { copy(isCuentasLoading = true, isCuentasFailed = false) }
    cuentaJob = coroutineScope.launch {
      cuentaRepository.observeAll().collect { result ->
        updateSession(token) {
          if (phase == MovimientoManagementPhase.Closing) this
          else
              when (result) {
                is DataOutcome.Success ->
                    copy(cuentas = result.data, isCuentasLoading = false, isCuentasFailed = false)
                is DataOutcome.Failure -> copy(isCuentasLoading = false, isCuentasFailed = true)
              }
        }
        uiState.value.session
            ?.takeIf { it.token == token }
            ?.movimiento
            ?.let { loadCuentaDefaults(token, it.monto.moneda) }
      }
    }
  }

  // The stored currency stands in for the primary one, so each fallback prefers it; a default in
  // another currency is still dropped by the selection rules.
  private fun loadCuentaDefaults(token: Long, moneda: Moneda) {
    defaultsJob?.cancel()
    defaultsJob = coroutineScope.launch {
      val defaults = (movimientoRepository.cuentaDefaults(moneda) as? DataOutcome.Success)?.data
      updateSession(token) { copy(cuentaDefaults = defaults) }
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

  private fun close(token: Long, outcome: ManagementOutcome? = null) {
    pendingOutcome = outcome
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
    pendingOutcome = null
    feedbackSession?.close()
    feedbackSession = null
    uiState.value = ManageMovimientoUiState()
  }

  private fun today(): LocalDate = clock.todayIn(timeZoneProvider.current())

  private fun stopReads() {
    readJob?.cancel()
    categoryJob?.cancel()
    etiquetaJob?.cancel()
    cuentaJob?.cancel()
    defaultsJob?.cancel()
    ++readGeneration
  }
}

internal data class ManageMovimientoRepositories(
    val movimientos: MovimientoRepository,
    val categorias: CategoriaRepository,
    val etiquetas: EtiquetaRepository,
    val cuentas: CuentaRepository,
)

private val EditMovimientoUiState.shownChoice: CuentaSelectionState
  get() = CuentaSelectionState(draft.medio, cuentaField.selected?.id, SelectionOwner.USER)

private val MovimientoManagementSession.canInteract: Boolean
  get() = !isSubmitting && phase != MovimientoManagementPhase.Closing

private val MovimientoManagementSession.hasPendingConfirmation: Boolean
  get() = discardTarget != null || isDeleteConfirmationVisible

private val MovimientoManagementSession.canModifyDraft: Boolean
  get() = phase == MovimientoManagementPhase.Edit && canInteract && !hasPendingConfirmation

internal fun Movimiento.toDraft() =
    MovimientoDraft(
        tipo,
        monto.toAmountInput(),
        fecha,
        categoriaId,
        etiquetaIds,
        titulo.orEmpty(),
        descripcion.orEmpty(),
        cuentaId,
        medio,
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
        cuentaId,
        medio,
    )

private fun String?.normalizeOptionalTextOrNull(): String? = this?.normalizeOptionalText()

internal fun EditMovimientoUiState.toChangesOrNull(): MovimientoChanges? {
  val amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money
  val categoria = categoriaId
  val cuenta = cuentaField.selected
  val medio = cuentaField.effectiveMedio(tipo)
  val isReady = !isLoading && error == null && hasValidCategoria && !isFechaInvalid
  if (amount == null || categoria == null || !isReady) return null
  return if (cuenta == null || medio == null) null
  else
      MovimientoChanges(
          tipo,
          amount,
          fecha,
          categoria,
          etiquetaIds,
          titulo.normalizeOptionalText(),
          descripcion.normalizeOptionalText(),
          cuenta.id,
          medio,
      )
}
