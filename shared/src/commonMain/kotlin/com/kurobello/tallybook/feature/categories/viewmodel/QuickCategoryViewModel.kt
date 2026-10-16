package com.kurobello.tallybook.feature.categories.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.feature.categories.ui.CategoryFormActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [draft] is null while the picker shows its results; otherwise the creation pane is open. */
internal data class QuickCategoryUiState(
    val draft: CategoryDraftState? = null,
    val saveFailed: Boolean = false,
)

internal sealed interface QuickCategoryEffect {
  val categoriaId: String

  data class Created(override val categoriaId: String) : QuickCategoryEffect

  /**
   * The draft collided with [categoriaId], which the user chose instead (shown again if hidden).
   */
  data class Used(override val categoriaId: String) : QuickCategoryEffect
}

@Stable
internal interface QuickCategoryActions : CategoryFormActions {
  fun onStart(nombre: String)

  fun onSave()

  fun onUseDuplicate()

  /** Leaves the creation pane; a write still in flight completes but no longer selects. */
  fun onClose()
}

// Every form field is its own entry point of QuickCategoryActions.
@Suppress("TooManyFunctions")
internal class QuickCategoryViewModel(
    private val categoriaRepository: CategoriaRepository,
    private val suggester: CategoryAppearanceSuggester,
    scope: CoroutineScope? = null,
) : ViewModel(), QuickCategoryActions {
  private val coroutineScope = scope ?: viewModelScope
  // Bumped whenever the pane opens or closes, so a write that outlives its pane is ignored.
  private var generation = 0L

  val uiState: StateFlow<QuickCategoryUiState>
    field = MutableStateFlow(QuickCategoryUiState())

  private val effectChannel = Channel<QuickCategoryEffect>(Channel.BUFFERED)
  val effects: Flow<QuickCategoryEffect> = effectChannel.receiveAsFlow()

  override fun onStart(nombre: String) {
    generation++
    uiState.value =
        QuickCategoryUiState(draft = CategoryDraftState.forNew(nombre.trim(), suggester))
  }

  override fun onNombreChanged(value: String) = uiState.updateDraft { onNombre(value, suggester) }

  override fun onIconoSelected(icono: CategoryIcon) = uiState.updateDraft { onIcono(icono) }

  override fun onColorSelected(color: CategoryTint) = uiState.updateDraft { onColor(color) }

  override fun onDescripcionChanged(value: String) = uiState.updateDraft { onDescripcion(value) }

  override fun onMoreOptions() = uiState.updateDraft { onMoreOptions() }

  override fun onSave() {
    val draft = uiState.value.draft?.takeIf { it.canSave } ?: return
    write(draft, { categoriaRepository.create(draft.toDraft()) }) { result ->
      when (result) {
        is CategoriaWrite.Saved -> finish(QuickCategoryEffect.Created(result.categoria.id))
        is CategoriaWrite.Duplicate ->
            uiState.value = QuickCategoryUiState(draft = draft.copy(duplicate = result.existing))
      }
    }
  }

  override fun onUseDuplicate() {
    val draft = uiState.value.draft?.takeUnless { it.saving } ?: return
    val existing = draft.duplicate?.takeUnless { it.id == CATEGORIA_OTROS_ID } ?: return
    val used = QuickCategoryEffect.Used(existing.id)
    if (existing.oculta) {
      write(draft, { categoriaRepository.setOculta(existing.id, oculta = false) }) { finish(used) }
    } else {
      finish(used)
    }
  }

  override fun onClose() {
    generation++
    uiState.value = QuickCategoryUiState()
  }

  private fun finish(effect: QuickCategoryEffect) {
    onClose()
    effectChannel.trySend(effect)
  }

  private fun <T> write(
      draft: CategoryDraftState,
      request: suspend () -> DataOutcome<T>,
      onSuccess: (T) -> Unit,
  ) {
    val token = generation
    uiState.value = QuickCategoryUiState(draft = draft.copy(saving = true))
    coroutineScope.launch {
      val outcome = request()
      if (token != generation) return@launch
      when (outcome) {
        is DataOutcome.Success -> onSuccess(outcome.data)
        is DataOutcome.Failure ->
            uiState.value =
                QuickCategoryUiState(draft = draft.copy(saving = false), saveFailed = true)
      }
    }
  }
}

private inline fun MutableStateFlow<QuickCategoryUiState>.updateDraft(
    transform: CategoryDraftState.() -> CategoryDraftState
) = update { state ->
  val draft = state.draft?.takeUnless { it.saving } ?: return@update state
  state.copy(draft = draft.transform())
}
