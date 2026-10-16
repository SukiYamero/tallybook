package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
// The screen callbacks and source acceptance share one synchronous input/session owner.
@Suppress("TooManyFunctions")
internal class SearchViewModel(
    private val repositories: SearchRepositories,
    private val savedStateHandle: SavedStateHandle,
    unknownCategoryLabel: String,
    private val dateProvider: SearchDateProvider,
    private val computationDispatcher: CoroutineDispatcher,
    scope: CoroutineScope? = null,
) : ViewModel(), SearchActions {
  private val initialToday = dateProvider.today()
  private val restored = restoreSearchInput(savedStateHandle, initialToday)
  private val inputs =
      MutableStateFlow(
          SearchInputs(
              restored.rawQuery,
              normalizeSearchText(restored.rawQuery),
              normalizeSearchText(restored.rawQuery),
              restored.filters,
              initialToday,
              label = unknownCategoryLabel,
          )
      )
  private var observationEpoch = 0L
  private var candidateGeneration = 0L
  private var nextSessionId = 0L
  private var preferenceLoad: SearchSourceLoad<AppPreferencesState> = SearchSourceLoad.Loading
  private var categoryLoad: SearchSourceLoad<List<Categoria>> = SearchSourceLoad.Loading
  private var readLoad: SearchRead = SearchRead(null, SearchSourceLoad.Loading)

  val uiState: StateFlow<SearchUiState>
    field = MutableStateFlow(SearchUiState(restored.rawQuery, restored.filters, initialToday))

  init {
    require(unknownCategoryLabel.isNotBlank()) {
      "Search fallback label must be localized and nonblank"
    }
    installSearchCheckpoint(savedStateHandle) {
      val input = inputs.value
      input.filters.savedInput(input.rawQuery)
    }
  }

  private val producer =
      (scope ?: viewModelScope).launch(start = CoroutineStart.UNDISPATCHED) {
        launch {
          inputs
              .map { SearchTextEdit(it.normalizedQuery, it.textRevision) }
              .distinctUntilChanged()
              .collectLatest { edit ->
                if (edit.normalized.isNotEmpty() && edit.revision != inputs.value.settledRevision)
                    delay(TEXT_DEBOUNCE_MILLIS)
                if (edit.revision == inputs.value.textRevision)
                    changeInputs {
                      it.copy(settledQuery = edit.normalized, settledRevision = edit.revision)
                    }
              }
        }
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)
            .command(uiState.subscriptionCount)
            .onEach { command ->
              observationEpoch += 1
              candidateGeneration += 1
              if (command == SharingCommand.START) {
                val today = dateProvider.today()
                changeInputs { it.copy(today = today) }
              }
              updatePending(SearchWork.Revalidating)
            }
            .collectLatest { command ->
              if (command == SharingCommand.START) {
                val epoch = observationEpoch
                observeSearch(repositories, inputs, computationDispatcher, receiver(epoch))
                    .collect { frame -> acceptFrame(epoch, frame) }
              }
            }
      }

  override val hasActiveFilterSession: Boolean
    get() = inputs.value.session != SearchFilterSession.Hidden

  override fun onQueryChanged(query: String) {
    val normalized = normalizeSearchText(query)
    changeInputs {
      val revision = it.textRevision + if (normalized != it.normalizedQuery) 1 else 0
      it.copy(
          rawQuery = query,
          normalizedQuery = normalized,
          settledQuery = if (normalized.isEmpty()) "" else it.settledQuery,
          textRevision = revision,
          settledRevision = if (normalized.isEmpty()) revision else it.settledRevision,
      )
    }
  }

  override fun onClearQuery() = onQueryChanged("")

  override fun onRemoveDateFilter() = changeInputs {
    it.copy(filters = it.filters.copy(preset = SearchDatePreset.All))
  }

  override fun onRemoveTypeFilter() = changeInputs {
    it.copy(filters = it.filters.copy(tipo = null))
  }

  override fun onRemoveCategoryFilter(id: String) = changeInputs {
    it.copy(filters = it.filters.copy(categoryIds = it.filters.categoryIds - id))
  }

  override fun onShowCategory(id: String) {
    if (id.isBlank()) return
    onQueryChanged("")
    val today = dateProvider.today()
    changeInputs {
      val session = it.session
      it.copy(
          today = today,
          filters = SearchFilters.initial(today).copy(categoryIds = setOf(id)),
          session =
              if (session is SearchFilterSession.Editing) session.cancel(session.sessionId)
              else session,
      )
    }
  }

  override fun onRetry() = changeInputs { it.copy(reload = it.reload + 1) }

  override fun onResume() {
    val today = dateProvider.today()
    changeInputs { current ->
      val session = current.session
      current.copy(
          today = today,
          reload = current.reload + 1,
          session =
              if (session is SearchFilterSession.Editing) {
                session.copy(
                    dateFailure =
                        (resolveSearchFilters(session.draft, today)
                                as? SearchFilterResolution.Invalid)
                            ?.failure
                )
              } else session,
      )
    }
  }

  override fun onPresentationChanged(unknownCategoryLabel: String, localeTag: String) {
    if (unknownCategoryLabel.isNotBlank())
        changeInputs { it.copy(label = unknownCategoryLabel, localeTag = localeTag) }
  }

  override fun onOpenFilters(): SearchFilterSessionId? {
    if (hasActiveFilterSession || !uiState.value.canOpenFilters) return null
    val id = SearchFilterSessionId(++nextSessionId)
    val minimumYear = uiState.value.availableYears.minOrNull()
    changeInputs {
      it.copy(session = it.session.open(id, it.filters), sessionMinimumYear = minimumYear)
    }
    return id
  }

  override fun onFilterDraftChanged(id: SearchFilterSessionId, filters: SearchFilters) =
      changeInputs {
        it.copy(session = it.session.edit(id) { filters })
      }

  override fun onClearFilters(id: SearchFilterSessionId) {
    val session = inputs.value.session
    if (session !is SearchFilterSession.Editing || session.sessionId != id) return
    val today = dateProvider.today()
    changeInputs {
      val transition = it.session.clear(id, today)
      it.copy(
          today = today,
          session = transition.session,
          filters = transition.committed ?: it.filters,
      )
    }
  }

  override fun onApplyFilters(id: SearchFilterSessionId) {
    val session = inputs.value.session
    if (session !is SearchFilterSession.Editing || session.sessionId != id) return
    val today = dateProvider.today()
    changeInputs {
      val transition = it.session.apply(id, today)
      it.copy(
          today = today,
          session = transition.session,
          filters = transition.committed ?: it.filters,
      )
    }
  }

  override fun onCancelFilters(id: SearchFilterSessionId) = changeInputs {
    it.copy(session = it.session.cancel(id))
  }

  override fun onFiltersHidden(id: SearchFilterSessionId) = changeInputs {
    val session = it.session.hidden(id)
    it.copy(
        session = session,
        sessionMinimumYear =
            if (session == SearchFilterSession.Hidden) null else it.sessionMinimumYear,
    )
  }

  private fun changeInputs(transform: (SearchInputs) -> SearchInputs) {
    val previous = inputs.value
    inputs.update(transform)
    val current = inputs.value
    if (previous == current) return
    val oldCriteria = uiState.value.criteria
    uiState.update {
      it.copy(
          rawQuery = current.rawQuery,
          filters = current.filters,
          today = current.today,
          filterSession = current.session,
          sessionMinimumYear = current.sessionMinimumYear,
      )
    }
    val sourceChanged = oldCriteria != uiState.value.criteria || previous.reload != current.reload
    val queryChanged =
        previous.textRevision != current.textRevision ||
            previous.settledRevision != current.settledRevision
    val presentationChanged =
        previous.label != current.label || previous.localeTag != current.localeTag
    val changed = sourceChanged || queryChanged || presentationChanged
    if (changed) {
      candidateGeneration += if (presentationChanged) 1 else 0
      val work =
          uiState.value.pendingWork(sourceChanged, presentationChanged, current.isTextPending)
      updatePending(work)
    }
  }

  private fun receiver(epoch: Long) =
      object : SearchObservationReceiver {
        override fun onPreferences(reload: Long, load: SearchSourceLoad<AppPreferencesState>) {
          if (!isActive(epoch, reload)) return
          candidateGeneration += 1
          preferenceLoad = load
          val currency = (load.data as? AppPreferencesState.Ready)?.value?.monedaPrincipal
          uiState.update { state ->
            state.copy(
                moneda = currency ?: state.moneda,
                years = if (currency != null && currency != state.moneda) null else state.years,
                preferenceFailure = preferenceFailure(),
            )
          }
          updatePending(SearchWork.Reading)
        }

        override fun onCategories(reload: Long, load: SearchSourceLoad<List<Categoria>>) {
          if (!isActive(epoch, reload)) return
          candidateGeneration += 1
          categoryLoad = load
          uiState.update { state ->
            state.copy(
                categories =
                    SearchCategoryState(
                        load.data ?: state.categories.values,
                        load is SearchSourceLoad.Loading,
                        load.failure,
                    )
            )
          }
          updatePending(SearchWork.Reading)
        }

        override fun onRead(read: SearchRead) {
          if (epoch != observationEpoch || read.request?.let { !requestMatches(it) } == true) return
          candidateGeneration += 1
          readLoad = read
          val snapshot = read.load.data
          if (snapshot != null && read.request != null)
              uiState.update { it.copy(years = read.years) }
          updatePending(SearchWork.Reading)
        }

        override fun onCandidateGeneration(request: SearchReadRequest?): Long {
          if (epoch == observationEpoch && (request == null || requestMatches(request))) {
            candidateGeneration += 1
            updatePending(SearchWork.Matching)
          }
          return candidateGeneration
        }

        override fun onMatching(
            request: SearchReadRequest,
            generation: Long,
            normalizedQuery: String,
        ) {
          if (!isActive(epoch, request.reload) || !requestMatches(request)) return
          if (generation != candidateGeneration || normalizedQuery != inputs.value.normalizedQuery)
              return
          updatePending(SearchWork.Matching)
        }

        override fun onWaitingText(
            request: SearchReadRequest,
            generation: Long,
            textRevision: Long,
        ) {
          if (!isActive(epoch, request.reload) || !requestMatches(request)) return
          if (generation != candidateGeneration || textRevision != inputs.value.textRevision) return
          if (inputs.value.isTextPending) updatePending(SearchWork.WaitingText)
        }
      }

  private fun preferenceFailure(): DataError? =
      preferenceLoad.failure
          ?: if (preferenceLoad.data == AppPreferencesState.Uninitialized) {
            DataError.InvalidInput("Preferences are uninitialized after app startup")
          } else null

  private fun failure(): SearchFailure? {
    val invalid =
        resolveSearchFilters(inputs.value.filters, inputs.value.today)
            as? SearchFilterResolution.Invalid
    return invalid?.let { SearchFailure.InvalidDates(it.failure) }
        ?: preferenceFailure()?.let { SearchFailure.Repository(SearchSource.Preferences, it) }
        ?: categoryLoad.failure?.let { SearchFailure.Repository(SearchSource.Categories, it) }
        ?: readLoad
            .takeIf { it.request?.let(::requestMatches) == true }
            ?.load
            ?.failure
            ?.let { SearchFailure.Repository(SearchSource.Movimientos, it) }
  }

  private fun updatePending(work: SearchWork) {
    uiState.update { state ->
      val previous = state.displayResults
      val failure = failure()
      if (failure != null)
          state.copy(
              results =
                  RetainedLoadState.Failed(
                      failure,
                      previous?.takeIf {
                        it.request.normalizedQuery == inputs.value.normalizedQuery
                      },
                  ),
              work = null,
          )
      else
          state.copy(
              results = RetainedLoadState.Loading(previous),
              work = work,
          )
    }
  }

  private fun acceptFrame(epoch: Long, frame: SearchFrame) {
    val request = frame.request ?: return
    val sourceCurrent = isActive(epoch, request.reload) && requestMatches(request)
    val candidateCurrent =
        frame.generation == candidateGeneration &&
            frame.normalizedQuery == inputs.value.normalizedQuery
    val textCurrent =
        frame.textRevision == inputs.value.textRevision &&
            frame.textRevision == inputs.value.settledRevision
    val requestCurrent = sourceCurrent && candidateCurrent && textCurrent
    if (!requestCurrent || failure() != null) return
    if (frame.result != null)
        uiState.update {
          it.copy(
              results = RetainedLoadState.Ready(frame.result),
              years = frame.result.years,
              work = null,
          )
        }
    else if (frame.failure != null)
        uiState.update { state ->
          state.copy(
              results =
                  RetainedLoadState.Failed(
                      frame.failure,
                      state.displayResults?.takeIf {
                        it.request.normalizedQuery == frame.normalizedQuery
                      },
                  ),
              work = null,
          )
        }
  }

  private fun requestMatches(request: SearchReadRequest): Boolean =
      request.reload == inputs.value.reload && request.criteria == uiState.value.criteria

  private fun isActive(epoch: Long, reload: Long): Boolean =
      epoch == observationEpoch && reload == inputs.value.reload

  override fun onCleared() {
    producer.cancel()
    savedStateHandle.clearSavedStateProvider(SEARCH_SAVED_INPUT_KEY)
  }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val TEXT_DEBOUNCE_MILLIS = 250L

private fun SearchUiState.pendingWork(
    sourceChanged: Boolean,
    presentationChanged: Boolean,
    textPending: Boolean,
): SearchWork =
    when {
      sourceChanged -> SearchWork.Reading
      presentationChanged -> SearchWork.Matching
      textPending ->
          work ?: if (displayResults != null) SearchWork.WaitingText else SearchWork.Reading
      else -> SearchWork.Matching
    }
