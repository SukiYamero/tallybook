package com.kurobello.tallybook.feature.tags.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.primaryMoneda
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.ui.tag.searchEtiquetas
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
internal class TagsViewModel(
    private val etiquetas: EtiquetaRepository,
    private val preferences: AppPreferencesRepository,
    scope: CoroutineScope? = null,
) : ViewModel(), TagsActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryRequests = MutableStateFlow(0L)
  private var source: TagsSource = TagsSource.Loading
  private val effectChannel = Channel<TagsEffect>(Channel.BUFFERED)

  val uiState: StateFlow<TagsUiState>
    field = MutableStateFlow(TagsUiState())

  val effects: Flow<TagsEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      retryRequests
          .flatMapLatest { observeSource().onStart { emit(TagsSource.Loading) } }
          .collect { loaded ->
            source = loaded
            uiState.update { it.withSource(loaded) }
          }
    }
  }

  override fun onQueryChanged(query: String) {
    uiState.update { it.copy(query = query, content = source.content(query)) }
  }

  override fun onRetry() {
    if (source is TagsSource.Failed) retryRequests.update { it + 1 }
  }

  override fun onOpenSheet(request: TagSheetRequest): Boolean {
    val loaded = source as? TagsSource.Ready
    val sheet =
        if (loaded == null || uiState.value.sheet != null) null
        else
            when (request) {
              is TagSheetRequest.New -> TagSheet.forNew(request.nombre)
              is TagSheetRequest.Open -> loaded.find(request.id)?.let(loaded::sheetFor)
            }
    if (sheet != null) uiState.update { it.copy(sheet = sheet) }
    return sheet != null
  }

  override fun onNombreChanged(value: String) = updateSheet {
    if (it.isEditable) it.copy(nombre = value, nombreEdited = true, duplicate = null) else it
  }

  override fun onDescripcionChanged(value: String) = updateSheet {
    if (it.isEditable) it.copy(descripcion = value) else it
  }

  override fun onSaveSheet() {
    val sheet = uiState.value.sheet?.takeIf { it.canSave } ?: return
    val nombre = sheet.nombre.trim()
    val descripcion = sheet.descripcion.normalizeOptionalText()
    val editing = sheet.editing
    if (editing != null && nombre == editing.nombre && descripcion == editing.descripcion) {
      updateSheet { it.copy(isClosing = true) }
      return
    }
    updateSheet { it.copy(saving = true, error = null) }
    coroutineScope.launch {
      val written =
          if (editing == null) etiquetas.create(nombre, descripcion)
          else etiquetas.update(editing.id, nombre, descripcion)
      when (written) {
        is DataOutcome.Failure ->
            updateSheet { it.copy(saving = false, error = TagSheetError.SAVE) }
        is DataOutcome.Success ->
            when (val write = written.data) {
              is EtiquetaWrite.Duplicate ->
                  updateSheet { it.copy(saving = false, duplicate = write.existing) }
              is EtiquetaWrite.Saved -> {
                updateSheet { it.copy(saving = false, isClosing = true) }
                feedback(if (editing == null) TagsFeedback.CREATED else TagsFeedback.UPDATED)
              }
            }
      }
    }
  }

  override fun onViewMovimientos() = updateSheet {
    if (it.isEditable && it.editing != null && it.count > 0) {
      it.copy(isClosing = true, opensSearch = true)
    } else {
      it
    }
  }

  override fun onOpenDuplicate() {
    val loaded = source as? TagsSource.Ready ?: return
    val duplicate = uiState.value.sheet?.takeIf { it.isEditable }?.duplicate ?: return
    val stored = loaded.find(duplicate.id) ?: duplicate
    uiState.update { it.copy(sheet = loaded.sheetFor(stored)) }
  }

  override fun onRequestDelete() {
    val etiqueta = uiState.value.sheet?.takeIf { it.isEditable }?.editing ?: return
    updateSheet { it.copy(deletion = TagDeletion.Checking, error = null) }
    coroutineScope.launch {
      when (val counted = etiquetas.countMovimientos(etiqueta.id)) {
        is DataOutcome.Failure ->
            updateSheet { it.copy(deletion = null, error = TagSheetError.DELETE) }
        is DataOutcome.Success ->
            updateSheet { it.copy(deletion = TagDeletion.Confirm(counted.data)) }
      }
    }
  }

  override fun onConfirmDelete() {
    val sheet = uiState.value.sheet
    val etiqueta = sheet?.editing
    val deletion = sheet?.deletion as? TagDeletion.Confirm
    if (etiqueta == null || deletion == null || deletion.deleting) return
    updateSheet { it.copy(deletion = deletion.copy(deleting = true)) }
    coroutineScope.launch {
      when (etiquetas.delete(etiqueta.id)) {
        is DataOutcome.Failure ->
            updateSheet { it.copy(deletion = null, error = TagSheetError.DELETE) }
        is DataOutcome.Success -> {
          updateSheet { it.copy(deletion = null, isClosing = true) }
          feedback(TagsFeedback.DELETED)
        }
      }
    }
  }

  override fun onCancelDelete() = updateSheet {
    if (it.deletion?.isBusy == false) it.copy(deletion = null) else it
  }

  override fun onRequestCloseSheet() = updateSheet {
    if (it.isEditable) it.copy(isClosing = true) else it
  }

  override fun onSheetHidden() {
    val sheet = uiState.value.sheet?.takeIf { it.isClosing } ?: return
    uiState.update { it.copy(sheet = null) }
    val id = sheet.editing?.id
    if (sheet.opensSearch && id != null) effectChannel.trySend(TagsEffect.OpenSearch(id))
  }

  private fun updateSheet(transform: (TagSheet) -> TagSheet) {
    uiState.update { state -> state.sheet?.let { state.copy(sheet = transform(it)) } ?: state }
  }

  private fun feedback(feedback: TagsFeedback) {
    effectChannel.trySend(TagsEffect.Feedback(feedback))
  }

  private fun observeSource(): Flow<TagsSource> {
    val counts =
        preferences.preferences
            .map { it.primaryMoneda() }
            .distinctUntilChanged()
            .flatMapLatest { moneda ->
              when (moneda) {
                is DataOutcome.Success -> etiquetas.observeMovimientoCounts(moneda.data)
                is DataOutcome.Failure -> flowOf(moneda)
              }
            }
    return combine(etiquetas.observeAll(), counts) { all, counted ->
      if (all is DataOutcome.Success && counted is DataOutcome.Success)
          TagsSource.Ready(all.data, counted.data)
      else TagsSource.Failed
    }
  }
}

private sealed interface TagsSource {
  data object Loading : TagsSource

  data object Failed : TagsSource

  data class Ready(val etiquetas: List<Etiqueta>, val counts: Map<String, Long>) : TagsSource {
    fun count(id: String): Long = counts[id] ?: 0L

    fun find(id: String): Etiqueta? = etiquetas.firstOrNull { it.id == id }

    fun sheetFor(etiqueta: Etiqueta): TagSheet = TagSheet.forEdit(etiqueta, count(etiqueta.id))

    fun row(etiqueta: Etiqueta): TagRow = TagRow(etiqueta, count(etiqueta.id))
  }
}

private fun TagsSource.content(query: String): TagsContent =
    when (this) {
      TagsSource.Loading -> TagsContent.Loading
      TagsSource.Failed -> TagsContent.Failed
      is TagsSource.Ready ->
          if (query.isBlank()) TagsContent.Browse(etiquetas.map(::row))
          else TagsContent.Search(query.trim(), searchEtiquetas(etiquetas, query).map(::row))
    }

private fun TagsUiState.withSource(loaded: TagsSource): TagsUiState =
    copy(content = loaded.content(query), sheet = sheet?.refreshedFrom(loaded))

private fun TagSheet.refreshedFrom(loaded: TagsSource): TagSheet {
  val ready = loaded as? TagsSource.Ready
  val stored = editing?.let { ready?.find(it.id) }
  return if (ready == null || stored == null) this
  else copy(editing = stored, count = ready.count(stored.id))
}
