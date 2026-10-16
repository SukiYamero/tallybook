package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.normalizeOptionalText
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val HISTORY_LIMIT = 1_000
private const val PREVIEW_DEBOUNCE_MILLIS = 250L
private val LINE_BREAKS = Regex("[\r\n]+")

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class AddMovimientoViewModel(
    private val repositories: AddMovimientoRepositories,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    private val entryEngine: MovimientoEntryEngine,
    scope: CoroutineScope? = null,
) : ViewModel(), AddMovimientoActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryLoadRequests = MutableStateFlow(0)
  private val retryEtiquetaRequests = MutableStateFlow(0)
  private val preferredLocale = MutableStateFlow<AppLocale?>(null)
  private val initialDraft =
      MovimientoDraft(
          tipo = TipoMovimiento.GASTO,
          amountInput = "",
          fecha = captureToday(),
          categoriaId = null,
          etiquetaIds = emptyList(),
          titulo = "",
          descripcion = "",
      )
  private var suggestionIndex = entryEngine.dictionaryIndex(clock.now())
  private var previewJob: Job? = null

  val uiState: StateFlow<AddMovimientoUiState>
    field =
        MutableStateFlow(
            AddMovimientoUiState(
                tipo = initialDraft.tipo,
                amountInput = initialDraft.amountInput,
                fecha = initialDraft.fecha,
                moneda = Moneda.COP,
                categorias = emptyList(),
                categoriaId = initialDraft.categoriaId,
                titulo = initialDraft.titulo,
                descripcion = initialDraft.descripcion,
                isLoading = true,
                isSubmitting = false,
                submitAttempted = false,
                error = null,
            )
        )

  private val effectChannel = Channel<AddMovimientoEffect>(Channel.BUFFERED)
  val effects: Flow<AddMovimientoEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      combine(
              repositories.preferenceLoadStates(retryLoadRequests),
              repositories.categoriaLoadStates(retryLoadRequests),
          ) { preferences, categorias ->
            LoadSnapshot.from(preferences, categorias)
          }
          .collect { snapshot ->
            preferredLocale.value = snapshot.appLocale
            uiState.update { current ->
              current
                  .copy(
                      moneda = snapshot.moneda ?: current.moneda,
                      categorias = snapshot.categorias ?: current.categorias,
                      isLoading = snapshot.isLoading,
                      error =
                          snapshot.error
                              ?: current.error?.takeIf { it == MovimientoFormError.SAVE_FAILED },
                  )
                  .withSuggestions()
            }
            refreshPreview(debounce = false)
          }
    }
    coroutineScope.launch { observeSuggestionIndex() }
    coroutineScope.launch { observeEtiquetas() }
  }

  override fun onTipoChanged(tipo: TipoMovimiento) {
    updateFields { copy(tipo = tipo) }
  }

  override fun onAmountChanged(value: String) {
    updateFields { copy(amountInput = value, amountNeedsReview = false) }
  }

  override fun onFechaChanged(value: LocalDate) {
    updateFields { copy(fecha = value) }
  }

  override fun onCategoriaSelected(id: String) {
    updateFields { if (isSelectableCategoria(id, categorias)) withCategoria(id) else this }
  }

  override fun onCategoriaCleared() {
    updateFields { categoriaId?.let { withCategoria(null, removed = it) } ?: this }
  }

  override fun onEtiquetasChanged(ids: List<String>) {
    updateFields {
      if (etiquetasLoad == EtiquetasLoad.READY) withEtiquetas(ids.toEtiquetaSelection()) else this
    }
  }

  override fun onEtiquetaRemoved(id: String) {
    updateFields { if (id in etiquetaIds) withEtiquetas(etiquetaIds - id) else this }
  }

  override fun onCreateEtiqueta(nombre: String) {
    uiState.update { it.copy(failedEtiquetaNombre = null) }
    coroutineScope.launch {
      when (val outcome = repositories.etiquetas.create(nombre)) {
        is DataOutcome.Success ->
            updateFields { withEtiquetas(etiquetaIds.withCreated(outcome.data)) }
        is DataOutcome.Failure -> uiState.update { it.copy(failedEtiquetaNombre = nombre) }
      }
    }
  }

  override fun onRetryEtiquetas() {
    retryEtiquetaRequests.update { it + 1 }
  }

  override fun onTituloChanged(value: String) {
    updateFields { copy(titulo = value.take(MOVIMIENTO_TITLE_MAX_LENGTH)) }
  }

  override fun onDescripcionChanged(value: String) {
    updateFields { copy(descripcion = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) }
  }

  override fun onPhraseChanged(value: String) {
    val phrase = value.replace(LINE_BREAKS, " ").take(MOVIMIENTO_TITLE_MAX_LENGTH)
    updateFields { if (phrase.isBlank()) withoutInterpretation(phrase) else copy(phrase = phrase) }
    refreshPreview(debounce = true)
  }

  override fun onPhraseContinue() {
    if (!uiState.value.canContinuePhrase) return
    previewJob?.cancel()
    val today = captureToday()
    val text = uiState.value.phrase.trim()
    updateFields {
      val (base, interpretation) = interpreting(text, today)
      val parsed = interpretation.parsed
      base
          .withDetection(interpretation, today)
          .copy(
              step = AddMovimientoStep.SUMMARY,
              tipo = interpretation.tipo,
              amountInput = parsed.monto?.toAmountInput().orEmpty(),
              fecha = parsed.fecha ?: today,
              titulo = text,
              amountNeedsReview =
                  parsed.monto != null && PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags,
              today = today,
              submitAttempted = false,
              formOpenedFromSummary = false,
          )
    }
  }

  override fun onFillManually() {
    previewJob?.cancel()
    updateFields {
      withInitialEntry()
          .copy(
              step = AddMovimientoStep.FORM,
              categoriaId = manualCategoriaId,
              categoriasEdited = categoriasEdited,
              etiquetaIds = manualEtiquetaIds,
              etiquetasEdited = etiquetasEdited,
          )
    }
  }

  override fun onEditMovimiento() {
    updateFields { copy(step = AddMovimientoStep.FORM, formOpenedFromSummary = true) }
  }

  override fun onSuggestionSelected(id: String) {
    updateFields { if (suggestions.none { it.id == id }) this else withCategoria(id) }
  }

  override fun onStepBack() {
    uiState.update { current ->
      val target = current.stepBackTarget
      if (target == null || current.isSubmitting) {
        current
      } else {
        current.copy(
            step = target,
            submitAttempted = current.submitAttempted && current.step != AddMovimientoStep.FORM,
        )
      }
    }
  }

  override fun onDiscardChanges() {
    if (uiState.value.isSubmitting) return
    previewJob?.cancel()
    uiState.update { current ->
      current
          .withInitialEntry()
          .copy(
              error = current.error?.takeUnless { it == MovimientoFormError.SAVE_FAILED },
              hasUnsavedChanges = false,
              step = AddMovimientoStep.PHRASE,
              suggestions = emptyList(),
          )
    }
  }

  override fun onRetryLoad() {
    retryLoadRequests.update { it + 1 }
  }

  override fun onSubmit() {
    if (uiState.value.isSubmitting) return

    uiState.update {
      it.copy(
          submitAttempted = true,
          error = it.error?.takeUnless { error -> error == MovimientoFormError.SAVE_FAILED },
      )
    }
    val timeZone = timeZoneProvider.current()
    val createdAt = clock.now()
    val movement = uiState.value.toMovimientoOrNull(idGenerator, createdAt, timeZone)
    if (movement == null) {
      uiState.update { current ->
        if (current.step == AddMovimientoStep.SUMMARY) {
          current.copy(step = AddMovimientoStep.FORM, formOpenedFromSummary = true)
        } else {
          current
        }
      }
      return
    }
    uiState.update { it.copy(isSubmitting = true) }
    coroutineScope.launch {
      var saved = false
      try {
        when (repositories.movimientos.insert(movement)) {
          is DataOutcome.Success -> {
            effectChannel.send(AddMovimientoEffect.Saved)
            saved = true
          }
          is DataOutcome.Failure ->
              uiState.update { it.copy(error = MovimientoFormError.SAVE_FAILED) }
        }
      } finally {
        if (!saved) {
          uiState.update { it.copy(isSubmitting = false) }
        }
      }
    }
  }

  private fun captureToday(): LocalDate = clock.todayIn(timeZoneProvider.current())

  private fun updateFields(transform: AddMovimientoUiState.() -> AddMovimientoUiState) {
    uiState.update { current ->
      transform(current)
          .copy(error = current.error?.takeUnless { it == MovimientoFormError.SAVE_FAILED })
          .withDirtyState()
          .withSuggestions()
    }
  }

  private fun AddMovimientoUiState.withInitialEntry(): AddMovimientoUiState =
      copy(
          tipo = initialDraft.tipo,
          amountInput = initialDraft.amountInput,
          fecha = initialDraft.fecha,
          categoriaId = initialDraft.categoriaId,
          etiquetaIds = initialDraft.etiquetaIds,
          titulo = initialDraft.titulo,
          descripcion = initialDraft.descripcion,
          submitAttempted = false,
          phrase = "",
          preview = null,
          suggestionQuery = null,
          dismissedSuggestionIds = emptySet(),
          categoriasEdited = false,
          etiquetasEdited = false,
          amountNeedsReview = false,
          today = initialDraft.fecha,
          formOpenedFromSummary = false,
      )

  // A category or tags picked in the sentence dialog do not guard it: only a typed sentence is
  // worth confirming.
  private fun AddMovimientoUiState.withDirtyState(): AddMovimientoUiState {
    val draft = toDraft()
    val comparable =
        if (step == AddMovimientoStep.PHRASE)
            draft.copy(
                categoriaId = initialDraft.categoriaId,
                etiquetaIds = initialDraft.etiquetaIds,
            )
        else draft
    return copy(hasUnsavedChanges = phrase.isNotBlank() || comparable != initialDraft)
  }

  private fun AddMovimientoUiState.withSuggestions(): AddMovimientoUiState =
      copy(
          suggestions =
              rankSuggestions()
                  .filterNot { it.categoriaId == categoriaId }
                  .mapNotNull { suggestion ->
                    categorias.firstOrNull { it.id == suggestion.categoriaId }
                  }
                  .take(MAX_SUGGESTION_CHIPS)
      )

  private fun AddMovimientoUiState.rankSuggestions() =
      suggestionQuery?.let { entryEngine.rank(suggestionIndex, it, candidates()) }.orEmpty()

  // A removed category that the sentence suggested stays dismissed for the same subject.
  private fun AddMovimientoUiState.withCategoria(
      updated: String?,
      removed: String? = null,
  ): AddMovimientoUiState {
    val dismissed = removed?.takeIf { id -> rankSuggestions().any { it.categoriaId == id } }
    return copy(
        categoriaId = updated,
        categoriasEdited = true,
        dismissedSuggestionIds = dismissedSuggestionIds + listOfNotNull(dismissed),
    )
  }

  private fun AddMovimientoUiState.withEtiquetas(ids: List<String>): AddMovimientoUiState =
      copy(etiquetaIds = ids, etiquetasEdited = true)

  // A category and tags picked by hand are the only draft values a blank sentence leaves behind.
  private fun AddMovimientoUiState.withoutInterpretation(phrase: String): AddMovimientoUiState =
      copy(
          phrase = phrase,
          preview = null,
          suggestionQuery = null,
          tipo = initialDraft.tipo,
          amountInput = initialDraft.amountInput,
          fecha = initialDraft.fecha,
          titulo = initialDraft.titulo,
          descripcion = initialDraft.descripcion,
          amountNeedsReview = false,
          categoriaId = manualCategoriaId,
          etiquetaIds = manualEtiquetaIds,
      )

  private val AddMovimientoUiState.manualCategoriaId: String?
    get() = if (categoriasEdited) categoriaId else initialDraft.categoriaId

  private val AddMovimientoUiState.manualEtiquetaIds: List<String>
    get() = if (etiquetasEdited) etiquetaIds else initialDraft.etiquetaIds

  private fun AddMovimientoUiState.withDetection(
      interpretation: PhraseInterpretation,
      today: LocalDate,
  ): AddMovimientoUiState =
      copy(
          preview =
              PhrasePreviewUi(
                  tipo = interpretation.tipo,
                  monto = interpretation.parsed.monto,
                  fecha = interpretation.parsed.fecha ?: today,
              ),
          suggestionQuery = interpretation.query,
          categoriaId =
              if (categoriasEdited) categoriaId else interpretation.preselectedCategoriaId,
          etiquetaIds = if (etiquetasEdited) etiquetaIds else interpretation.etiquetaIds,
      )

  // Dismissals belong to the subject they were made for; a different sentence starts clean.
  private fun AddMovimientoUiState.interpreting(
      text: String,
      today: LocalDate,
      computed: PhraseInterpretation = interpret(this, text, today),
  ): Interpreted {
    val keepsDismissals =
        dismissedSuggestionIds.isEmpty() || computed.query.subject == suggestionQuery?.subject
    if (keepsDismissals) return Interpreted(this, computed)
    val clean = copy(dismissedSuggestionIds = emptySet())
    return Interpreted(clean, interpret(clean, text, today))
  }

  private fun AddMovimientoUiState.candidates() =
      SuggestionCandidates(
          availableIds =
              categorias.filterNot(Categoria::oculta).mapTo(mutableSetOf(), Categoria::id) -
                  CATEGORIA_OTROS_ID,
          excludedIds = dismissedSuggestionIds,
      )

  private fun AddMovimientoUiState.phraseContext(today: LocalDate) =
      PhraseContext(moneda, entryEngine.resolveLocale(preferredLocale.value), today)

  private fun interpret(state: AddMovimientoUiState, text: String, today: LocalDate) =
      entryEngine.interpret(
          text,
          state.phraseContext(today),
          suggestionIndex,
          state.candidates(),
          state.etiquetas,
      )

  private fun refreshPreview(debounce: Boolean) {
    previewJob?.cancel()
    val text = uiState.value.phrase.trim()
    if (uiState.value.step != AddMovimientoStep.PHRASE || text.isEmpty()) return
    previewJob = coroutineScope.launch {
      if (debounce) delay(PREVIEW_DEBOUNCE_MILLIS)
      val snapshot = uiState.value
      val interpretation =
          entryEngine.interpretOffMain(
              text,
              snapshot.phraseContext(snapshot.today),
              suggestionIndex,
              snapshot.candidates(),
              snapshot.etiquetas,
          )
      uiState.update { current ->
        if (current.step == AddMovimientoStep.PHRASE && current.phrase.trim() == text) {
          val (base, interpreted) = current.interpreting(text, current.today, interpretation)
          base.withDetection(interpreted, current.today).withSuggestions()
        } else {
          current
        }
      }
    }
  }

  private suspend fun observeEtiquetas() {
    retryEtiquetaRequests
        .loadStates { repositories.etiquetas.observeAll() }
        .collect { state ->
          uiState.update { current ->
            when (state) {
              LoadState.Loading -> current.copy(etiquetasLoad = EtiquetasLoad.LOADING)
              is LoadState.Value ->
                  when (val outcome = state.outcome) {
                    is DataOutcome.Success ->
                        current.copy(etiquetas = outcome.data, etiquetasLoad = EtiquetasLoad.READY)
                    is DataOutcome.Failure ->
                        current.copy(etiquetasLoad = EtiquetasLoad.UNAVAILABLE)
                  }
            }
          }
          refreshPreview(debounce = false)
        }
  }

  private suspend fun observeSuggestionIndex() {
    combine(
            repositories.movimientos
                .observeCategoryHistory(HISTORY_LIMIT)
                .map { outcome -> (outcome as? DataOutcome.Success)?.data.orEmpty() }
                .catch { emit(emptyList()) },
            uiState.map { it.moneda }.distinctUntilChanged(),
            preferredLocale,
        ) { history, moneda, locale ->
          HistoryInputs(history, moneda, locale)
        }
        .mapLatest { inputs ->
          val context =
              PhraseContext(
                  inputs.moneda,
                  entryEngine.resolveLocale(inputs.locale),
                  captureToday(),
              )
          entryEngine.buildIndex(inputs.history, context, clock.now())
        }
        .collect { index ->
          suggestionIndex = index
          uiState.update { it.withSuggestions() }
          refreshPreview(debounce = false)
        }
  }
}

private data class Interpreted(
    val state: AddMovimientoUiState,
    val interpretation: PhraseInterpretation,
)

private class HistoryInputs(
    val history: List<CategoryHistoryRow>,
    val moneda: Moneda,
    val locale: AppLocale?,
)

internal data class AddMovimientoRepositories(
    val preferences: AppPreferencesRepository,
    val categorias: CategoriaRepository,
    val movimientos: MovimientoRepository,
    val etiquetas: EtiquetaRepository,
)

private fun AddMovimientoUiState.toMovimientoOrNull(
    idGenerator: IdGenerator,
    createdAt: kotlin.time.Instant,
    timeZone: kotlinx.datetime.TimeZone,
): Movimiento? {
  val amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money
  val categoriaId = effectiveCategoriaId
  val isValid = fecha <= createdAt.toLocalDateTime(timeZone).date && !isLoading && error == null
  return if (amount == null || categoriaId == null || !isValid) null
  else
      Movimiento(
          id = idGenerator.next(),
          tipo = tipo,
          monto = amount,
          fecha = fecha,
          categoriaId = categoriaId,
          titulo = titulo.normalizeOptionalText(),
          descripcion = descripcion.normalizeOptionalText(),
          createdAt = createdAt,
          etiquetaIds = selectedEtiquetas.map(Etiqueta::id),
      )
}

private sealed interface LoadState<out T> {
  data object Loading : LoadState<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : LoadState<T>
}

private fun AddMovimientoRepositories.preferenceLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<AppPreferencesState>> = retryRequests.loadStates { preferences.preferences }

private fun AddMovimientoRepositories.categoriaLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<List<Categoria>>> = retryRequests.loadStates { categorias.observeAll() }

/** Restarts [source] on every retry request, reporting Loading until it emits. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> Flow<Int>.loadStates(source: () -> Flow<DataOutcome<T>>): Flow<LoadState<T>> =
    flatMapLatest {
      source()
          .map<DataOutcome<T>, LoadState<T>> { LoadState.Value(it) }
          .onStart { emit(LoadState.Loading) }
    }

private data class LoadSnapshot(
    val moneda: Moneda?,
    val appLocale: AppLocale?,
    val categorias: List<Categoria>?,
    val isLoading: Boolean,
    val error: MovimientoFormError?,
) {
  companion object {
    fun from(
        preferences: LoadState<AppPreferencesState>,
        categorias: LoadState<List<Categoria>>,
    ): LoadSnapshot {
      val loaded =
          (preferences as? LoadState.Value)
              ?.outcome
              .let { it as? DataOutcome.Success }
              ?.data
              ?.let { it as? AppPreferencesState.Ready }
              ?.value
      return LoadSnapshot(
          moneda = loaded?.monedaPrincipal,
          appLocale = loaded?.appLocale,
          categorias =
              (categorias as? LoadState.Value)?.outcome.let { it as? DataOutcome.Success }?.data,
          isLoading = preferences is LoadState.Loading || categorias is LoadState.Loading,
          error =
              when {
                (preferences as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                    MovimientoFormError.PREFERENCES_UNAVAILABLE
                (categorias as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                    MovimientoFormError.CATEGORIES_UNAVAILABLE
                else -> null
              },
      )
    }
  }
}
