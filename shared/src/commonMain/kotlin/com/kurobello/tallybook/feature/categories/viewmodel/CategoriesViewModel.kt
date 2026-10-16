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
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.category.categoryBlocks
import com.kurobello.tallybook.core.ui.category.categoryGroups
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
    uiState.update { it.copy(query = query, content = source.content(query)) }
  }

  override fun onClearQuery() = onQueryChanged("")

  override fun onRetry() {
    if (source is CategoriesSource.Failed) retryRequests.update { it + 1 }
  }

  override fun onOpenSheet(request: CategorySheetRequest): Boolean {
    val sheet = if (uiState.value.sheet == null) newSheet(request) else null
    if (sheet != null) uiState.update { it.copy(sheet = sheet) }
    return sheet != null
  }

  override fun onNombreChanged(value: String) = updateDraft { it.onNombre(value, suggester) }

  override fun onIconoSelected(icono: CategoryIcon) = updateDraft { it.onIcono(icono) }

  override fun onColorSelected(color: CategoryTint) = updateDraft { it.onColor(color) }

  override fun onDescripcionChanged(value: String) = updateDraft { it.onDescripcion(value) }

  override fun onMoreOptions() = updateDraft { it.onMoreOptions() }

  override fun onToggleHidden(id: String) {
    val categoria = source.find(id)?.takeIf { it.canChangeVisibility } ?: return
    val shown =
        when (val sheet = uiState.value.sheet) {
          is CategoryEditor -> sheet.isEditable && sheet.editing?.id == id
          is CategoryDetails -> sheet.isInteractive && sheet.categoria.id == id
          null -> false
        }
    if (!shown) return
    updateSheet { it.withVisibility(pending = true, failed = false) }
    coroutineScope.launch {
      val failed = categorias.setOculta(categoria.id, !categoria.oculta) is DataOutcome.Failure
      updateSheet { it.withVisibility(pending = false, failed = failed) }
    }
  }

  override fun onViewMovimientos() {
    when (val sheet = uiState.value.sheet) {
      is CategoryEditor ->
          if (sheet.isEditable && sheet.editing != null && sheet.count > 0) {
            updateEditor {
              if (it.hasUnsavedChanges) it.copy(confirmingDiscard = true, opensSearch = true)
              else it.copy(isClosing = true, opensSearch = true)
            }
          }
      is CategoryDetails ->
          if (sheet.isInteractive && sheet.count > 0) {
            updateDetails { it.copy(isClosing = true, opensSearch = true) }
          }
      null -> Unit
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

  override fun onRequestCloseSheet() {
    when (val sheet = uiState.value.sheet) {
      is CategoryEditor ->
          if (sheet.isEditable) {
            updateEditor {
              if (it.hasUnsavedChanges) it.copy(confirmingDiscard = true)
              else it.copy(isClosing = true)
            }
          }
      is CategoryDetails -> if (sheet.isInteractive) updateDetails { it.copy(isClosing = true) }
      null -> Unit
    }
  }

  override fun onConfirmDiscard() = updateEditor {
    if (it.confirmingDiscard) it.copy(confirmingDiscard = false, isClosing = true) else it
  }

  override fun onContinueEditing() = updateEditor {
    it.copy(confirmingDiscard = false, opensSearch = false)
  }

  override fun onSheetHidden() {
    val sheet = uiState.value.sheet?.takeIf { it.isClosing } ?: return
    uiState.update { it.copy(sheet = null) }
    val id = sheet.categoriaId
    if (sheet.opensSearch && id != null) effectChannel.trySend(CategoriesEffect.OpenSearch(id))
  }

  override fun onOpenDuplicate() {
    val loaded = source as? CategoriesSource.Ready
    val duplicate =
        uiState.value.editor
            ?.takeIf { it.isEditable }
            ?.draft
            ?.duplicate
            ?.takeIf { it.id != CATEGORIA_OTROS_ID }
    if (loaded == null || duplicate == null) return
    val stored = loaded.find(duplicate.id) ?: duplicate
    uiState.update { it.copy(sheet = loaded.sheetFor(stored)) }
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

  private fun newSheet(request: CategorySheetRequest): CategorySheet? {
    val loaded = source as? CategoriesSource.Ready ?: return null
    return when (request) {
      is CategorySheetRequest.New ->
          CategoryEditor.of(null, CategoryDraftState.forNew(request.nombre.trim(), suggester))
      is CategorySheetRequest.Open -> loaded.find(request.id)?.let(loaded::sheetFor)
    }
  }

  private fun updateDraft(transform: (CategoryDraftState) -> CategoryDraftState) = updateEditor {
    if (it.isEditable) it.copy(draft = transform(it.draft)) else it
  }

  private fun updateEditor(transform: (CategoryEditor) -> CategoryEditor) {
    uiState.update { state -> state.editor?.let { state.copy(sheet = transform(it)) } ?: state }
  }

  private fun updateDetails(transform: (CategoryDetails) -> CategoryDetails) {
    uiState.update { state -> state.details?.let { state.copy(sheet = transform(it)) } ?: state }
  }

  private fun updateSheet(transform: (CategorySheet) -> CategorySheet) {
    uiState.update { state -> state.sheet?.let { state.copy(sheet = transform(it)) } ?: state }
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

private val CategoryDetails.isInteractive: Boolean
  get() = !isClosing && !visibilityPending

private val CategorySheet.categoriaId: String?
  get() =
      when (this) {
        is CategoryEditor -> editing?.id
        is CategoryDetails -> categoria.id
      }

private fun CategorySheet.withVisibility(pending: Boolean, failed: Boolean): CategorySheet =
    when (this) {
      is CategoryEditor ->
          copy(
              visibilityPending = pending,
              error =
                  when {
                    failed -> CategoryEditorError.VISIBILITY
                    pending -> null
                    else -> error
                  },
          )
      is CategoryDetails -> copy(visibilityPending = pending, visibilityFailed = failed)
    }

private sealed interface CategoriesSource {
  data object Loading : CategoriesSource

  data object Failed : CategoriesSource

  data class Ready(val categorias: List<Categoria>, val counts: Map<String, Long>) :
      CategoriesSource {
    fun count(id: String): Long = counts[id] ?: 0L

    fun sheetFor(categoria: Categoria): CategorySheet =
        if (categoria.isCustom)
            CategoryEditor.of(categoria, CategoryDraftState.forEdit(categoria), count(categoria.id))
        else CategoryDetails(categoria, count(categoria.id))
  }
}

private fun CategoriesSource.find(id: String): Categoria? =
    (this as? CategoriesSource.Ready)?.categorias?.firstOrNull { it.id == id }

private fun CategoriesSource.content(query: String): CategoriesContent =
    when (this) {
      CategoriesSource.Loading -> CategoriesContent.Loading
      CategoriesSource.Failed -> CategoriesContent.Failed
      is CategoriesSource.Ready -> {
        fun row(categoria: Categoria) = CategoryRow(categoria, count(categoria.id))
        if (query.isBlank()) {
          val blocks = categoryBlocks(categorias)
          CategoriesContent.Browse(
              custom = blocks.custom.map(::row),
              families = blocks.families.map { family -> family.map(::row) },
          )
        } else {
          CategoriesContent.Search(
              query = query.trim(),
              matches = categoryGroups(categorias, emptySet(), query).flatten().map(::row),
          )
        }
      }
    }

private fun CategoriesUiState.withSource(loaded: CategoriesSource): CategoriesUiState =
    copy(content = loaded.content(query), sheet = sheet?.refreshedFrom(loaded))

private fun CategorySheet.refreshedFrom(loaded: CategoriesSource): CategorySheet {
  val ready = loaded as? CategoriesSource.Ready
  val stored = categoriaId?.let { id -> ready?.find(id) }
  if (ready == null || stored == null) return this
  return when (this) {
    is CategoryEditor -> copy(editing = stored, count = ready.count(stored.id))
    is CategoryDetails -> copy(categoria = stored, count = ready.count(stored.id))
  }
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
