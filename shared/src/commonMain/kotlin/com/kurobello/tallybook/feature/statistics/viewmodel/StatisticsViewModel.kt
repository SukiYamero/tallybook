package com.kurobello.tallybook.feature.statistics.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.StatisticsRepository
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.fold
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.monedas
import com.kurobello.tallybook.core.model.viewedMoneda
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.state.failed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal data class StatisticsRepositories(
    val preferences: AppPreferencesRepository,
    val statistics: StatisticsRepository,
    val actividades: ActividadRepository,
    val categorias: CategoriaRepository,
    val etiquetas: EtiquetaRepository,
    val cuentas: CuentaRepository,
)

@OptIn(ExperimentalCoroutinesApi::class)
// The screen and sheet callbacks share one synchronous input owner.
@Suppress("TooManyFunctions")
internal class StatisticsViewModel(
    private val repositories: StatisticsRepositories,
    private val savedStateHandle: SavedStateHandle,
    private val civilToday: CivilToday,
    private val computationDispatcher: CoroutineDispatcher,
    scope: CoroutineScope? = null,
) : ViewModel(), StatisticsActions {
  private val initialToday = civilToday.date()
  private val restored = restoreStatisticsInput(savedStateHandle, initialToday)
  private val inputs =
      MutableStateFlow(
          StatisticsInputs(
              selection = restored.selection,
              today = initialToday,
              selectedMoneda = restored.moneda,
              retainedYears = setOf(restored.selection.anchor.year),
          )
      )
  private val effectChannel = Channel<StatisticsEffect>(Channel.BUFFERED)
  val effects: Flow<StatisticsEffect> = effectChannel.receiveAsFlow()

  val uiState: StateFlow<StatisticsUiState>
    field =
        MutableStateFlow(
            StatisticsUiState(
                selection = inputs.value.selection,
                today = initialToday,
                years = inputs.value.availableYears,
                dimension = restored.dimension,
            )
        )

  init {
    installStatisticsCheckpoint(savedStateHandle) {
      statisticsSavedInput(
          inputs.value.selection,
          inputs.value.selectedMoneda,
          uiState.value.dimension,
      )
    }
  }

  private val producer =
      (scope ?: viewModelScope).launch(start = CoroutineStart.UNDISPATCHED) {
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)
            .command(uiState.subscriptionCount)
            .onEach { command -> if (command == SharingCommand.START) refreshDay() }
            .collectLatest { command -> if (command == SharingCommand.START) observe() }
      }

  override fun onScopeChanged(scope: StatisticsScope) {
    changeInputs { it.copy(selection = it.selection.selectScope(scope)) }
  }

  override fun onPeriodSelected(period: MovimientoDateRange) {
    changeInputs { current ->
      if (period in current.selection.periods(current.today, current.availableYears))
          current.copy(selection = current.selection.selectPeriod(period, current.today))
      else current
    }
  }

  override fun onYearSelected(year: Int) {
    changeInputs { current ->
      if (year in current.availableYears)
          current.copy(selection = current.selection.selectYear(year, current.today))
      else current
    }
  }

  override fun onMonedaSelected(moneda: Moneda) {
    changeInputs { current ->
      if (current.monedas?.contains(moneda) != false) current.copy(selectedMoneda = moneda)
      else current
    }
  }

  override fun onDimensionSelected(dimension: StatisticsDimension) {
    uiState.update { it.copy(dimension = dimension) }
  }

  override fun onTrendMonthSelected(index: Int) {
    uiState.update { state ->
      val months = state.content.cachedValue?.trend.orEmpty()
      if (index in months.indices) state.copy(trendSelection = index) else state
    }
  }

  override fun onShareSelected(key: StatisticsKey) {
    when (key) {
      is StatisticsKey.Categoria -> openDetail(key.id)
      is StatisticsKey.Etiqueta,
      is StatisticsKey.Cuenta -> searchFor(key)?.let(effectChannel::trySend)
      is StatisticsKey.Medio -> Unit
    }
  }

  override fun onDetailDismissed() {
    closeDetail(opensSearch = false)
  }

  override fun onViewMovimientos() {
    closeDetail(opensSearch = true)
  }

  override fun onDetailHidden() {
    val detail = uiState.value.detail?.takeIf { it.isClosing } ?: return
    uiState.update { it.copy(detail = null) }
    detail.pendingSearch?.let(effectChannel::trySend)
  }

  override fun onAddMovimiento() {
    effectChannel.trySend(StatisticsEffect.OpenCreation)
  }

  override fun onRetry() {
    changeInputs { it.copy(reload = it.reload + 1) }
  }

  override fun onResume() {
    refreshDay()
    if (uiState.value.content is RetainedLoadState.Failed) onRetry()
  }

  private fun refreshDay() {
    val today = civilToday.date()
    changeInputs { it.copy(selection = it.selection.refresh(it.today, today), today = today) }
  }

  private fun openDetail(rootId: String) {
    if (uiState.value.detail != null || inputs.value.context == null) return
    uiState.update { it.copy(detail = CategoryDetailState(rootId)) }
    inputs.update { it.copy(detailRootId = rootId) }
  }

  private fun closeDetail(opensSearch: Boolean) {
    val detail = uiState.value.detail?.takeIf { !it.isClosing } ?: return
    val search = if (opensSearch) searchFor(StatisticsKey.Categoria(detail.rootId)) else null
    uiState.update { it.copy(detail = detail.copy(isClosing = true, pendingSearch = search)) }
    inputs.update { it.copy(detailRootId = null) }
  }

  private fun searchFor(key: StatisticsKey): StatisticsEffect.OpenSearch? =
      inputs.value.context?.let { StatisticsEffect.OpenSearch(key, it.moneda, it.request.period) }

  private fun changeInputs(transform: (StatisticsInputs) -> StatisticsInputs) {
    val previous = inputs.value
    inputs.update { current ->
      val next = transform(current)
      val contextChanged = next.context != current.context
      next.copy(
          retainedYears = current.retainedYears + next.selection.anchor.year,
          persistedYears = if (next.moneda != current.moneda) emptyList() else next.persistedYears,
          detailRootId = if (contextChanged) null else next.detailRootId,
      )
    }
    val current = inputs.value
    if (current == previous) return
    val contextChanged = previous.context != current.context
    val reloaded = previous.reload != current.reload
    uiState.update { state ->
      state.copy(
          selection = current.selection,
          today = current.today,
          years = current.availableYears,
          moneda = current.moneda,
          monedas = current.monedas.orEmpty(),
          trendSelection = if (contextChanged) null else state.trendSelection,
          content =
              if ((contextChanged && previous.context != null) || reloaded)
                  state.content.refreshing()
              else state.content,
          detail =
              state.detail?.let { detail ->
                when {
                  contextChanged && !detail.isClosing -> detail.copy(isClosing = true)
                  reloaded -> detail.copy(content = detail.content.refreshing())
                  else -> detail
                }
              },
      )
    }
  }

  private suspend fun observe() = coroutineScope {
    launch { observePreferences() }
    observeFrames(repositories, inputs, computationDispatcher, ::acceptCuentas).collect(
        ::acceptFrame
    )
  }

  private suspend fun observePreferences() {
    inputs
        .map { it.reload }
        .distinctUntilChanged()
        .flatMapLatest { reload -> repositories.preferences.preferences.map { reload to it } }
        .collect { (reload, outcome) -> acceptPreferences(reload, outcome) }
  }

  private fun acceptPreferences(reload: Long, outcome: DataOutcome<AppPreferencesState>) {
    if (reload != inputs.value.reload) return
    when (outcome) {
      is DataOutcome.Failure -> failContent(outcome.error)
      is DataOutcome.Success ->
          when (val state = outcome.data) {
            AppPreferencesState.Uninitialized ->
                failContent(DataError.InvalidInput("Preferences are uninitialized after app startup"))
            is AppPreferencesState.Ready ->
                changeInputs { it.copy(primary = state.value.monedaPrincipal) }
          }
    }
  }

  private fun failContent(error: DataError) {
    uiState.update { it.copy(content = it.content.failed(StatisticsFailure.Repository(error))) }
  }

  // A missing selected currency is dropped rather than kept for a later account in it.
  private fun acceptCuentas(monedas: List<Moneda>) {
    changeInputs { current ->
      current.copy(
          monedas = monedas,
          selectedMoneda = current.selectedMoneda?.takeIf { it in monedas },
      )
    }
  }

  private fun acceptFrame(frame: StatisticsFrame) {
    val current = inputs.value
    frame.years
        ?.takeIf { it.key == current.yearsKey }
        ?.value
        ?.let { years -> changeInputs { it.copy(persistedYears = years) } }
    uiState.update { state ->
      val main = frame.main?.takeIf { it.key == current.mainKey }
      val detail = state.detail
      val detailFrame = frame.detail?.takeIf { it.key == current.detailKey }
      state.copy(
          content = main?.state?.retaining(state.content.cachedValue) ?: state.content,
          detail =
              if (detail != null && detailFrame != null && detailFrame.key.rootId == detail.rootId)
                  detail.copy(content = detailFrame.state.retaining(detail.content.cachedValue))
              else detail,
      )
    }
  }

  override fun onCleared() {
    producer.cancel()
    savedStateHandle.clearSavedStateProvider(STATISTICS_SAVED_INPUT_KEY)
  }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** [primary] and [monedas] stay null until their sources first answer. */
private data class StatisticsInputs(
    val selection: StatisticsSelection,
    val today: LocalDate,
    val selectedMoneda: Moneda?,
    val retainedYears: Set<Int>,
    val primary: Moneda? = null,
    val monedas: List<Moneda>? = null,
    val persistedYears: List<Int> = emptyList(),
    val reload: Long = 0,
    val detailRootId: String? = null,
) {
  val moneda: Moneda?
    get() = primary?.let { viewedMoneda(it, selectedMoneda, monedas) }

  val context: StatisticsContext?
    get() = moneda?.let { StatisticsContext.of(selection, it, today) }

  val mainKey: MainKey?
    get() = context?.let { MainKey(it, reload) }

  val detailKey: DetailKey?
    get() = mainKey?.let { main -> detailRootId?.let { DetailKey(it, main) } }

  val yearsKey: YearsKey?
    get() = moneda?.let { YearsKey(it, reload) }

  val availableYears: List<Int>
    get() = selection.availableYears(persistedYears, today, retainedYears)
}

private data class MainKey(val context: StatisticsContext, val reload: Long)

private data class DetailKey(val rootId: String, val main: MainKey)

private data class YearsKey(val moneda: Moneda, val reload: Long)

private data class Keyed<K, T>(val key: K, val value: T)

private data class StatisticsCatalogLoad(
    val categorias: SourceLoad<Map<String, Categoria>>,
    val etiquetas: SourceLoad<Map<String, String>>,
    val cuentas: SourceLoad<Map<String, String>>,
) {
  val failure: DataError?
    get() = categorias.failure ?: etiquetas.failure ?: cuentas.failure

  val value: StatisticsCatalog?
    get() {
      val categorias = categorias.data ?: return null
      val etiquetas = etiquetas.data ?: return null
      val cuentas = cuentas.data ?: return null
      return StatisticsCatalog(categorias, etiquetas, cuentas)
    }
}

private data class StatisticsSources(
    val main: Keyed<MainKey, SourceLoad<StatisticsSnapshot>>?,
    val detail: Keyed<DetailKey, SourceLoad<CategoryStatisticsSnapshot>>?,
    val years: Keyed<YearsKey, SourceLoad<List<Int>>>?,
    val catalog: StatisticsCatalogLoad,
)

private data class LoadedFrame<K, T : Any>(
    val key: K,
    val state: RetainedLoadState<T, StatisticsFailure>,
)

private data class StatisticsFrame(
    val main: LoadedFrame<MainKey, StatisticsContent>?,
    val detail: LoadedFrame<DetailKey, CategoryDetailContent>?,
    val years: Keyed<YearsKey, List<Int>?>?,
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeFrames(
    repositories: StatisticsRepositories,
    inputs: StateFlow<StatisticsInputs>,
    dispatcher: CoroutineDispatcher,
    onMonedas: (List<Moneda>) -> Unit,
): Flow<StatisticsFrame> =
    combine(
            inputs
                .map { it.mainKey }
                .distinctUntilChanged()
                .flatMapLatest { key ->
                  if (key == null) flowOf(null)
                  else
                      repositories.statistics
                          .observe(key.context.request)
                          .toSourceLoad()
                          .map { Keyed(key, it) }
                },
            inputs
                .map { it.detailKey }
                .distinctUntilChanged()
                .flatMapLatest { key ->
                  if (key == null) flowOf(null)
                  else
                      repositories.statistics
                          .observeCategory(key.main.context.categoryRequest(key.rootId))
                          .toSourceLoad()
                          .map { Keyed(key, it) }
                },
            inputs
                .map { it.yearsKey }
                .distinctUntilChanged()
                .flatMapLatest { key ->
                  if (key == null) flowOf(null)
                  else
                      repositories.actividades
                          .observeYears(key.moneda)
                          .toSourceLoad()
                          .map { Keyed(key, it) }
                },
            inputs
                .map { it.reload }
                .distinctUntilChanged()
                .flatMapLatest { observeCatalog(repositories, onMonedas) },
        ) { main, detail, years, catalog ->
          StatisticsSources(main, detail, years, catalog)
        }
        .mapLatest { sources -> withContext(dispatcher) { sources.toFrame() } }

private fun observeCatalog(
    repositories: StatisticsRepositories,
    onMonedas: (List<Moneda>) -> Unit,
): Flow<StatisticsCatalogLoad> =
    combine(
        repositories.categorias.observeAll().toSourceLoad().map { load ->
          load.mapData { categorias -> categorias.associateBy { it.id } }
        },
        repositories.etiquetas.observeAll().toSourceLoad().map { load ->
          load.mapData { etiquetas -> etiquetas.associate { it.id to it.nombre } }
        },
        repositories.cuentas
            .observeAll()
            .toSourceLoad()
            .onEach { load -> load.data?.let { onMonedas(it.monedas()) } }
            .map { load -> load.mapData { cuentas -> cuentas.associate { it.id to it.nombre } } },
    ) { categorias, etiquetas, cuentas ->
      StatisticsCatalogLoad(categorias, etiquetas, cuentas)
    }

private fun StatisticsSources.toFrame(): StatisticsFrame {
  val catalogValue = catalog.value
  val mainFrame =
      main?.let { (key, load) ->
        LoadedFrame(
            key,
            present(load, catalog.failure, catalogValue) { snapshot, names ->
              presentStatistics(snapshot, key.context, names)
            },
        )
      }
  val mainContent = mainFrame?.state?.cachedValue?.takeIf { detail?.key?.main == mainFrame.key }
  val detailFrame =
      detail?.let { (key, load) ->
        LoadedFrame(
            key,
            present(load, catalog.failure, catalogValue) { snapshot, names ->
              presentCategoryDetail(snapshot, key.rootId, key.main.context, names, mainContent)
            },
        )
      }
  return StatisticsFrame(
      mainFrame,
      detailFrame,
      years?.let { (key, load) -> Keyed(key, load.data) },
  )
}

private inline fun <S, T : Any> present(
    load: SourceLoad<S>,
    catalogFailure: DataError?,
    catalog: StatisticsCatalog?,
    block: (S, StatisticsCatalog) -> T,
): RetainedLoadState<T, StatisticsFailure> {
  val failure = load.failure ?: catalogFailure
  val data = load.data
  return when {
    failure != null -> RetainedLoadState.Failed(StatisticsFailure.Repository(failure))
    data == null || catalog == null -> RetainedLoadState.Loading()
    else ->
        try {
          RetainedLoadState.Ready(block(data, catalog))
        } catch (_: ArithmeticException) {
          RetainedLoadState.Failed(StatisticsFailure.CalculationOverflow)
        }
  }
}

private fun <T : Any, E : Any> RetainedLoadState<T, E>.retaining(
    previous: T?
): RetainedLoadState<T, E> =
    when (this) {
      is RetainedLoadState.Loading -> copy(previous = previous)
      is RetainedLoadState.Failed -> copy(previous = previous)
      is RetainedLoadState.Ready -> this
    }

private sealed interface SourceLoad<out T> {
  data object Loading : SourceLoad<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : SourceLoad<T>
}

private fun <T> Flow<DataOutcome<T>>.toSourceLoad(): Flow<SourceLoad<T>> =
    map<DataOutcome<T>, SourceLoad<T>> { SourceLoad.Value(it) }.onStart { emit(SourceLoad.Loading) }

private fun <T, R> SourceLoad<T>.mapData(transform: (T) -> R): SourceLoad<R> =
    when (this) {
      SourceLoad.Loading -> SourceLoad.Loading
      is SourceLoad.Value -> SourceLoad.Value(outcome.map(transform))
    }

private val <T> SourceLoad<T>.data: T?
  get() =
      when (this) {
        SourceLoad.Loading -> null
        is SourceLoad.Value -> outcome.fold(onSuccess = { it }, onFailure = { null })
      }

private val <T> SourceLoad<T>.failure: DataError?
  get() =
      when (this) {
        SourceLoad.Loading -> null
        is SourceLoad.Value -> outcome.fold(onSuccess = { null }, onFailure = { it })
      }
