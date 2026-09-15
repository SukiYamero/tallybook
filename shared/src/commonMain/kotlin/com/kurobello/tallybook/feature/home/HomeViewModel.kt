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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val movimientoRepository: MovimientoRepository,
    private val categoriaRepository: CategoriaRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {

  private val coroutineScope = scope ?: viewModelScope
  private val retryRequests = MutableStateFlow(0)

  val uiState =
      retryRequests
          .flatMapLatest { observeHomeState() }
          .stateIn(coroutineScope, SharingStarted.Eagerly, HomeUiState.loading())

  fun retry() {
    retryRequests.update { it + 1 }
  }

  private fun observeHomeState(): Flow<HomeUiState> =
      preferencesRepository.preferences
          .map<DataOutcome<AppPreferencesState>, PreferenceLoadState> {
            PreferenceLoadState.Value(it)
          }
          .onStart { emit(PreferenceLoadState.Loading) }
          .flatMapLatest { preferenceState ->
            when (preferenceState) {
              PreferenceLoadState.Loading -> HomeUiState.loading().asFlow()
              is PreferenceLoadState.Value -> preferenceState.outcome.toHomeStateFlow()
            }
          }

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
          movimientoRepository.observeRecent(HOME_RECENT_LIMIT).toLoadState(),
          categoriaRepository.observeAll().toLoadState(),
      ) { summary, recent, categorias ->
        HomeSnapshot.from(moneda, summary, recent, categorias).toUiState()
      }
}

private sealed interface PreferenceLoadState {
  data object Loading : PreferenceLoadState

  data class Value(val outcome: DataOutcome<AppPreferencesState>) : PreferenceLoadState
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
  fun toUiState(): HomeUiState =
      when {
        error != null -> HomeUiState.error(error)
        isLoading -> HomeUiState.loading()
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
        moneda: Moneda,
        summary: LoadState<MovimientoSummary>,
        recent: LoadState<List<Movimiento>>,
        categorias: LoadState<List<com.kurobello.tallybook.core.model.Categoria>>,
    ): HomeSnapshot =
        HomeSnapshot(
            summary = summary.successOrNull(),
            recent =
                recent
                    .successOrNull()
                    ?.filter { it.monto.moneda == moneda }
                    ?.take(HOME_RECENT_LIMIT),
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
