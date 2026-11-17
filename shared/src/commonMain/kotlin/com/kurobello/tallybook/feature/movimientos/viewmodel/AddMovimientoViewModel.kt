package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.RecurrenciaRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.data.voice.VoiceDictation
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
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
import kotlinx.coroutines.flow.flowOf
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
  private val voice =
      AddMovimientoVoice(
          MovimientoDictation(repositories.voice),
          coroutineScope,
          VoiceHost(),
      )

  val uiState: StateFlow<AddMovimientoUiState>
    field =
        MutableStateFlow(
            AddMovimientoUiState(
                tipo = initialDraft.tipo,
                amountInput = initialDraft.amountInput,
                fecha = initialDraft.fecha,
                categorias = emptyList(),
                categoriaId = initialDraft.categoriaId,
                titulo = initialDraft.titulo,
                descripcion = initialDraft.descripcion,
                isLoading = true,
                isSubmitting = false,
                submitAttempted = false,
                error = null,
                voice = VoiceInputUi(isAvailable = voice.isAvailable),
            )
        )

  private val effectChannel = Channel<AddMovimientoEffect>(Channel.BUFFERED)
  val effects: Flow<AddMovimientoEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      combine(
              repositories.preferenceLoadStates(retryLoadRequests).flatMapLatest { preferences ->
                repositories.cuentaLoadStates(preferences).map { cuentas -> preferences to cuentas }
              },
              repositories.categoriaLoadStates(retryLoadRequests),
          ) { (preferences, cuentas), categorias ->
            LoadSnapshot.from(preferences, cuentas, categorias)
          }
          .collect { snapshot ->
            preferredLocale.value = snapshot.appLocale
            uiState.update { current ->
              current
                  .withCatalog(snapshot)
                  .copy(
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
    updateFields { copy(tipo = tipo, cuentaChoice = cuentaChoice.forTipo(tipo)) }
  }

  override fun onAmountChanged(value: String) {
    updateFields { copy(amountInput = value, amountNeedsReview = false, amountEdited = true) }
  }

  override fun onMedioSelected(medio: MedioPago) {
    updateCuentaChoice {
      if (medio.allows(selectionTipo)) shownChoice.withMedio(medio, cuentas, cuentaDefaults)
      else null
    }
  }

  override fun onCuentaSelected(id: String) {
    updateCuentaChoice {
      cuentas
          .firstOrNull { it.id == id && !it.archivada }
          ?.let { shownChoice.withCuenta(it, selectionTipo) }
    }
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

  // Typing while listening takes the sentence back from the recognizer.
  override fun onPhraseChanged(value: String) {
    voice.cancel()
    updateVoice { copy(notice = null) }
    applyPhrase(value)
  }

  private fun applyPhrase(value: String) {
    val phrase = value.replace(LINE_BREAKS, " ").take(MOVIMIENTO_TITLE_MAX_LENGTH)
    updateFields { if (phrase.isBlank()) withoutInterpretation(phrase) else copy(phrase = phrase) }
    refreshPreview(debounce = true)
  }

  override fun onVoiceStart() = voice.start()

  override fun onVoiceStop() = voice.finish()

  override fun onVoiceCancel() = voice.cancel()

  override fun onVoicePermissionResult(granted: Boolean) = voice.onPermissionResult(granted)

  override fun onVoiceOpenSettings() {
    effectChannel.trySend(AddMovimientoEffect.OpenDictationSettings)
  }

  private fun updateVoice(transform: VoiceInputUi.() -> VoiceInputUi) {
    uiState.update { it.copy(voice = it.voice.transform()) }
  }

  override fun onPhraseContinue() {
    if (!uiState.value.canContinuePhrase) return
    voice.cancel()
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
              amountEdited = false,
              fecha = parsed.fecha ?: today,
              titulo = text,
              amountNeedsReview =
                  parsed.monto != null && PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags,
              frecuencia = if (frecuenciaEdited) frecuencia else parsed.frecuencia,
              recurrenceUnsupported =
                  !frecuenciaEdited &&
                      PhraseReviewFlag.UNSUPPORTED_RECURRENCE in parsed.reviewFlags,
              today = today,
              submitAttempted = false,
              formOpenedFromSummary = false,
          )
    }
  }

  override fun onFillManually() {
    voice.cancel()
    previewJob?.cancel()
    updateFields {
      withInitialEntry()
          .copy(
              step = AddMovimientoStep.FORM,
              categoriaId = manualCategoriaId,
              categoriasEdited = categoriasEdited,
              etiquetaIds = manualEtiquetaIds,
              etiquetasEdited = etiquetasEdited,
              cuentaChoice =
                  cuentaChoice
                      .withoutSentence(cuentaDefaults, initialDraft.tipo)
                      .forTipo(initialDraft.tipo),
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

  override fun onRepetirChanged(enabled: Boolean) {
    updateFields {
      copy(
          frecuencia = if (enabled) frecuencia ?: Frecuencia.MENSUAL else null,
          frecuenciaEdited = true,
          recurrenceUnsupported = false,
      )
    }
  }

  override fun onFrecuenciaChanged(frecuencia: Frecuencia) {
    updateFields {
      if (this.frecuencia == null) this
      else copy(frecuencia = frecuencia, frecuenciaEdited = true, recurrenceUnsupported = false)
    }
  }

  override fun onDiscardChanges() {
    if (uiState.value.isSubmitting) return
    voice.cancel()
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
    val frecuencia = uiState.value.frecuencia
    val today = createdAt.toLocalDateTime(timeZone).date
    coroutineScope.launch {
      var saved = false
      try {
        val written =
            if (frecuencia == null) repositories.movimientos.insert(movement)
            else repositories.recurrencias.create(movement, frecuencia, today).map {}
        when (val outcome = written) {
          is DataOutcome.Success -> {
            effectChannel.send(AddMovimientoEffect.Saved)
            saved = true
          }
          is DataOutcome.Failure -> {
            // Every refusal is invalid input; re-reading the cuenta tells a refused one apart.
            val rejected =
                outcome.error is DataError.InvalidInput &&
                    (repositories.cuentas.getById(movement.cuentaId) as? DataOutcome.Success)
                        ?.data
                        ?.accepts(movement.medio, movement.monto.moneda) != true
            uiState.update { current ->
              if (rejected) current.withRejectedCuenta()
              else current.copy(error = MovimientoFormError.SAVE_FAILED)
            }
          }
        }
      } finally {
        if (!saved) {
          uiState.update { it.copy(isSubmitting = false) }
        }
      }
    }
  }

  private fun captureToday(): LocalDate = clock.todayIn(timeZoneProvider.current())

  private fun AddMovimientoUiState.withRejectedCuenta(): AddMovimientoUiState =
      copy(
          cuentaRejected = true,
          step = if (step == AddMovimientoStep.SUMMARY) AddMovimientoStep.FORM else step,
          formOpenedFromSummary = formOpenedFromSummary || step == AddMovimientoStep.SUMMARY,
      )

  /** The choice as the user sees it, with the only compatible cuenta standing for an empty one. */
  private val AddMovimientoUiState.shownChoice: CuentaSelectionState
    get() = cuentaChoice.copy(cuentaId = cuentaField.selected?.id)

  private fun updateCuentaChoice(choose: AddMovimientoUiState.() -> CuentaSelectionState?) {
    updateFields {
      val choice = choose() ?: return@updateFields this
      val previous = moneda
      copy(cuentaChoice = choice, cuentaRejected = false).withRederivedAmount(previous)
    }
    if (uiState.value.step == AddMovimientoStep.PHRASE) refreshPreview(debounce = false)
  }

  private fun AddMovimientoUiState.withRederivedAmount(previous: Moneda): AddMovimientoUiState {
    val text = phrase.trim()
    val ownsAmount = step != AddMovimientoStep.PHRASE && !amountEdited
    if (moneda == previous || !ownsAmount || text.isEmpty())
        return copy(amountInput = amountInput.reformatAmountInput(previous, moneda))
    val parsed = entryEngine.parse(text, phraseContext(today), moneda, cuentas)
    return copy(
        amountInput = parsed.monto?.toAmountInput().orEmpty(),
        amountNeedsReview =
            parsed.monto != null && PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags,
    )
  }

  // Only the default follows new catalog data; a sentence or user choice is re-layered elsewhere.
  private fun AddMovimientoUiState.withCatalog(snapshot: LoadSnapshot): AddMovimientoUiState {
    val catalog = snapshot.cuentas
    val updated =
        copy(
            primaryMoneda = snapshot.moneda ?: primaryMoneda,
            cuentas = catalog?.cuentas ?: cuentas,
            cuentaDefaults = catalog?.defaults ?: cuentaDefaults,
        )
    return if (cuentaChoice.owner == SelectionOwner.DEFAULT)
        updated.copy(cuentaChoice = baseSelection(updated.cuentaDefaults, updated.selectionTipo))
    else updated
  }

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
          cuentaChoice = baseSelection(cuentaDefaults, initialDraft.tipo),
          sentenceSelection = SentenceSelection.EMPTY,
          amountEdited = false,
          cuentaRejected = false,
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
          frecuencia = null,
          frecuenciaEdited = false,
          recurrenceUnsupported = false,
      )

  // A category, tags, medio or cuenta picked in the sentence dialog do not guard it: only a typed
  // sentence is worth confirming.
  private fun AddMovimientoUiState.withDirtyState(): AddMovimientoUiState {
    val draft = toDraft()
    val comparable =
        if (step == AddMovimientoStep.PHRASE)
            draft.copy(
                categoriaId = initialDraft.categoriaId,
                etiquetaIds = initialDraft.etiquetaIds,
                cuentaId = initialDraft.cuentaId,
                medio = initialDraft.medio,
            )
        else draft
    return copy(
        hasUnsavedChanges = phrase.isNotBlank() || comparable != initialDraft || frecuencia != null
    )
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
          amountEdited = false,
          categoriaId = manualCategoriaId,
          etiquetaIds = manualEtiquetaIds,
          cuentaChoice = cuentaChoice.withoutSentence(cuentaDefaults, initialDraft.tipo),
          sentenceSelection = SentenceSelection.EMPTY,
          frecuencia = if (frecuenciaEdited) frecuencia else null,
          recurrenceUnsupported = false,
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
                  frecuencia = interpretation.parsed.frecuencia,
              ),
          suggestionQuery = interpretation.query,
          categoriaId =
              if (categoriasEdited) categoriaId else interpretation.preselectedCategoriaId,
          etiquetaIds = if (etiquetasEdited) etiquetaIds else interpretation.etiquetaIds,
          sentenceSelection = interpretation.sentence,
          cuentaChoice = sentenceChoice(interpretation.sentence, interpretation.tipo),
      )

  private fun AddMovimientoUiState.sentenceChoice(
      sentence: SentenceSelection,
      tipo: TipoMovimiento,
  ): CuentaSelectionState =
      cuentaChoice.withSentence(sentence, cuentas, cuentaDefaults, tipo).forTipo(tipo)

  private fun AddMovimientoUiState.sentenceCuentas() =
      SentenceCuentas(cuentas, primaryMoneda ?: moneda) { sentence, tipo ->
        monedaOf(sentenceChoice(sentence, tipo), tipo)
      }

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
          SentenceCatalogs(state.etiquetas, state.sentenceCuentas()),
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
              SentenceCatalogs(snapshot.etiquetas, snapshot.sentenceCuentas()),
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

  private inner class VoiceHost : AddMovimientoVoiceHost {
    override val state: AddMovimientoUiState
      get() = uiState.value

    override fun dictationLanguageTag(): String =
        entryEngine.dictationLanguageFor(preferredLocale.value)

    override fun updateVoice(transform: VoiceInputUi.() -> VoiceInputUi) =
        this@AddMovimientoViewModel.updateVoice(transform)

    override fun applyDictatedPhrase(value: String) = applyPhrase(value)

    override fun send(effect: AddMovimientoEffect) {
      effectChannel.trySend(effect)
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
            uiState.map { (it.primaryMoneda ?: it.moneda) to it.cuentas }.distinctUntilChanged(),
            preferredLocale,
        ) { history, (moneda, cuentas), locale ->
          HistoryInputs(history, moneda, cuentas, locale)
        }
        .mapLatest { inputs ->
          val context =
              PhraseContext(
                  inputs.moneda,
                  entryEngine.resolveLocale(inputs.locale),
                  captureToday(),
              )
          entryEngine.buildIndex(inputs.history, context, clock.now(), inputs.cuentas)
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
    val cuentas: List<Cuenta>,
    val locale: AppLocale?,
)

internal data class AddMovimientoRepositories(
    val preferences: AppPreferencesRepository,
    val categorias: CategoriaRepository,
    val movimientos: MovimientoRepository,
    val etiquetas: EtiquetaRepository,
    val cuentas: CuentaRepository,
    val recurrencias: RecurrenciaRepository,
    val voice: VoiceDictation,
)

private fun AddMovimientoUiState.toMovimientoOrNull(
    idGenerator: IdGenerator,
    createdAt: kotlin.time.Instant,
    timeZone: kotlinx.datetime.TimeZone,
): Movimiento? {
  val amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money
  val categoriaId = effectiveCategoriaId
  val cuenta = cuentaField.selected
  val medio = cuentaField.effectiveMedio(tipo)
  val isValid = fecha <= createdAt.toLocalDateTime(timeZone).date && !isLoading && error == null
  if (amount == null || categoriaId == null || !isValid) return null
  return if (cuenta == null || medio == null) null
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
          cuentaId = cuenta.id,
          medio = medio,
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

/** The live cuentas plus the defaults resolved against them, once the primary currency is known. */
private data class CuentaCatalog(val cuentas: List<Cuenta>, val defaults: CuentaDefaults)

@OptIn(ExperimentalCoroutinesApi::class)
private fun AddMovimientoRepositories.cuentaLoadStates(
    preferences: LoadState<AppPreferencesState>
): Flow<LoadState<CuentaCatalog>?> {
  val primary =
      ((preferences as? LoadState.Value)?.outcome as? DataOutcome.Success)
          ?.data
          ?.let { it as? AppPreferencesState.Ready }
          ?.value
          ?.monedaPrincipal ?: return flowOf(null)
  return cuentas
      .observeAll()
      .mapLatest<DataOutcome<List<Cuenta>>, LoadState<CuentaCatalog>?> { outcome ->
        LoadState.Value(
            when (outcome) {
              is DataOutcome.Failure -> outcome
              is DataOutcome.Success ->
                  movimientos.cuentaDefaults(primary).map { CuentaCatalog(outcome.data, it) }
            }
        )
      }
      .onStart { emit(LoadState.Loading) }
}

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
    val cuentas: CuentaCatalog?,
    val categorias: List<Categoria>?,
    val isLoading: Boolean,
    val error: MovimientoFormError?,
) {
  companion object {
    fun from(
        preferences: LoadState<AppPreferencesState>,
        cuentas: LoadState<CuentaCatalog>?,
        categorias: LoadState<List<Categoria>>,
    ): LoadSnapshot {
      val cuentaOutcome = (cuentas as? LoadState.Value)?.outcome
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
          cuentas = (cuentaOutcome as? DataOutcome.Success)?.data,
          categorias =
              (categorias as? LoadState.Value)?.outcome.let { it as? DataOutcome.Success }?.data,
          isLoading =
              preferences is LoadState.Loading ||
                  cuentas is LoadState.Loading ||
                  categorias is LoadState.Loading,
          error =
              when {
                (preferences as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                    MovimientoFormError.PREFERENCES_UNAVAILABLE
                (categorias as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                    MovimientoFormError.CATEGORIES_UNAVAILABLE
                cuentaOutcome is DataOutcome.Failure -> MovimientoFormError.ACCOUNTS_UNAVAILABLE
                else -> null
              },
      )
    }
  }
}
