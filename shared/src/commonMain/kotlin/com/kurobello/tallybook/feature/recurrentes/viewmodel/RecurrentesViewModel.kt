package com.kurobello.tallybook.feature.recurrentes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.PresupuestoRepository
import com.kurobello.tallybook.core.data.RecurrenciaRepository
import com.kurobello.tallybook.core.data.noticeAfterSave
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.OccurrenceResult
import com.kurobello.tallybook.core.model.RecurrenciaDraft
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.RecurrenciaWrite
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.budgetTouch
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.feedback.ManagementOutcome
import com.kurobello.tallybook.core.ui.feedback.ManagementSubject
import com.kurobello.tallybook.core.ui.feedback.UndoSnapshot
import com.kurobello.tallybook.feature.movimientos.viewmodel.CuentaSelectionState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoDraft
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import com.kurobello.tallybook.feature.movimientos.viewmodel.SelectionOwner
import com.kurobello.tallybook.feature.movimientos.viewmodel.isSelectableCategoria
import com.kurobello.tallybook.feature.movimientos.viewmodel.toChangesOrNull
import com.kurobello.tallybook.feature.movimientos.viewmodel.toEtiquetaSelection
import com.kurobello.tallybook.feature.movimientos.viewmodel.withCreated
import com.kurobello.tallybook.feature.movimientos.viewmodel.withCuenta
import com.kurobello.tallybook.feature.movimientos.viewmodel.withMedio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal data class RecurrentesRepositories(
    val recurrencias: RecurrenciaRepository,
    val categorias: CategoriaRepository,
    val cuentas: CuentaRepository,
    val etiquetas: EtiquetaRepository,
    val presupuestos: PresupuestoRepository,
)

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class RecurrentesViewModel(
    private val repositories: RecurrentesRepositories,
    private val today: CivilToday,
    scope: CoroutineScope? = null,
) : ViewModel(), RecurrentesActions {
  private val coroutineScope = scope ?: viewModelScope
  private val currentDay = MutableStateFlow(today.date())
  private val contentRetries = MutableStateFlow(0)
  private val cuentaRetries = MutableStateFlow(0)
  private val etiquetaRetries = MutableStateFlow(0)
  private var pendingOutcome: ManagementOutcome? = null

  val uiState: StateFlow<RecurrentesUiState>
    field = MutableStateFlow(RecurrentesUiState(today = currentDay.value))

  private val effectChannel = Channel<RecurrentesEffect>(Channel.BUFFERED)
  val effects: Flow<RecurrentesEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch { observeContent() }
    coroutineScope.launch { observeCuentas() }
    coroutineScope.launch { observeEtiquetas() }
  }

  /** Every resume reads the day again, so occurrences that fell due meanwhile appear. */
  fun onResume() {
    currentDay.value = today.date()
  }

  override fun onRetry() {
    if (uiState.value.content != RecurrentesContent.Failed) return
    uiState.update { it.copy(content = RecurrentesContent.Loading) }
    contentRetries.update { it + 1 }
  }

  override fun onOpenOccurrence(recurrenciaId: String): Boolean {
    val state = uiState.value
    val occurrence =
        state.ready?.pending?.firstOrNull { it.recurrencia.id == recurrenciaId && it.isNext }
    if (state.sheet != null || occurrence == null) return false
    uiState.update {
      it.copy(sheet = OccurrenceSheet(occurrence.recurrencia, occurrence.dueDate, state.today))
    }
    return true
  }

  override fun onOpenRecurrencia(recurrenciaId: String): Boolean {
    val state = uiState.value
    val recurrencia = state.ready?.scheduled?.firstOrNull { it.id == recurrenciaId }
    if (state.sheet != null || recurrencia == null) return false
    uiState.update { it.copy(sheet = RecurrenciaSheet.of(recurrencia, state.today)) }
    return true
  }

  override fun onOccurrenceAmountChanged(value: String) = updateOccurrence {
    copy(amountInput = value, error = null)
  }

  override fun onOccurrenceFechaChanged(value: LocalDate) = updateOccurrence {
    if (value > this.today) this else copy(fecha = value, error = null)
  }

  override fun onPaid() {
    val sheet = (uiState.value.sheet as? OccurrenceSheet)?.takeIf { it.isEditable } ?: return
    val validated = sheet.copy(submitAttempted = true, error = null)
    val monto = validated.monto
    uiState.update {
      it.copy(sheet = validated.copy(busy = OccurrenceAction.CONFIRM.takeIf { monto != null }))
    }
    if (monto == null) return
    val template = sheet.recurrencia
    val touch =
        budgetTouch(
            template.tipo,
            template.categoriaId,
            monto.moneda,
            validated.fecha,
            categorias(),
        )
    coroutineScope.launch {
      val day = today.date()
      val outcome = repositories.recurrencias.confirm(template.id, day, monto, validated.fecha)
      val confirmed = (outcome as? DataOutcome.Success)?.data is OccurrenceResult.Done
      val budget =
          if (confirmed) repositories.presupuestos.noticeAfterSave(listOfNotNull(touch), day)
          else null
      finishOccurrence(outcome, ManagementFeedbackKind.Confirmed, budget) { error ->
        if (error is DataError.InvalidInput) OccurrenceError.TEMPLATE_INVALID
        else OccurrenceError.CONFIRM_FAILED
      }
    }
  }

  override fun onSkip() {
    val sheet = (uiState.value.sheet as? OccurrenceSheet)?.takeIf { it.isEditable } ?: return
    uiState.update { it.copy(sheet = sheet.copy(busy = OccurrenceAction.SKIP, error = null)) }
    coroutineScope.launch {
      val outcome = repositories.recurrencias.skip(sheet.recurrencia.id, today.date())
      finishOccurrence(outcome, ManagementFeedbackKind.Skipped) { OccurrenceError.SKIP_FAILED }
    }
  }

  override fun onNotYet() {
    if ((uiState.value.sheet as? OccurrenceSheet)?.isEditable == true) close()
  }

  override fun onEditFromOccurrence() {
    val state = uiState.value
    val sheet = (state.sheet as? OccurrenceSheet)?.takeIf { it.isEditable } ?: return
    val recurrencia =
        state.ready?.scheduled?.firstOrNull { it.id == sheet.recurrencia.id } ?: sheet.recurrencia
    uiState.update { it.copy(sheet = RecurrenciaSheet.of(recurrencia, state.today)) }
  }

  private fun finishOccurrence(
      outcome: DataOutcome<OccurrenceResult>,
      kind: ManagementFeedbackKind,
      budget: BudgetNotice? = null,
      errorOf: (DataError) -> OccurrenceError,
  ) {
    when (outcome) {
      is DataOutcome.Failure ->
          updateOccurrence(force = true) { copy(busy = null, error = errorOf(outcome.error)) }
      is DataOutcome.Success ->
          when (val result = outcome.data) {
            is OccurrenceResult.Done ->
                close(
                    ManagementOutcome(
                        ManagementSubject.Recurrencia,
                        kind,
                        UndoSnapshot.OfOccurrence(result.undo),
                        budget,
                    )
                )
            OccurrenceResult.NothingPending -> close()
          }
    }
  }

  override fun onTipoChanged(tipo: TipoMovimiento) = updateDraft {
    copy(tipo = tipo, medio = medio?.forTipo(tipo))
  }

  override fun onAmountChanged(value: String) = updateDraft { copy(amountInput = value) }

  override fun onFechaChanged(value: LocalDate) {
    if (!isScheduleLocked()) updateDraft { copy(fecha = value) }
  }

  override fun onFrecuenciaChanged(frecuencia: Frecuencia) {
    if (!isScheduleLocked()) updateRecurrenciaSheet { copy(frecuencia = frecuencia) }
  }

  override fun onCategoriaSelected(id: String) {
    if (isSelectableCategoria(id, categorias())) updateDraft { copy(categoriaId = id) }
  }

  override fun onTituloChanged(value: String) = updateDraft {
    copy(titulo = value.take(MOVIMIENTO_TITLE_MAX_LENGTH))
  }

  override fun onDescripcionChanged(value: String) = updateDraft {
    copy(descripcion = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH))
  }

  override fun onEtiquetasChanged(ids: List<String>) {
    if (uiState.value.catalog.etiquetasLoad != EtiquetasLoad.READY) return
    updateDraft { copy(etiquetaIds = ids.toEtiquetaSelection()) }
  }

  override fun onEtiquetaRemoved(id: String) = updateDraft { copy(etiquetaIds = etiquetaIds - id) }

  override fun onCreateEtiqueta(nombre: String) {
    val sheet = (uiState.value.sheet as? RecurrenciaSheet)?.takeIf { it.canInteract } ?: return
    updateRecurrenciaSheet { copy(edit = edit.copy(failedEtiquetaNombre = null)) }
    coroutineScope.launch {
      val outcome = repositories.etiquetas.create(nombre)
      if (uiState.value.sheet?.recurrencia?.id != sheet.recurrencia.id) return@launch
      when (outcome) {
        is DataOutcome.Success ->
            updateDraft { copy(etiquetaIds = etiquetaIds.withCreated(outcome.data)) }
        is DataOutcome.Failure ->
            updateRecurrenciaSheet { copy(edit = edit.copy(failedEtiquetaNombre = nombre)) }
      }
    }
  }

  override fun onRetryEtiquetas() {
    if (uiState.value.catalog.etiquetasLoad == EtiquetasLoad.UNAVAILABLE)
        etiquetaRetries.update { it + 1 }
  }

  override fun onMedioSelected(medio: MedioPago) {
    val form = recurrenciaForm() ?: return
    if (medio == form.draft.medio || !medio.allows(form.tipo)) return
    val next =
        form.shownChoice.withMedio(medio, form.cuentas, defaults = null, moneda = form.moneda)
    updateDraft { copy(medio = next.medio, cuentaId = next.cuentaId) }
  }

  override fun onCuentaSelected(id: String) {
    val form = recurrenciaForm() ?: return
    val cuenta =
        form.cuentas.firstOrNull { it.id == id && !it.archivada && it.moneda == form.moneda }
            ?: return
    val next = form.shownChoice.withCuenta(cuenta, form.tipo)
    updateDraft { copy(medio = next.medio, cuentaId = next.cuentaId) }
  }

  override fun onRetryLoad() {
    if (uiState.value.catalog.isCuentasFailed) cuentaRetries.update { it + 1 }
  }

  override fun onSubmit() {
    val state = uiState.value
    val sheet = (state.sheet as? RecurrenciaSheet)?.takeIf { it.canInteract } ?: return
    val validated = sheet.copy(edit = sheet.edit.copy(submitAttempted = true, error = null))
    uiState.update { it.copy(sheet = validated) }
    val changes = validated.form(state.catalog, categorias()).toChangesOrNull()
    when {
      changes == null -> Unit
      !validated.hasChanges -> close()
      else -> save(sheet, changes.toRecurrenciaDraft(validated, state))
    }
  }

  private fun save(sheet: RecurrenciaSheet, draft: RecurrenciaDraft) {
    updateRecurrenciaSheet { copy(isSubmitting = true) }
    coroutineScope.launch {
      when (
          val outcome = repositories.recurrencias.update(sheet.recurrencia.id, draft, today.date())
      ) {
        is DataOutcome.Failure -> {
          val rejected = outcome.error is DataError.InvalidInput && rejectsCuenta(draft, sheet)
          updateRecurrenciaSheet(force = true) {
            copy(
                isSubmitting = false,
                edit =
                    edit.copy(
                        error = MovimientoFormError.SAVE_FAILED.takeUnless { rejected },
                        cuentaRejected = rejected,
                    ),
            )
          }
        }
        is DataOutcome.Success ->
            when (outcome.data) {
              is RecurrenciaWrite.Saved ->
                  close(
                      ManagementOutcome(ManagementSubject.Recurrencia, ManagementFeedbackKind.Saved)
                  )
              RecurrenciaWrite.ScheduleLocked ->
                  updateRecurrenciaSheet(force = true) {
                    copy(
                        isSubmitting = false,
                        scheduleRejected = true,
                        frecuencia = recurrencia.frecuencia,
                        edit = edit.copy(draft = edit.draft.copy(fecha = recurrencia.proximaFecha)),
                    )
                  }
            }
      }
    }
  }

  override fun onRequestDelete() {
    updateRecurrenciaSheet { copy(isDeleteConfirmationVisible = true, deleteFailed = false) }
  }

  override fun onCancelDelete() {
    val sheet = uiState.value.sheet as? RecurrenciaSheet ?: return
    if (sheet.isDeleteConfirmationVisible && !sheet.isSubmitting)
        updateRecurrenciaSheet(force = true) { copy(isDeleteConfirmationVisible = false) }
  }

  override fun onConfirmDelete() {
    val sheet = uiState.value.sheet as? RecurrenciaSheet ?: return
    if (!sheet.isDeleteConfirmationVisible || sheet.isSubmitting || sheet.isClosing) return
    updateRecurrenciaSheet(force = true) {
      copy(isSubmitting = true, isDeleteConfirmationVisible = false)
    }
    coroutineScope.launch {
      when (repositories.recurrencias.delete(sheet.recurrencia.id)) {
        is DataOutcome.Failure ->
            updateRecurrenciaSheet(force = true) { copy(isSubmitting = false, deleteFailed = true) }
        is DataOutcome.Success ->
            close(ManagementOutcome(ManagementSubject.Recurrencia, ManagementFeedbackKind.Deleted))
      }
    }
  }

  override fun onRequestCloseSheet() {
    when (val sheet = uiState.value.sheet) {
      null -> Unit
      is OccurrenceSheet -> if (sheet.isEditable) close()
      is RecurrenciaSheet ->
          when {
            sheet.isDeleteConfirmationVisible -> onCancelDelete()
            sheet.canInteract -> close()
          }
    }
  }

  override fun onSheetHidden() {
    if (uiState.value.sheet?.isClosing != true) return
    uiState.update { it.copy(sheet = null) }
    pendingOutcome?.let { effectChannel.trySend(RecurrentesEffect.Feedback(it)) }
    pendingOutcome = null
  }

  private fun close(outcome: ManagementOutcome? = null) {
    pendingOutcome = outcome
    uiState.update { state ->
      state.copy(
          sheet =
              when (val sheet = state.sheet) {
                null -> null
                is OccurrenceSheet -> sheet.copy(busy = null, isClosing = true)
                is RecurrenciaSheet ->
                    sheet.copy(
                        isSubmitting = false,
                        isDeleteConfirmationVisible = false,
                        isClosing = true,
                    )
              }
      )
    }
  }

  private fun isScheduleLocked(): Boolean {
    val state = uiState.value
    val sheet = state.sheet as? RecurrenciaSheet ?: return true
    return state.isScheduleLocked(sheet)
  }

  private fun categorias(): List<Categoria> =
      uiState.value.ready?.categoriasById?.values?.toList().orEmpty()

  private fun recurrenciaForm() =
      (uiState.value.sheet as? RecurrenciaSheet)
          ?.takeIf { it.canInteract }
          ?.form(uiState.value.catalog, categorias())

  private fun rejectsCuenta(draft: RecurrenciaDraft, sheet: RecurrenciaSheet): Boolean =
      uiState.value.catalog.cuentas
          .firstOrNull { it.id == draft.cuentaId }
          ?.accepts(draft.medio, draft.monto.moneda, storedCuentaId = sheet.recurrencia.cuentaId) !=
          true

  private fun updateOccurrence(
      force: Boolean = false,
      transform: OccurrenceSheet.() -> OccurrenceSheet,
  ) {
    uiState.update { state ->
      val sheet =
          (state.sheet as? OccurrenceSheet)?.takeIf { !it.isClosing && (force || it.isEditable) }
      if (sheet == null) state else state.copy(sheet = sheet.transform())
    }
  }

  private fun updateRecurrenciaSheet(
      force: Boolean = false,
      transform: RecurrenciaSheet.() -> RecurrenciaSheet,
  ) {
    uiState.update { state ->
      val sheet =
          (state.sheet as? RecurrenciaSheet)?.takeIf { !it.isClosing && (force || it.canInteract) }
      if (sheet == null) state else state.copy(sheet = sheet.transform())
    }
  }

  private fun updateDraft(transform: MovimientoDraft.() -> MovimientoDraft) =
      updateRecurrenciaSheet {
        val draft = edit.draft.transform()
        copy(
            edit =
                edit.copy(
                    draft = draft,
                    error = null,
                    cuentaRejected =
                        edit.cuentaRejected &&
                            draft.cuentaId == edit.draft.cuentaId &&
                            draft.medio == edit.draft.medio,
                ),
            deleteFailed = false,
        )
      }

  private suspend fun observeContent() {
    combine(currentDay, contentRetries) { day, _ -> day }
        .flatMapLatest { day ->
          combine(
              repositories.recurrencias.observeLive(day),
              repositories.categorias.observeAll(),
          ) { live, categorias ->
            day to contentOf(live, categorias)
          }
        }
        .collect { (day, content) -> uiState.update { it.copy(today = day, content = content) } }
  }

  private suspend fun observeCuentas() {
    cuentaRetries
        .flatMapLatest {
          uiState.update {
            it.copy(catalog = it.catalog.copy(isCuentasLoading = true, isCuentasFailed = false))
          }
          repositories.cuentas.observeAll()
        }
        .collect { outcome ->
          uiState.update {
            it.copy(
                catalog =
                    when (outcome) {
                      is DataOutcome.Success ->
                          it.catalog.copy(
                              cuentas = outcome.data,
                              isCuentasLoading = false,
                              isCuentasFailed = false,
                          )
                      is DataOutcome.Failure ->
                          it.catalog.copy(isCuentasLoading = false, isCuentasFailed = true)
                    }
            )
          }
        }
  }

  private suspend fun observeEtiquetas() {
    etiquetaRetries
        .flatMapLatest {
          uiState.update {
            it.copy(catalog = it.catalog.copy(etiquetasLoad = EtiquetasLoad.LOADING))
          }
          repositories.etiquetas.observeAll()
        }
        .collect { outcome ->
          uiState.update {
            it.copy(
                catalog =
                    when (outcome) {
                      is DataOutcome.Success ->
                          it.catalog.copy(
                              etiquetas = outcome.data,
                              etiquetasLoad = EtiquetasLoad.READY,
                          )
                      is DataOutcome.Failure ->
                          it.catalog.copy(etiquetasLoad = EtiquetasLoad.UNAVAILABLE)
                    }
            )
          }
        }
  }
}

private fun contentOf(
    live: DataOutcome<List<RecurrenciaWithPending>>,
    categorias: DataOutcome<List<Categoria>>,
): RecurrentesContent =
    if (live is DataOutcome.Success && categorias is DataOutcome.Success)
        live.data.toReady(categorias.data)
    else RecurrentesContent.Failed

private val EditMovimientoUiState.shownChoice: CuentaSelectionState
  get() = CuentaSelectionState(draft.medio, cuentaField.selected?.id, SelectionOwner.USER)

/**
 * While the schedule is locked the template keeps its frecuencia and next date, so a save of the
 * other fields is never refused for them. Tags deleted meanwhile drop out instead of failing it.
 */
private fun MovimientoChanges.toRecurrenciaDraft(
    sheet: RecurrenciaSheet,
    state: RecurrentesUiState,
): RecurrenciaDraft {
  val locked = state.isScheduleLocked(sheet)
  val etiquetas = state.catalog.etiquetas.map { it.id }.toSet()
  return RecurrenciaDraft(
      tipo = tipo,
      monto = monto,
      categoriaId = categoriaId,
      titulo = titulo,
      descripcion = descripcion,
      cuentaId = cuentaId,
      medio = medio,
      etiquetaIds =
          if (state.catalog.etiquetasLoad == EtiquetasLoad.READY)
              etiquetaIds.filter { it in etiquetas }
          else etiquetaIds,
      frecuencia = if (locked) sheet.recurrencia.frecuencia else sheet.frecuencia,
      proximaFecha = if (locked) sheet.recurrencia.proximaFecha else fecha,
  )
}
