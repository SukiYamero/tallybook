package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.feature.search.ui.SearchCurrencyYears
import com.kurobello.tallybook.feature.search.ui.SearchRequest
import com.kurobello.tallybook.feature.search.ui.SearchResultsUi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal data class SearchRepositories(
    val preferences: AppPreferencesRepository,
    val movimientos: MovimientoRepository,
    val categorias: CategoriaRepository,
)

internal data class SearchInputs(
    val rawQuery: String,
    val normalizedQuery: String,
    val settledQuery: String,
    val filters: SearchFilters,
    val today: LocalDate,
    val session: SearchFilterSession = SearchFilterSession.Hidden,
    val sessionMinimumYear: Int? = null,
    val label: String,
    val localeTag: String = "",
    val reload: Long = 0,
    val textRevision: Long = 0,
    val settledRevision: Long = 0,
) {
  val isTextPending: Boolean
    get() = textRevision != settledRevision

  fun criteria(moneda: Moneda): MovimientoReadCriteria? {
    val resolution =
        resolveSearchFilters(filters, today) as? SearchFilterResolution.Valid ?: return null
    return MovimientoReadCriteria(moneda, resolution.range, filters.tipo, filters.categoryIds)
  }
}

internal sealed interface SearchSourceLoad<out T> {
  data object Loading : SearchSourceLoad<Nothing>

  data class Ready<T>(val data: T) : SearchSourceLoad<T>

  data class Failed(val error: DataError) : SearchSourceLoad<Nothing>
}

internal val <T> SearchSourceLoad<T>.data: T?
  get() = (this as? SearchSourceLoad.Ready)?.data

internal val SearchSourceLoad<*>.failure: DataError?
  get() = (this as? SearchSourceLoad.Failed)?.error

internal data class SearchReadRequest(val criteria: MovimientoReadCriteria, val reload: Long)

internal data class SearchRead(
    val request: SearchReadRequest?,
    val load: SearchSourceLoad<MovimientoSearchSnapshot>,
    val years: SearchCurrencyYears? = null,
)

internal data class SearchFrame(
    val request: SearchReadRequest?,
    val generation: Long,
    val normalizedQuery: String,
    val textRevision: Long,
    val result: SearchResultsUi? = null,
    val failure: SearchFailure? = null,
)

internal interface SearchObservationReceiver {
  fun onPreferences(reload: Long, load: SearchSourceLoad<AppPreferencesState>)

  fun onCategories(reload: Long, load: SearchSourceLoad<List<Categoria>>)

  fun onRead(read: SearchRead)

  fun onCandidateGeneration(request: SearchReadRequest?): Long

  fun onMatching(request: SearchReadRequest, generation: Long, normalizedQuery: String)

  fun onWaitingText(request: SearchReadRequest, generation: Long, textRevision: Long)
}

private data class CandidateSources(
    val read: SearchRead,
    val categories: SearchSourceLoad<List<Categoria>>,
    val label: String,
)

private data class CandidateFrame(
    val request: SearchReadRequest?,
    val generation: Long,
    val candidates: List<SearchCandidate>? = null,
    val years: SearchCurrencyYears? = null,
    val failure: SearchFailure? = null,
)

internal data class SearchTextEdit(val normalized: String, val revision: Long)

private data class QueryIdentity(
    val normalized: String,
    val settled: String,
    val revision: Long,
    val settledRevision: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeSearch(
    repositories: SearchRepositories,
    inputs: StateFlow<SearchInputs>,
    dispatcher: CoroutineDispatcher,
    receiver: SearchObservationReceiver,
): Flow<SearchFrame> =
    inputs
        .map { it.reload }
        .distinctUntilChanged()
        .flatMapLatest { reload ->
          val categories =
              repositories.categorias.observeAll().sourceLoads().onEach {
                receiver.onCategories(reload, it)
              }
          val reads = observeSearchReads(repositories, inputs, reload, receiver)
          val candidates =
              combine(
                      reads,
                      categories,
                      inputs.map { it.label to it.localeTag }.distinctUntilChanged(),
                  ) { read, catalog, presentation ->
                    CandidateSources(read, catalog, presentation.first)
                  }
                  .mapLatest { it.toCandidates(dispatcher, receiver) }
          combine(
                  candidates,
                  inputs
                      .map {
                        QueryIdentity(
                            it.normalizedQuery,
                            it.settledQuery,
                            it.textRevision,
                            it.settledRevision,
                        )
                      }
                      .distinctUntilChanged(),
              ) { candidate, query ->
                candidate to query
              }
              .mapLatest { (candidate, query) -> candidate.toFrame(query, dispatcher, receiver) }
        }

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeSearchReads(
    repositories: SearchRepositories,
    inputs: StateFlow<SearchInputs>,
    reload: Long,
    receiver: SearchObservationReceiver,
): Flow<SearchRead> {
  val preferences =
      repositories.preferences.preferences.sourceLoads().onEach {
        receiver.onPreferences(reload, it)
      }
  return combine(preferences, inputs) { preference, input ->
        val currency = (preference.data as? AppPreferencesState.Ready)?.value?.monedaPrincipal
        val criteria = currency?.let(input::criteria)
        criteria?.takeIf { input.reload == reload }?.let { SearchReadRequest(it, reload) }
      }
      .distinctUntilChanged()
      .flatMapLatest { request ->
        if (request == null) flowOf(SearchRead(null, SearchSourceLoad.Loading))
        else
            repositories.movimientos.observeFiltered(request.criteria).sourceLoads().map { load ->
              SearchRead(
                  request,
                  load,
                  load.data?.let {
                    SearchCurrencyYears(request.criteria.moneda, it.persistedYears)
                  },
              )
            }
      }
      .onEach(receiver::onRead)
}

private suspend fun CandidateSources.toCandidates(
    dispatcher: CoroutineDispatcher,
    receiver: SearchObservationReceiver,
): CandidateFrame {
  val generation = receiver.onCandidateGeneration(read.request)
  val pending = CandidateFrame(read.request, generation)
  val failure =
      categories.failure?.let { SearchFailure.Repository(SearchSource.Categories, it) }
          ?: read.load.failure?.let { SearchFailure.Repository(SearchSource.Movimientos, it) }
  val request = read.request
  val snapshot = read.load.data
  val catalog = categories.data
  return when {
    failure != null -> pending.copy(failure = failure)
    request == null || snapshot == null || catalog == null -> pending
    else ->
        withContext(dispatcher) {
          CandidateFrame(
              request,
              generation,
              buildSearchCandidates(snapshot.movimientos, catalog, label),
              requireNotNull(read.years),
          )
        }
  }
}

private suspend fun CandidateFrame.toFrame(
    query: QueryIdentity,
    dispatcher: CoroutineDispatcher,
    receiver: SearchObservationReceiver,
): SearchFrame {
  val currentRequest = request
  val rows = candidates
  val pending =
      SearchFrame(currentRequest, generation, query.normalized, query.revision, failure = failure)
  return when {
    failure != null -> pending
    currentRequest == null || rows == null -> pending
    query.revision != query.settledRevision -> {
      receiver.onWaitingText(currentRequest, generation, query.revision)
      pending
    }
    else -> {
      receiver.onMatching(currentRequest, generation, query.settled)
      val matches = withContext(dispatcher) { matchSearchCandidates(rows, query.settled) }
      SearchFrame(
          currentRequest,
          generation,
          query.settled,
          query.revision,
          SearchResultsUi(
              SearchRequest(currentRequest.criteria, query.settled),
              matches,
              requireNotNull(years),
          ),
      )
    }
  }
}

private fun <T> Flow<DataOutcome<T>>.sourceLoads(): Flow<SearchSourceLoad<T>> =
    map<DataOutcome<T>, SearchSourceLoad<T>> {
          when (it) {
            is DataOutcome.Success -> SearchSourceLoad.Ready(it.data)
            is DataOutcome.Failure -> SearchSourceLoad.Failed(it.error)
          }
        }
        .onStart { emit(SearchSourceLoad.Loading) }
        .distinctUntilChanged()
