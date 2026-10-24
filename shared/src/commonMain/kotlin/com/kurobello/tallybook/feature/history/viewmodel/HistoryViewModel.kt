package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.monedas
import com.kurobello.tallybook.core.model.viewedMoneda
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.state.failed
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

internal data class HistoryRepositories(
    val preferences: AppPreferencesRepository,
    val actividades: ActividadRepository,
    val categorias: CategoriaRepository,
    val cuentas: CuentaRepository,
)

@OptIn(ExperimentalCoroutinesApi::class)
// The screen callbacks and source acceptance share one synchronous input owner.
@Suppress("TooManyFunctions")
internal class HistoryViewModel(
    private val repositories: HistoryRepositories,
    private val savedStateHandle: SavedStateHandle,
    unknownCategoryLabel: String,
    private val dateProvider: HistoryDateProvider,
    private val computationDispatcher: CoroutineDispatcher,
    scope: CoroutineScope? = null,
) : ViewModel(), HistoryActions {
  private val initialToday = dateProvider.today()
  private val restored = restoreHistoryCheckpoint(savedStateHandle, initialToday)
  private val initialSelection = restored.selection
  private val inputs =
      MutableStateFlow(
          HistoryInputs(
              initialSelection,
              initialToday,
              unknownCategoryLabel,
              setOf(initialSelection.anchor.year),
              selectedMoneda = restored.moneda,
          )
      )
  private val observationEpoch = MutableStateFlow(0L)
  val uiState: StateFlow<HistoryUiState>
    field =
        MutableStateFlow(
            HistoryUiState(
                initialSelection,
                initialToday,
                years = HistoryYearState(retained = inputs.value.retainedYears),
            )
        )

  init {
    require(unknownCategoryLabel.isNotBlank()) {
      "History fallback label must be localized and nonblank"
    }
    installHistoryCheckpoint(
        savedStateHandle,
        { inputs.value.selection },
        { inputs.value.selectedMoneda },
    )
  }

  private val producer =
      (scope ?: viewModelScope).launch(start = CoroutineStart.UNDISPATCHED) {
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)
            .command(uiState.subscriptionCount)
            .onEach { command ->
              observationEpoch.update { it + 1 }
              if (command == SharingCommand.START) {
                val today = dateProvider.today()
                changeInputs {
                  it.copy(selection = it.selection.refresh(it.today, today), today = today)
                }
              }
              // Invalidation precedes cancellation, which can wait for a pending
              // database/computation call.
              uiState.update { it.revalidating() }
            }
            .collectLatest { command ->
              if (command == SharingCommand.START) {
                val epoch = observationEpoch.value
                observeHistory(
                        repositories,
                        inputs,
                        computationDispatcher,
                        HistoryReceiver(
                            onPreference = { load, reload ->
                              acceptPreferences(epoch, reload, load)
                            },
                            onCuentas = ::acceptCuentas,
                        ),
                    )
                    .collect { frame -> acceptFrame(epoch, frame) }
              }
            }
      }

  override fun onScopeChanged(scope: HistoryScope) {
    changeInputs { it.copy(selection = it.selection.selectScope(scope)) }
  }

  override fun onYearSelected(year: Int) {
    changeInputs { current ->
      if (
          year in
              current.selection.availableYears(
                  uiState.value.years.persisted,
                  current.today,
                  current.retainedYears,
              )
      ) {
        current.copy(selection = current.selection.selectYear(year, current.today))
      } else current
    }
  }

  override fun onPeriodSelected(period: MovimientoDateRange) {
    changeInputs { current ->
      if (period in current.selection.periods(current.today)) {
        current.copy(selection = current.selection.selectPeriod(period, current.today))
      } else current
    }
  }

  override fun onMonedaSelected(moneda: Moneda) {
    changeInputs { current ->
      if (current.monedas?.contains(moneda) != false) current.copy(selectedMoneda = moneda)
      else current
    }
  }

  override fun onRetry() {
    changeInputs { it.copy(reload = it.reload + 1) }
  }

  override fun onResume() {
    val today = dateProvider.today()
    changeInputs {
      it.copy(
          selection = it.selection.refresh(it.today, today),
          today = today,
          reload = it.reload + 1,
      )
    }
  }

  override fun onPresentationChanged(unknownCategoryLabel: String) {
    if (unknownCategoryLabel.isNotBlank()) changeInputs { it.copy(label = unknownCategoryLabel) }
  }

  private fun changeInputs(transform: (HistoryInputs) -> HistoryInputs) {
    val previous = inputs.value
    inputs.update { current ->
      val next = transform(current)
      next.copy(retainedYears = current.retainedYears + next.selection.anchor.year)
    }
    val current = inputs.value
    if (current == previous) return
    uiState.update { state ->
      val rangeChanged =
          previous.selection.range(previous.today) != current.selection.range(current.today)
      val presentationChanged = previous.label != current.label
      val reloadChanged = previous.reload != current.reload
      val viewed = state.primary?.let { current.viewedMoneda(it) } ?: state.moneda
      val monedaChanged = viewed != state.moneda
      state.copy(
          selection = current.selection,
          today = current.today,
          moneda = viewed,
          monedas = current.monedas.orEmpty(),
          years =
              if (monedaChanged) HistoryYearState(retained = current.retainedYears)
              else state.years.copy(retained = current.retainedYears),
          period =
              when {
                rangeChanged || monedaChanged -> RetainedLoadState.Loading()
                presentationChanged || reloadChanged -> state.period.refreshing()
                else -> state.period
              },
          preferenceFailure = if (reloadChanged) null else state.preferenceFailure,
      )
    }
  }

  // A missing selected currency is dropped rather than kept for a later account in it.
  private fun acceptCuentas(load: SourceLoad<List<Cuenta>>) {
    val monedas = load.data?.monedas() ?: return
    changeInputs { current ->
      current.copy(
          monedas = monedas,
          selectedMoneda = current.selectedMoneda?.takeIf { it in monedas },
      )
    }
  }

  private fun acceptPreferences(epoch: Long, reload: Long, load: SourceLoad<AppPreferencesState>) {
    val primary = load.readyCurrency
    val moneda = primary?.let { inputs.value.viewedMoneda(it) }
    val failure = load.preferenceFailure
    uiState.update { state ->
      when {
        epoch != observationEpoch.value || reload != inputs.value.reload -> state
        failure != null ->
            state.copy(
                preferenceFailure = failure,
                period = state.period.failed(HistoryFailure.Repository(failure)),
            )
        moneda != null && moneda != state.moneda ->
            state.copy(
                moneda = moneda,
                primary = primary,
                preferenceFailure = null,
                period = RetainedLoadState.Loading(),
                years = HistoryYearState(retained = inputs.value.retainedYears),
            )
        else -> state.copy(primary = primary ?: state.primary, preferenceFailure = null)
      }
    }
  }

  private fun acceptFrame(epoch: Long, frame: HistoryFrame) {
    uiState.update { state ->
      val current = inputs.value
      if (
          epoch != observationEpoch.value ||
              frame.request.reload != current.reload ||
              state.preferenceFailure != null
      )
          state
      else if (frame.request.moneda != state.moneda) state
      else {
        val periodMatches = frame.request.range == state.range && frame.label == current.label
        state.copy(
            period =
                if (periodMatches) frame.period.retainPrevious(state.period.cachedValue)
                else state.period,
            years =
                state.years.copy(
                    persisted = frame.years.data ?: state.years.persisted,
                    retained = current.retainedYears,
                    isLoading = frame.years is SourceLoad.Loading,
                    failure = frame.years.failure,
                ),
        )
      }
    }
  }

  override fun onCleared() {
    producer.cancel()
    savedStateHandle.clearSavedStateProvider(HISTORY_SAVED_INPUT_KEY)
  }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

private data class HistoryInputs(
    val selection: HistorySelection,
    val today: LocalDate,
    val label: String,
    val retainedYears: Set<Int>,
    val reload: Long = 0,
    val selectedMoneda: Moneda? = null,
    val monedas: List<Moneda>? = null,
) {
  fun viewedMoneda(primary: Moneda): Moneda = viewedMoneda(primary, selectedMoneda, monedas)
}

private class HistoryReceiver(
    val onPreference: (SourceLoad<AppPreferencesState>, Long) -> Unit,
    val onCuentas: (SourceLoad<List<Cuenta>>) -> Unit,
)

private data class RangeRequest(
    val moneda: Moneda,
    val range: MovimientoDateRange,
    val reload: Long,
)

private data class RangeLoad(val request: RangeRequest, val load: SourceLoad<List<Actividad>>)

private data class HistorySources(
    val range: RangeLoad,
    val categories: SourceLoad<List<Categoria>>,
    val cuentas: SourceLoad<List<Cuenta>>,
    val years: SourceLoad<List<Int>>,
    val label: String,
)

private data class HistoryFrame(
    val request: RangeRequest,
    val period: RetainedLoadState<HistoryProjection.Content, HistoryFailure>,
    val years: SourceLoad<List<Int>>,
    val label: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeHistory(
    repositories: HistoryRepositories,
    inputs: StateFlow<HistoryInputs>,
    dispatcher: CoroutineDispatcher,
    receiver: HistoryReceiver,
): Flow<HistoryFrame> =
    inputs
        .map { it.reload }
        .distinctUntilChanged()
        .flatMapLatest { reload ->
          repositories.preferences.preferences
              .toSourceLoad()
              .onEach { receiver.onPreference(it, reload) }
              .flatMapLatest { preference ->
                val primary = preference.readyCurrency
                if (primary == null) emptyFlow()
                else
                    inputs
                        .map { it.viewedMoneda(primary) }
                        .distinctUntilChanged()
                        .flatMapLatest { moneda ->
                          observeCurrency(repositories, inputs, moneda, receiver)
                        }
                        .mapLatest { sources -> sources.toFrame(dispatcher) }
              }
        }

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeCurrency(
    repositories: HistoryRepositories,
    inputs: StateFlow<HistoryInputs>,
    moneda: Moneda,
    receiver: HistoryReceiver,
): Flow<HistorySources> =
    combine(
        inputs
            .map { RangeRequest(moneda, it.selection.range(it.today), it.reload) }
            .distinctUntilChanged()
            .flatMapLatest { request ->
              repositories.actividades
                  .observeActividadRange(moneda, request.range)
                  .toSourceLoad()
                  .map { RangeLoad(request, it) }
            },
        repositories.categorias.observeAll().toSourceLoad(),
        repositories.cuentas.observeAll().toSourceLoad().onEach(receiver.onCuentas),
        repositories.actividades.observeYears(moneda).toSourceLoad(),
        inputs.map { it.label }.distinctUntilChanged(),
    ) { range, categories, cuentas, years, label ->
      HistorySources(range, categories, cuentas, years, label)
    }

private sealed interface SourceLoad<out T> {
  data object Loading : SourceLoad<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : SourceLoad<T>
}

private fun <T> Flow<DataOutcome<T>>.toSourceLoad(): Flow<SourceLoad<T>> =
    map<DataOutcome<T>, SourceLoad<T>> { SourceLoad.Value(it) }.onStart { emit(SourceLoad.Loading) }

private val <T> SourceLoad<T>.data: T?
  get() =
      when (this) {
        SourceLoad.Loading -> null
        is SourceLoad.Value ->
            when (val result = outcome) {
              is DataOutcome.Success -> result.data
              is DataOutcome.Failure -> null
            }
      }

private val <T> SourceLoad<T>.failure: DataError?
  get() =
      when (this) {
        SourceLoad.Loading -> null
        is SourceLoad.Value ->
            when (val result = outcome) {
              is DataOutcome.Success -> null
              is DataOutcome.Failure -> result.error
            }
      }

private val SourceLoad<AppPreferencesState>.readyCurrency: Moneda?
  get() =
      when (val value = data) {
        null,
        AppPreferencesState.Uninitialized -> null
        is AppPreferencesState.Ready -> value.value.monedaPrincipal
      }

private val SourceLoad<AppPreferencesState>.preferenceFailure: DataError?
  get() =
      failure
          ?: if (data == AppPreferencesState.Uninitialized) {
            DataError.InvalidInput("Preferences are uninitialized after app startup")
          } else null

private fun <T : Any, E : Any> RetainedLoadState<T, E>.retainPrevious(
    previous: T?
): RetainedLoadState<T, E> =
    when (this) {
      is RetainedLoadState.Loading -> copy(previous = previous)
      is RetainedLoadState.Failed -> copy(previous = previous)
      is RetainedLoadState.Ready -> this
    }

internal class HistoryDateProvider(
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
) {
  fun today(): LocalDate {
    val now = clock.now()
    val zone = timeZoneProvider.current()
    return now.toLocalDateTime(zone).date
  }
}

private fun HistoryUiState.revalidating(): HistoryUiState =
    copy(
        period = period.refreshing(),
        years = years.copy(isLoading = true),
    )

private suspend fun HistorySources.toFrame(dispatcher: CoroutineDispatcher): HistoryFrame {
  val failure = range.load.failure ?: categories.failure ?: cuentas.failure
  val rows = range.load.data
  val categoryValues = categories.data
  val cuentaValues = cuentas.data
  val period: RetainedLoadState<HistoryProjection.Content, HistoryFailure> =
      when {
        failure != null -> RetainedLoadState.Failed(HistoryFailure.Repository(failure))
        rows == null || categoryValues == null || cuentaValues == null ->
            RetainedLoadState.Loading()
        else ->
            when (
                val projection =
                    withContext(dispatcher) {
                      projectHistory(
                          range.request.moneda,
                          range.request.range,
                          rows,
                          categoryValues.associateBy { it.id },
                          label,
                          cuentaValues.associateBy { it.id },
                      )
                    }
            ) {
              is HistoryProjection.Content -> RetainedLoadState.Ready(projection)
              is HistoryProjection.Failure -> RetainedLoadState.Failed(projection.failure)
            }
      }
  return HistoryFrame(range.request, period, years, label)
}
