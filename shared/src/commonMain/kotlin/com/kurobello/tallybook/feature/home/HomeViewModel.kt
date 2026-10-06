package com.kurobello.tallybook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val movimientoRepository: MovimientoRepository,
    private val categoriaRepository: CategoriaRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {

  private val retryRequests = MutableStateFlow(0L)
  private var observationEpoch = 0L
  val uiState: StateFlow<HomeUiState>
    field = MutableStateFlow(HomeUiState.loading())

  private val producer =
      (scope ?: viewModelScope).launch(start = CoroutineStart.UNDISPATCHED) {
        SharingStarted.WhileSubscribed(HOME_STOP_TIMEOUT_MILLIS)
            .command(uiState.subscriptionCount)
            .onEach {
              observationEpoch += 1
              uiState.update { it.revalidating() }
            }
            .collectLatest { command ->
              if (command == SharingCommand.START) {
                val epoch = observationEpoch
                retryRequests
                    .flatMapLatest { revision -> observeHomeState().map { revision to it } }
                    .collect { (revision, state) ->
                      uiState.update { current ->
                        when {
                          epoch != observationEpoch || revision != retryRequests.value -> current
                          state.isLoading &&
                              state.summary.balance.moneda == current.summary.balance.moneda ->
                              current.revalidating()
                          else -> state
                        }
                      }
                    }
              }
            }
      }

  fun retry() {
    retryRequests.update { it + 1 }
    uiState.update { it.revalidating() }
  }

  fun onResume() = retry()

  override fun onCleared() {
    producer.cancel()
  }

  private fun observeHomeState(): Flow<HomeUiState> =
      preferencesRepository.preferences.flatMapLatest { it.toHomeStateFlow() }

  private fun DataOutcome<AppPreferencesState>.toHomeStateFlow(): Flow<HomeUiState> =
      when (this) {
        is DataOutcome.Failure -> HomeUiState.error(error).asFlow()
        is DataOutcome.Success ->
            when (data) {
              AppPreferencesState.Uninitialized ->
                  HomeUiState.error(
                          DataError.InvalidInput("Preferences are uninitialized after app startup")
                      )
                      .asFlow()
              is AppPreferencesState.Ready -> observePersistedHome(data.value.monedaPrincipal)
            }
      }

  private fun observePersistedHome(moneda: Moneda): Flow<HomeUiState> =
      combine(
          movimientoRepository.observeSummary(moneda).toLoadState(),
          movimientoRepository.observeRecent(moneda, HOME_RECENT_LIMIT).toLoadState(),
          categoriaRepository.observeAll().toLoadState(),
      ) { summary, recent, categorias ->
        HomeSnapshot.from(summary, recent, categorias).toUiState(moneda)
      }
}

private sealed interface LoadState<out T> {
  data object Loading : LoadState<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : LoadState<T>
}

private fun <T> Flow<DataOutcome<T>>.toLoadState(): Flow<LoadState<T>> =
    map<DataOutcome<T>, LoadState<T>> { LoadState.Value(it) }.onStart { emit(LoadState.Loading) }

private fun HomeUiState.asFlow(): Flow<HomeUiState> = kotlinx.coroutines.flow.flowOf(this)

private data class HomeSnapshot(
    val summary: MovimientoSummary?,
    val recent: List<Movimiento>?,
    val categoriasById: Map<String, com.kurobello.tallybook.core.model.Categoria>?,
    val error: DataError?,
    val isLoading: Boolean,
) {
  fun toUiState(moneda: Moneda): HomeUiState =
      when {
        error != null -> HomeUiState.error(error)
        isLoading -> HomeUiState.loading(moneda)
        else ->
            HomeUiState(
                isLoading = false,
                summary = requireNotNull(summary),
                recent = requireNotNull(recent),
                categoriasById = requireNotNull(categoriasById),
                error = null,
            )
      }

  companion object {
    fun from(
        summary: LoadState<MovimientoSummary>,
        recent: LoadState<List<Movimiento>>,
        categorias: LoadState<List<com.kurobello.tallybook.core.model.Categoria>>,
    ): HomeSnapshot =
        HomeSnapshot(
            summary = summary.successOrNull(),
            recent = recent.successOrNull(),
            categoriasById = categorias.successOrNull()?.associateBy { it.id },
            error = summary.errorOrNull() ?: recent.errorOrNull() ?: categorias.errorOrNull(),
            isLoading =
                summary is LoadState.Loading ||
                    recent is LoadState.Loading ||
                    categorias is LoadState.Loading,
        )
  }
}

private fun <T> LoadState<T>.successOrNull(): T? =
    (this as? LoadState.Value)?.outcome.let { it as? DataOutcome.Success }?.data

private fun <T> LoadState<T>.errorOrNull(): DataError? =
    (this as? LoadState.Value)?.outcome.let { it as? DataOutcome.Failure }?.error

private const val HOME_RECENT_LIMIT = 6

private const val HOME_STOP_TIMEOUT_MILLIS = 5_000L

internal fun HomeUiState.revalidating(): HomeUiState = copy(isLoading = true, error = null)
