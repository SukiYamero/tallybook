package com.kurobello.tallybook.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.PresupuestoRepository
import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.PresupuestoWrite
import com.kurobello.tallybook.core.model.monedas
import com.kurobello.tallybook.core.model.primaryMoneda
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class BudgetsViewModel(
    private val presupuestos: PresupuestoRepository,
    cuentas: CuentaRepository,
    categorias: CategoriaRepository,
    preferences: AppPreferencesRepository,
    private val today: CivilToday,
    scope: CoroutineScope? = null,
) : ViewModel(), BudgetsActions {
  private val coroutineScope = scope ?: viewModelScope
  private val currentDay = MutableStateFlow(today.date())
  private val contentRetries = MutableStateFlow(0)

  // Null until both the accounts and the primary moneda have loaded; creating waits for them.
  private val editorMonedas = MutableStateFlow<List<Moneda>?>(null)
  private var suggestionJob: Job? = null

  val uiState: StateFlow<BudgetsUiState>
    field = MutableStateFlow(BudgetsUiState())

  private val effectChannel = Channel<BudgetsEffect>(Channel.BUFFERED)
  val effects: Flow<BudgetsEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch { observeContent() }
    coroutineScope.launch {
      categorias.observeAll().collect { outcome ->
        val live = (outcome as? DataOutcome.Success)?.data.orEmpty()
        uiState.update { it.copy(categorias = live) }
      }
    }
    val primaryMoneda = preferences.preferences.map { it.primaryMoneda() }
    coroutineScope.launch {
      primaryMoneda.collect { outcome ->
        if (outcome is DataOutcome.Success) {
          uiState.update { it.copy(primaryMoneda = outcome.data) }
        }
      }
    }
    coroutineScope.launch {
      combine(cuentas.observeAll(), primaryMoneda) { accounts, primary ->
            if (accounts is DataOutcome.Success && primary is DataOutcome.Success) {
              primaryFirst(accounts.data.filterNot { it.archivada }.monedas(), primary.data)
            } else {
              null
            }
          }
          .collect { editorMonedas.value = it }
    }
  }

  /** Every resume reads the day again, so a new day or month moves the spent window. */
  override fun onResume() {
    currentDay.value = today.date()
  }

  override fun onRetry() {
    if (uiState.value.content !is RetainedLoadState.Failed) return
    uiState.update { it.copy(content = it.content.refreshing()) }
    contentRetries.update { it + 1 }
  }

  override fun onCreate() {
    val monedas = editorMonedas.value ?: return
    uiState.update {
      it.copy(
          editor =
              BudgetEditorState(
                  editingId = null,
                  categoriaId = null,
                  moneda = monedas.first(),
                  monedas = monedas,
                  amountInput = "",
                  suggestion = null,
                  amountError = false,
                  duplicateOf = null,
                  saving = false,
              )
      )
    }
  }

  override fun onOpen(id: String) {
    val status =
        uiState.value.content.cachedValue?.statuses?.find { it.presupuesto.id == id } ?: return
    uiState.update {
      it.copy(detail = BudgetDetailState(status, history = null, confirmingDelete = false))
    }
    coroutineScope.launch {
      val outcome = presupuestos.history(id, today.date())
      showHistory(id, outcome)
    }
  }

  override fun onCategorySelected(id: String) {
    val editor = uiState.value.editor?.takeIf { it.editingId == null } ?: return
    uiState.update {
      it.copy(editor = editor.copy(categoriaId = id, suggestion = null, saveFailed = false))
    }
    loadSuggestion()
  }

  override fun onMonedaSelected(moneda: Moneda) {
    val editor = uiState.value.editor?.takeIf { it.editingId == null && moneda in it.monedas }
    if (editor == null) return
    uiState.update {
      it.copy(
          editor =
              editor.copy(
                  moneda = moneda,
                  saveFailed = false,
                  suggestion = null,
                  amountInput = editor.amountInput.reformatAmountInput(editor.moneda, moneda),
                  amountError = false,
              )
      )
    }
    loadSuggestion()
  }

  override fun onAmountChanged(text: String) = updateEditor {
    copy(amountInput = text, amountError = false, saveFailed = false)
  }

  override fun onUseSuggestion() = updateEditor {
    suggestion?.let {
      copy(amountInput = it.toAmountInput(), amountError = false, saveFailed = false)
    } ?: this
  }

  override fun onSave() {
    val editor = uiState.value.editor?.takeIf { !it.saving && it.canSave } ?: return
    val parsed = parseAmountInput(editor.amountInput, editor.moneda)
    if (parsed is AmountInputResult.Valid) {
      updateEditor { copy(saving = true, saveFailed = false) }
      coroutineScope.launch { finishSave(write(editor, parsed.money)) }
    } else {
      updateEditor { copy(amountError = true) }
    }
  }

  private suspend fun write(
      editor: BudgetEditorState,
      monto: Money,
  ): DataOutcome<PresupuestoWrite> =
      if (editor.editingId != null) {
        presupuestos.updateMonto(editor.editingId, monto)
      } else {
        presupuestos.create(requireNotNull(editor.categoriaId), monto, today.date())
      }

  override fun onDismissEditor() {
    suggestionJob?.cancel()
    uiState.update { it.copy(editor = null) }
  }

  override fun onEditAmount() {
    val presupuesto = uiState.value.detail?.status?.presupuesto ?: return
    val monto = presupuesto.monto
    uiState.update {
      it.copy(
          editor =
              BudgetEditorState(
                  editingId = presupuesto.id,
                  categoriaId = presupuesto.categoriaId,
                  moneda = monto.moneda,
                  monedas = listOf(monto.moneda),
                  amountInput = monto.toAmountInput(),
                  suggestion = null,
                  amountError = false,
                  duplicateOf = null,
                  saving = false,
              )
      )
    }
  }

  override fun onRequestDelete() = updateDetail {
    copy(confirmingDelete = true, deleteFailed = false)
  }

  override fun onCancelDelete() = updateDetail { copy(confirmingDelete = false) }

  override fun onConfirmDelete() {
    val detail = uiState.value.detail?.takeIf { it.confirmingDelete } ?: return
    coroutineScope.launch {
      when (presupuestos.delete(detail.status.presupuesto.id)) {
        is DataOutcome.Success -> uiState.update { it.copy(detail = null) }
        is DataOutcome.Failure ->
            updateDetail { copy(confirmingDelete = false, deleteFailed = true) }
      }
    }
  }

  override fun onDismissDetail() {
    uiState.update { it.copy(detail = null) }
  }

  override fun onViewMovements() {
    val presupuesto = uiState.value.detail?.status?.presupuesto ?: return
    val day = today.date()
    uiState.update { it.copy(detail = null) }
    effectChannel.trySend(
        BudgetsEffect.OpenSearch(
            categoriaId = presupuesto.categoriaId,
            moneda = presupuesto.monto.moneda,
            range = MovimientoDateRange(LocalDate(day.year, day.month, 1), day),
        )
    )
  }

  private fun loadSuggestion() {
    val editor = uiState.value.editor ?: return
    val categoriaId = editor.categoriaId ?: return
    suggestionJob?.cancel()
    suggestionJob = coroutineScope.launch {
      val outcome = presupuestos.suggestion(categoriaId, editor.moneda, today.date())
      val suggestion = (outcome as? DataOutcome.Success)?.data
      updateEditor { copy(suggestion = suggestion) }
    }
  }

  private fun finishSave(outcome: DataOutcome<PresupuestoWrite>) {
    when (outcome) {
      is DataOutcome.Failure -> updateEditor { copy(saving = false, saveFailed = true) }
      is DataOutcome.Success ->
          when (val write = outcome.data) {
            is PresupuestoWrite.Saved -> uiState.update { it.copy(editor = null) }
            is PresupuestoWrite.Duplicate ->
                updateEditor {
                  copy(
                      editingId = write.existing.id,
                      amountInput = write.existing.monto.toAmountInput(),
                      duplicateOf = write.existing,
                      suggestion = null,
                      saving = false,
                  )
                }
            PresupuestoWrite.UnknownCategoria ->
                updateEditor { copy(categoriaId = null, suggestion = null, saving = false) }
          }
    }
  }

  private fun showHistory(id: String, outcome: DataOutcome<List<BudgetMonth>>) {
    uiState.update { state ->
      val detail = state.detail
      if (detail?.status?.presupuesto?.id == id) {
        state.copy(
            detail =
                when (outcome) {
                  is DataOutcome.Success -> detail.copy(history = outcome.data)
                  is DataOutcome.Failure -> detail.copy(historyFailed = true)
                }
        )
      } else {
        state
      }
    }
  }

  private fun updateEditor(transform: BudgetEditorState.() -> BudgetEditorState) {
    uiState.update { state -> state.copy(editor = state.editor?.transform()) }
  }

  private fun updateDetail(transform: BudgetDetailState.() -> BudgetDetailState) {
    uiState.update { state -> state.copy(detail = state.detail?.transform()) }
  }

  private suspend fun observeContent() {
    combine(currentDay, contentRetries) { day, _ -> day }
        .flatMapLatest { day ->
          presupuestos.observeStatuses(day).map { outcome -> day to outcome }
        }
        .collect { (day, outcome) ->
          uiState.update { state ->
            when (outcome) {
              is DataOutcome.Failure ->
                  state.copy(
                      content = RetainedLoadState.Failed(outcome.error, state.content.cachedValue)
                  )
              is DataOutcome.Success ->
                  state.copy(
                      content = RetainedLoadState.Ready(contentOf(outcome.data, day)),
                      detail = state.detail?.refreshedFrom(outcome.data),
                  )
            }
          }
        }
  }
}

private val BudgetEditorState.canSave: Boolean
  get() = editingId != null || categoriaId != null

private fun primaryFirst(monedas: List<Moneda>, primary: Moneda): List<Moneda> =
    if (primary in monedas) listOf(primary) + (monedas - primary)
    else monedas.ifEmpty { listOf(primary) }

/** Null when the budget is gone, which closes its detail. */
private fun BudgetDetailState.refreshedFrom(statuses: List<BudgetStatus>): BudgetDetailState? =
    statuses.find { it.presupuesto.id == status.presupuesto.id }?.let { copy(status = it) }

private fun contentOf(statuses: List<BudgetStatus>, day: LocalDate): BudgetsContent {
  val month = day.yearMonth
  return BudgetsContent(
      statuses = statuses,
      fineCount = statuses.count { it.state == BudgetState.FINE },
      daysLeft = month.numberOfDays - day.day + 1,
      month = month,
  )
}
