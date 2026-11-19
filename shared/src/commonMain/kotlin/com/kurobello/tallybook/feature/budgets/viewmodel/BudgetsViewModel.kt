package com.kurobello.tallybook.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
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
    preferences: AppPreferencesRepository,
    private val today: CivilToday,
    scope: CoroutineScope? = null,
) : ViewModel(), BudgetsActions {
  private val coroutineScope = scope ?: viewModelScope
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
      combine(cuentas.observeAll(), preferences.preferences.map { it.primaryMoneda() }) {
              accounts,
              primary ->
            if (accounts is DataOutcome.Success && primary is DataOutcome.Success) {
              primaryFirst(accounts.data.filterNot { it.archivada }.monedas(), primary.data)
            } else {
              null
            }
          }
          .collect { editorMonedas.value = it }
    }
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
      if (outcome is DataOutcome.Success) showHistory(id, outcome.data)
    }
  }

  override fun onCategorySelected(id: String) {
    val editor = uiState.value.editor?.takeIf { it.editingId == null } ?: return
    uiState.update { it.copy(editor = editor.copy(categoriaId = id, suggestion = null)) }
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
                  suggestion = null,
                  amountInput = editor.amountInput.reformatAmountInput(editor.moneda, moneda),
                  amountError = false,
              )
      )
    }
    loadSuggestion()
  }

  override fun onAmountChanged(text: String) = updateEditor {
    copy(amountInput = text, amountError = false)
  }

  override fun onUseSuggestion() = updateEditor {
    suggestion?.let { copy(amountInput = it.toAmountInput(), amountError = false) } ?: this
  }

  override fun onSave() {
    val editor = uiState.value.editor?.takeIf { !it.saving && it.canSave } ?: return
    val parsed = parseAmountInput(editor.amountInput, editor.moneda)
    if (parsed is AmountInputResult.Valid) {
      updateEditor { copy(saving = true) }
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

  override fun onRequestDelete() = updateDetail { copy(confirmingDelete = true) }

  override fun onConfirmDelete() {
    val detail = uiState.value.detail?.takeIf { it.confirmingDelete } ?: return
    coroutineScope.launch {
      when (presupuestos.delete(detail.status.presupuesto.id)) {
        is DataOutcome.Success -> uiState.update { it.copy(detail = null) }
        is DataOutcome.Failure -> updateDetail { copy(confirmingDelete = false) }
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
      is DataOutcome.Failure -> updateEditor { copy(saving = false) }
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

  private fun showHistory(id: String, history: List<BudgetMonth>) {
    uiState.update { state ->
      val detail = state.detail
      if (detail?.status?.presupuesto?.id == id) {
        state.copy(detail = detail.copy(history = history))
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
    contentRetries
        .flatMapLatest {
          val day = today.date()
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
