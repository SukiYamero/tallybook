package com.kurobello.tallybook.feature.categories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.category.categoryFamilies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class CategoriesViewModel(
    private val categorias: CategoriaRepository,
    private val preferences: AppPreferencesRepository,
    private val suggester: CategoryAppearanceSuggester,
    scope: CoroutineScope? = null,
) : ViewModel(), CategoriesActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryRequests = MutableStateFlow(0L)
  private var source: CategoriesSource = CategoriesSource.Loading
  private val effectChannel = Channel<CategoriesEffect>(Channel.BUFFERED)

  val uiState: StateFlow<CategoriesUiState>
    field = MutableStateFlow(CategoriesUiState())

  val effects: Flow<CategoriesEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      retryRequests
          .flatMapLatest { observeSource().onStart { emit(CategoriesSource.Loading) } }
          .collect { loaded ->
            source = loaded
            uiState.update { it.withSource(loaded) }
          }
    }
  }

  override fun onQueryChanged(query: String) {
    uiState.update { it.copy(query = query, highlightedId = null, content = source.content(query)) }
  }

  override fun onClearQuery() = onQueryChanged("")

  override fun onRetry() {
    if (source is CategoriesSource.Failed) retryRequests.update { it + 1 }
  }

  override fun onOpenCategory(id: String) {
    if (uiState.value.editor != null || source.find(id) == null) return
    uiState.update { it.copy(highlightedId = null) }
    effectChannel.trySend(CategoriesEffect.OpenSearch(id))
  }

  override fun onOpenEditor(request: CategoryEditorRequest): Boolean {
    val editor = if (uiState.value.editor == null) newEditor(request) else null
    if (editor != null) uiState.update { it.copy(editor = editor) }
    return editor != null
  }

  override fun onNombreChanged(value: String) = updateDraft { it.onNombre(value, suggester) }

  override fun onIconoSelected(icono: CategoryIcon) = updateDraft { it.onIcono(icono) }

  override fun onColorSelected(color: CategoryTint) = updateDraft { it.onColor(color) }

  override fun onDescripcionChanged(value: String) = updateDraft { it.onDescripcion(value) }

  override fun onMoreOptions() = updateDraft { it.onMoreOptions() }

  override fun onToggleHidden(id: String) {
    val categoria = source.find(id) ?: return
    if (categoria.id == CATEGORIA_OTROS_ID) return
    val editor = uiState.value.editor
    when {
      editor == null -> toggleFromRow(categoria)
      editor.editing?.id == id && editor.isEditable -> toggleFromEditor(categoria)
    }
  }

  override fun onSaveEditor() {
    val editor = uiState.value.editor?.takeIf { it.isEditable && it.draft.canSave } ?: return
    val draft = editor.draft.toDraft()
    val editingId = editor.editing?.id
    updateEditor { it.copy(draft = it.draft.copy(saving = true), error = null) }
    coroutineScope.launch {
      val written =
          if (editingId == null) categorias.create(draft) else categorias.update(editingId, draft)
      when (written) {
        is DataOutcome.Failure ->
            updateEditor {
              it.copy(draft = it.draft.copy(saving = false), error = CategoryEditorError.SAVE)
            }
        is DataOutcome.Success ->
            when (val write = written.data) {
              is CategoriaWrite.Duplicate ->
                  updateEditor {
                    it.copy(draft = it.draft.copy(saving = false, duplicate = write.existing))
                  }
              is CategoriaWrite.Saved -> {
                updateEditor { it.copy(draft = it.draft.copy(saving = false), isClosing = true) }
                feedback(
                    if (editingId == null) CategoriesFeedback.Created
                    else CategoriesFeedback.Updated
                )
              }
            }
      }
    }
  }

  override fun onRequestCloseEditor() {
    val editor = uiState.value.editor?.takeIf { it.isEditable } ?: return
    updateEditor {
      if (editor.hasUnsavedChanges) it.copy(confirmingDiscard = true) else it.copy(isClosing = true)
    }
  }

  override fun onConfirmDiscard() = updateEditor {
    if (it.confirmingDiscard) it.copy(confirmingDiscard = false, isClosing = true) else it
  }

  override fun onContinueEditing() = updateEditor { it.copy(confirmingDiscard = false) }

  override fun onEditorHidden() {
    uiState.update { if (it.editor?.isClosing == true) it.copy(editor = null) else it }
  }

  override fun onOpenDuplicate() {
    val editor = uiState.value.editor?.takeIf { it.isEditable } ?: return
    val duplicate = editor.draft.duplicate?.takeIf { it.id != CATEGORIA_OTROS_ID } ?: return
    val stored = source.find(duplicate.id) ?: duplicate
    uiState.update {
      it.copy(
          query = "",
          content = source.content(""),
          highlightedId = stored.id,
          editor =
              if (stored.origen == CategoriaOrigen.USUARIO)
                  CategoryEditor.of(stored, CategoryDraftState.forEdit(stored))
              else editor.copy(isClosing = true),
      )
    }
    effectChannel.trySend(CategoriesEffect.Reveal(stored.id))
  }

  override fun onRequestDelete() {
    val editor = uiState.value.editor?.takeIf { it.isEditable } ?: return
    val categoria = editor.editing ?: return
    updateEditor { it.copy(deletion = CategoryDeletion.Checking, error = null) }
    coroutineScope.launch {
      when (val counted = categorias.countMovimientos(categoria.id)) {
        is DataOutcome.Failure ->
            updateEditor { it.copy(deletion = null, error = CategoryEditorError.DELETE) }
        is DataOutcome.Success ->
            updateEditor {
              it.copy(
                  deletion =
                      if (counted.data == 0L) CategoryDeletion.Confirm()
                      else CategoryDeletion.InUse(counted.data)
              )
            }
      }
    }
  }

  override fun onConfirmDelete() {
    val editor = uiState.value.editor
    val categoria = editor?.editing
    val deletion = editor?.deletion as? CategoryDeletion.Confirm
    if (categoria == null || deletion == null || deletion.deleting) return
    updateEditor { it.copy(deletion = CategoryDeletion.Confirm(deleting = true)) }
    coroutineScope.launch {
      when (val deleted = categorias.delete(categoria.id)) {
        is DataOutcome.Failure ->
            updateEditor { it.copy(deletion = null, error = CategoryEditorError.DELETE) }
        is DataOutcome.Success ->
            when (val result = deleted.data) {
              is CategoriaDelete.InUse ->
                  updateEditor { it.copy(deletion = CategoryDeletion.InUse(result.count)) }
              CategoriaDelete.Deleted -> {
                updateEditor { it.copy(deletion = null, isClosing = true) }
                feedback(CategoriesFeedback.Deleted)
              }
            }
      }
    }
  }

  override fun onHideInstead() {
    val editor = uiState.value.editor
    val categoria = editor?.editing
    val deletion = editor?.deletion as? CategoryDeletion.InUse
    if (categoria == null || deletion == null || deletion.hiding) return
    updateEditor { it.copy(deletion = deletion.copy(hiding = true)) }
    coroutineScope.launch {
      when (categorias.setOculta(categoria.id, true)) {
        is DataOutcome.Failure ->
            updateEditor { it.copy(deletion = null, error = CategoryEditorError.VISIBILITY) }
        is DataOutcome.Success -> {
          updateEditor { it.copy(deletion = null, isClosing = true) }
          feedback(CategoriesFeedback.Hidden(categoria.nombre))
        }
      }
    }
  }

  override fun onCancelDelete() = updateEditor {
    if (it.deletion?.isBusy == false) it.copy(deletion = null) else it
  }

  private fun newEditor(request: CategoryEditorRequest): CategoryEditor? {
    val loaded = source as? CategoriesSource.Ready ?: return null
    return when (request) {
      is CategoryEditorRequest.New ->
          CategoryEditor.of(null, CategoryDraftState.forNew(request.nombre.trim(), suggester))
      is CategoryEditorRequest.Edit ->
          loaded
              .find(request.id)
              ?.takeIf { it.origen == CategoriaOrigen.USUARIO }
              ?.let { CategoryEditor.of(it, CategoryDraftState.forEdit(it)) }
    }
  }

  private fun toggleFromRow(categoria: Categoria) {
    if (categoria.id in uiState.value.pendingVisibility) return
    uiState.update { it.copy(pendingVisibility = it.pendingVisibility + categoria.id) }
    coroutineScope.launch {
      val written = categorias.setOculta(categoria.id, !categoria.oculta)
      uiState.update { it.copy(pendingVisibility = it.pendingVisibility - categoria.id) }
      feedback(
          when {
            written is DataOutcome.Failure -> CategoriesFeedback.VisibilityFailed
            categoria.oculta -> CategoriesFeedback.Shown(categoria.nombre)
            else -> CategoriesFeedback.Hidden(categoria.nombre)
          }
      )
    }
  }

  private fun toggleFromEditor(categoria: Categoria) {
    updateEditor { it.copy(visibilityPending = true, error = null) }
    coroutineScope.launch {
      val failed = categorias.setOculta(categoria.id, !categoria.oculta) is DataOutcome.Failure
      updateEditor {
        it.copy(
            visibilityPending = false,
            error = if (failed) CategoryEditorError.VISIBILITY else it.error,
        )
      }
    }
  }

  private fun updateDraft(transform: (CategoryDraftState) -> CategoryDraftState) = updateEditor {
    if (it.isEditable) it.copy(draft = transform(it.draft)) else it
  }

  private fun updateEditor(transform: (CategoryEditor) -> CategoryEditor) {
    uiState.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }
  }

  private fun feedback(feedback: CategoriesFeedback) {
    effectChannel.trySend(CategoriesEffect.Feedback(feedback))
  }

  private fun observeSource(): Flow<CategoriesSource> {
    val counts =
        preferences.preferences
            .map { it.primaryMoneda() }
            .distinctUntilChanged()
            .flatMapLatest { moneda ->
              when (moneda) {
                is DataOutcome.Success -> categorias.observeMovimientoCounts(moneda.data)
                is DataOutcome.Failure -> flowOf(moneda)
              }
            }
    return combine(categorias.observeAll(), counts) { all, counted ->
      if (all is DataOutcome.Success && counted is DataOutcome.Success)
          CategoriesSource.Ready(all.data, counted.data)
      else CategoriesSource.Failed
    }
  }
}

private val CategoryEditor.isEditable: Boolean
  get() = !isClosing && !isBusy && deletion == null && !confirmingDiscard

private sealed interface CategoriesSource {
  data object Loading : CategoriesSource

  data object Failed : CategoriesSource

  data class Ready(val categorias: List<Categoria>, val counts: Map<String, Long>) :
      CategoriesSource
}

private fun CategoriesSource.find(id: String): Categoria? =
    (this as? CategoriesSource.Ready)?.categorias?.firstOrNull { it.id == id }

private fun CategoriesSource.content(query: String): CategoriesContent =
    when (this) {
      CategoriesSource.Loading -> CategoriesContent.Loading
      CategoriesSource.Failed -> CategoriesContent.Failed
      is CategoriesSource.Ready -> {
        val key = normalizeSearchText(query)
        fun row(categoria: Categoria) = CategoryRow(categoria, counts[categoria.id] ?: 0L)
        fun matches(categoria: Categoria) =
            key.isEmpty() || normalizeSearchText(categoria.nombre).contains(key)
        val (custom, predefined) = categorias.partition { it.origen == CategoriaOrigen.USUARIO }
        CategoriesContent.Ready(
            custom =
                custom
                    .filter(::matches)
                    .sortedBy { CategoriaRules.nombreKey(it.nombre) }
                    .map(::row),
            predefined =
                categoryFamilies(predefined)
                    .map { family -> family.filter(::matches).map(::row) }
                    .filter { it.isNotEmpty() },
        )
      }
    }

private fun CategoriesUiState.withSource(loaded: CategoriesSource): CategoriesUiState {
  val current = editor
  val refreshed = current?.editing?.let { editing -> loaded.find(editing.id) }
  return copy(
      content = loaded.content(query),
      editor = if (refreshed != null) current.copy(editing = refreshed) else current,
  )
}

private fun DataOutcome<AppPreferencesState>.primaryMoneda(): DataOutcome<Moneda> =
    when (this) {
      is DataOutcome.Failure -> this
      is DataOutcome.Success ->
          when (val state = data) {
            is AppPreferencesState.Ready -> DataOutcome.Success(state.value.monedaPrincipal)
            AppPreferencesState.Uninitialized ->
                DataOutcome.Failure(
                    DataError.InvalidInput("Preferences are uninitialized after app startup")
                )
          }
    }
