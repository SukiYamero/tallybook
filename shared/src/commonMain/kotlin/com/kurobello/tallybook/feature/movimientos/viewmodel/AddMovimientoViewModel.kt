package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
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
  private val preferredLocale = MutableStateFlow<AppLocale?>(null)
  private val initialDraft =
      MovimientoDraft(
          tipo = TipoMovimiento.GASTO,
          amountInput = "",
          fecha = captureToday(),
          selectedCategoriaId = null,
          descripcion = "",
      )
  private var suggestionIndex = entryEngine.dictionaryIndex(clock.now())
  private var suggestionQuery: SuggestionQuery? = null
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
                selectedCategoriaId = initialDraft.selectedCategoriaId,
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
    updateFields {
      if (categorias.none { it.id == id }) this
      else copy(selectedCategoriaId = id, dismissedSuggestionIds = dismissedAfterSelecting(id))
    }
  }

  override fun onDescripcionChanged(value: String) {
    updateFields { copy(descripcion = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) }
  }

  override fun onPhraseChanged(value: String) {
    val phrase = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)
    updateFields { copy(phrase = phrase, preview = preview.takeIf { phrase.isNotBlank() }) }
    refreshPreview(debounce = true)
  }

  override fun onPhraseContinue() {
    if (!uiState.value.canContinuePhrase) return
    previewJob?.cancel()
    val today = captureToday()
    val text = uiState.value.phrase.trim()
    var state = uiState.value
    var interpretation = interpret(state, text, today)
    // Dismissals belong to the subject they were made for; a different sentence starts clean.
    if (
        state.dismissedSuggestionIds.isNotEmpty() &&
            interpretation.query.subject != suggestionQuery?.subject
    ) {
      state = state.copy(dismissedSuggestionIds = emptySet())
      interpretation = interpret(state, text, today)
    }
    suggestionQuery = interpretation.query
    updateFields {
      val parsed = interpretation.parsed
      copy(
          step = AddMovimientoStep.SUMMARY,
          dismissedSuggestionIds = state.dismissedSuggestionIds,
          tipo = interpretation.tipo,
          amountInput = parsed.monto?.toAmountInput().orEmpty(),
          fecha = parsed.fecha ?: today,
          descripcion = text,
          selectedCategoriaId = interpretation.preselectedCategoriaId,
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
    suggestionQuery = null
    updateFields { withInitialEntry().copy(step = AddMovimientoStep.FORM) }
  }

  override fun onEditMovimiento() {
    updateFields { copy(step = AddMovimientoStep.FORM, formOpenedFromSummary = true) }
  }

  override fun onSuggestionSelected(id: String) {
    updateFields { if (suggestions.any { it.id == id }) copy(selectedCategoriaId = id) else this }
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
    suggestionQuery = null
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
          selectedCategoriaId = initialDraft.selectedCategoriaId,
          descripcion = initialDraft.descripcion,
          submitAttempted = false,
          phrase = "",
          preview = null,
          dismissedSuggestionIds = emptySet(),
          amountNeedsReview = false,
          today = initialDraft.fecha,
          formOpenedFromSummary = false,
      )

  private fun AddMovimientoUiState.withDirtyState(): AddMovimientoUiState =
      copy(hasUnsavedChanges = phrase.isNotBlank() || toDraft() != initialDraft)

  private fun AddMovimientoUiState.withSuggestions(): AddMovimientoUiState =
      copy(
          suggestions =
              rankSuggestions()
                  .filterNot { it.categoriaId == selectedCategoriaId }
                  .mapNotNull { suggestion ->
                    categorias.firstOrNull { it.id == suggestion.categoriaId }
                  }
                  .take(MAX_SUGGESTION_CHIPS)
      )

  private fun AddMovimientoUiState.rankSuggestions() =
      suggestionQuery?.let { entryEngine.rank(suggestionIndex, it, candidates()) }.orEmpty()

  // A suggestion that was selected and then replaced from the form is dismissed for this draft.
  private fun AddMovimientoUiState.dismissedAfterSelecting(newId: String): Set<String> {
    val replaced = selectedCategoriaId?.takeIf { previous ->
      previous != newId && rankSuggestions().any { it.categoriaId == previous }
    }
    return dismissedSuggestionIds + setOfNotNull(replaced)
  }

  private fun AddMovimientoUiState.candidates() =
      SuggestionCandidates(
          availableIds = categorias.mapTo(mutableSetOf()) { it.id } - CATEGORIA_OTROS_ID,
          excludedIds = dismissedSuggestionIds,
      )

  private fun AddMovimientoUiState.phraseContext(today: LocalDate) =
      PhraseContext(moneda, entryEngine.resolveLocale(preferredLocale.value), today)

  private fun interpret(state: AddMovimientoUiState, text: String, today: LocalDate) =
      entryEngine.interpret(text, state.phraseContext(today), suggestionIndex, state.candidates())

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
          )
      uiState.update { current ->
        if (current.step == AddMovimientoStep.PHRASE && current.phrase.trim() == text) {
          current.copy(preview = interpretation.toPreview(current.categorias))
        } else {
          current
        }
      }
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

private class HistoryInputs(
    val history: List<CategoryHistoryRow>,
    val moneda: Moneda,
    val locale: AppLocale?,
)

internal data class AddMovimientoRepositories(
    val preferences: AppPreferencesRepository,
    val categorias: CategoriaRepository,
    val movimientos: MovimientoRepository,
)

private fun PhraseInterpretation.toPreview(categorias: List<Categoria>): PhrasePreviewUi? {
  val categoria = preselectedCategoriaId?.let { id -> categorias.firstOrNull { it.id == id } }
  val preview =
      PhrasePreviewUi(
          tipo = parsed.tipo ?: tipo.takeIf { it == TipoMovimiento.INGRESO },
          monto = parsed.monto,
          fecha = parsed.fecha,
          categoria = categoria,
      )
  return preview.takeUnless { it.monto == null && it.fecha == null && it.categoria == null }
}

private data class MovimientoCandidate(
    val amount: Money?,
    val categoriaId: String?,
    val isFechaValid: Boolean,
    val isReadyToSave: Boolean,
) {
  fun toMovimientoOrNull(
      idGenerator: IdGenerator,
      tipo: TipoMovimiento,
      fecha: LocalDate,
      descripcion: String?,
      createdAt: kotlin.time.Instant,
  ): Movimiento? =
      takeIf { amount != null && categoriaId != null && isFechaValid && isReadyToSave }
          ?.let {
            Movimiento(
                id = idGenerator.next(),
                tipo = tipo,
                monto = requireNotNull(amount),
                fecha = fecha,
                categoriaId = requireNotNull(categoriaId),
                descripcion = descripcion,
                createdAt = createdAt,
            )
          }
}

private fun AddMovimientoUiState.toMovimientoOrNull(
    idGenerator: IdGenerator,
    createdAt: kotlin.time.Instant,
    timeZone: kotlinx.datetime.TimeZone,
): Movimiento? =
    MovimientoCandidate(
            amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money,
            categoriaId = effectiveCategoriaId,
            isFechaValid = fecha <= createdAt.toLocalDateTime(timeZone).date,
            isReadyToSave = !isLoading && error == null,
        )
        .toMovimientoOrNull(
            idGenerator = idGenerator,
            tipo = tipo,
            fecha = fecha,
            descripcion = descripcion.normalizeDescription(),
            createdAt = createdAt,
        )

private sealed interface LoadState<out T> {
  data object Loading : LoadState<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : LoadState<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun AddMovimientoRepositories.preferenceLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<AppPreferencesState>> = retryRequests.flatMapLatest {
  preferences.preferences
      .map<DataOutcome<AppPreferencesState>, LoadState<AppPreferencesState>> { LoadState.Value(it) }
      .onStart { emit(LoadState.Loading) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun AddMovimientoRepositories.categoriaLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<List<Categoria>>> = retryRequests.flatMapLatest {
  categorias
      .observeAll()
      .map<DataOutcome<List<Categoria>>, LoadState<List<Categoria>>> { LoadState.Value(it) }
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

internal fun String.normalizeDescription(): String? =
    trim().replace(Regex("\\s+"), " ").ifEmpty { null }
