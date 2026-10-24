package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import kotlinx.datetime.LocalDate

internal enum class MovimientoFormError {
  PREFERENCES_UNAVAILABLE,
  CATEGORIES_UNAVAILABLE,
  ACCOUNTS_UNAVAILABLE,
  SAVE_FAILED,
}

private val STEP_BACK_TARGETS: Map<AddMovimientoStep, AddMovimientoStep> =
    mapOf(AddMovimientoStep.SUMMARY to AddMovimientoStep.PHRASE)

internal data class AddMovimientoUiState(
    override val tipo: TipoMovimiento,
    override val amountInput: String,
    override val fecha: LocalDate,
    override val categorias: List<Categoria>,
    override val categoriaId: String?,
    override val titulo: String,
    override val descripcion: String,
    override val isLoading: Boolean,
    override val isSubmitting: Boolean,
    override val submitAttempted: Boolean,
    override val error: MovimientoFormError?,
    override val etiquetaIds: List<String> = emptyList(),
    override val etiquetas: List<Etiqueta> = emptyList(),
    override val etiquetasLoad: EtiquetasLoad = EtiquetasLoad.LOADING,
    override val failedEtiquetaNombre: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val step: AddMovimientoStep = AddMovimientoStep.PHRASE,
    val phrase: String = "",
    val preview: PhrasePreviewUi? = null,
    val suggestionQuery: SuggestionQuery? = null,
    override val suggestions: List<Categoria> = emptyList(),
    val dismissedSuggestionIds: Set<String> = emptySet(),
    // While false the sentence owns the category or the tags; the first manual change takes over.
    val categoriasEdited: Boolean = false,
    val etiquetasEdited: Boolean = false,
    val amountNeedsReview: Boolean = false,
    val today: LocalDate = fecha,
    val formOpenedFromSummary: Boolean = false,
    val cuentas: List<Cuenta> = emptyList(),
    val primaryMoneda: Moneda? = null,
    val cuentaDefaults: CuentaDefaults? = null,
    /** The medio and cuenta the movimiento is saved with; [moneda] follows the cuenta. */
    val cuentaChoice: CuentaSelectionState = CuentaSelectionState.NONE,
    /** The last interpretation's medio and cuenta, whose alternatives stay offered. */
    val sentenceSelection: SentenceSelection = SentenceSelection.EMPTY,
    // While false the sentence owns the amount, so another cuenta's currency re-derives it.
    val amountEdited: Boolean = false,
    val cuentaRejected: Boolean = false,
) : MovimientoFormState {
  /** The tipo medio options follow: the sentence's while it is being typed, the draft's after. */
  val selectionTipo: TipoMovimiento
    get() = if (step == AddMovimientoStep.PHRASE) detected.tipo else tipo

  override val cuentaField: MovimientoCuentaUi by lazy {
    movimientoCuentaUi(
        selection = cuentaChoice,
        cuentas = cuentas,
        tipo = selectionTipo,
        submitAttempted = submitAttempted && step != AddMovimientoStep.PHRASE,
        cuentaRejected = cuentaRejected,
        primaryMoneda = primaryMoneda,
        suggestionIds = sentenceSelection.cuenta.alternativeIds,
    )
  }

  override val moneda: Moneda
    get() = monedaOf(cuentaField.selected)

  private val amountResult by lazy { parseAmountInput(amountInput, moneda) }

  fun monedaOf(cuenta: Cuenta?): Moneda = cuenta?.moneda ?: primaryMoneda ?: Moneda.COP

  /** The currency [choice] would leave the draft in, before it is applied. */
  fun monedaOf(choice: CuentaSelectionState, tipo: TipoMovimiento): Moneda =
      monedaOf(movimientoCuentaUi(choice, cuentas, tipo, false, false).selected)

  override val amountError: AmountInputError?
    get() = (amountResult as? AmountInputResult.Invalid)?.reason?.takeIf { submitAttempted }

  override val isCategoriaMissing: Boolean
    get() = submitAttempted && effectiveCategoriaId == null

  /** The loaded selection, or the pending fallback when nothing valid is selected. */
  val effectiveCategoriaId: String?
    get() = categoriaId?.takeIf(::isLoaded) ?: CATEGORIA_OTROS_ID.takeIf(::isLoaded)

  override val displayedCategoriaId: String?
    get() = effectiveCategoriaId

  val effectiveCategoria: Categoria?
    get() = categorias.firstOrNull { it.id == effectiveCategoriaId }

  val categoriaIsPending: Boolean
    get() = effectiveCategoriaId == CATEGORIA_OTROS_ID

  val canSubmit: Boolean
    get() =
        amountResult is AmountInputResult.Valid &&
            effectiveCategoriaId != null &&
            cuentaField.selected != null &&
            cuentaField.effectiveMedio(tipo) != null &&
            !isLoading &&
            !isSubmitting &&
            error == null

  /** Load failures block entry; a failed save keeps the draft and leaves Save available. */
  val loadError: MovimientoFormError?
    get() = error?.takeUnless { it == MovimientoFormError.SAVE_FAILED }

  val canContinuePhrase: Boolean
    get() = phrase.isNotBlank() && !isSubmitting && !isLoading && loadError == null

  val stepBackTarget: AddMovimientoStep?
    get() =
        STEP_BACK_TARGETS[step]
            ?: AddMovimientoStep.SUMMARY.takeIf {
              step == AddMovimientoStep.FORM && formOpenedFromSummary
            }

  /** What the sentence dialog shows as detected, with the defaults until the text says more. */
  val detected: PhrasePreviewUi
    get() = preview ?: PhrasePreviewUi(TipoMovimiento.GASTO, monto = null, fecha = today)

  val sentence: MovimientoSentenceUi
    get() =
        MovimientoSentenceUi(
            text = phrase,
            detected = detected,
            today = today,
            moneda = moneda,
            categoria = effectiveCategoria,
            categoriaIsPending = categoriaIsPending,
            suggestions = suggestions,
            etiquetas = selectedEtiquetas,
            etiquetasLoad = etiquetasLoad,
            cuenta = cuentaField,
            isEnabled = !isSubmitting,
            actionsEnabled = !isLoading && loadError == null,
        )

  // Without the Otros fallback in the catalog the category is empty, and the review still shows
  // its actions so the user can add one or edit instead of facing an empty sheet.
  val summary: MovimientoSummaryUi
    get() =
        MovimientoSummaryUi(
            tipo = tipo,
            fecha = fecha,
            monto = (amountResult as? AmountInputResult.Valid)?.money,
            amountNeedsReview = amountNeedsReview,
            titulo = titulo,
            categoria = effectiveCategoria,
            categoriaIsPending = categoriaIsPending,
            suggestions = suggestions,
            etiquetas = selectedEtiquetas,
            etiquetasLoad = etiquetasLoad,
            cuenta = cuentaField,
            isSubmitting = isSubmitting,
        )

  private fun isLoaded(id: String): Boolean = categorias.any { it.id == id }
}

internal data class MovimientoDraft(
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val categoriaId: String?,
    val etiquetaIds: List<String>,
    val titulo: String,
    val descripcion: String,
    val cuentaId: String? = null,
    val medio: MedioPago? = null,
)

/** Only a medio and cuenta picked by hand count as a draft change; defaults are not the user's. */
internal fun AddMovimientoUiState.toDraft(): MovimientoDraft =
    MovimientoDraft(
        tipo = tipo,
        amountInput = amountInput,
        fecha = fecha,
        categoriaId = categoriaId,
        etiquetaIds = etiquetaIds,
        titulo = titulo,
        descripcion = descripcion,
        cuentaId = cuentaChoice.cuentaId.takeIf { cuentaChoice.owner == SelectionOwner.USER },
        medio = cuentaChoice.medio.takeIf { cuentaChoice.owner == SelectionOwner.USER },
    )
