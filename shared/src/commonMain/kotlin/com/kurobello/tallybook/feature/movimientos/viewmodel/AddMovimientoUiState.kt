package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal enum class MovimientoFormError {
  PREFERENCES_UNAVAILABLE,
  CATEGORIES_UNAVAILABLE,
  SAVE_FAILED,
}

private val STEP_BACK_TARGETS: Map<AddMovimientoStep, AddMovimientoStep> =
    mapOf(AddMovimientoStep.SUMMARY to AddMovimientoStep.PHRASE)

internal data class AddMovimientoUiState(
    override val tipo: TipoMovimiento,
    override val amountInput: String,
    override val fecha: LocalDate,
    override val moneda: Moneda,
    override val categorias: List<Categoria>,
    override val categoriaIds: List<String>,
    override val descripcion: String,
    override val isLoading: Boolean,
    override val isSubmitting: Boolean,
    override val submitAttempted: Boolean,
    override val error: MovimientoFormError?,
    val hasUnsavedChanges: Boolean = false,
    val step: AddMovimientoStep = AddMovimientoStep.PHRASE,
    val phrase: String = "",
    val preview: PhrasePreviewUi? = null,
    val suggestionQuery: SuggestionQuery? = null,
    val suggestions: List<Categoria> = emptyList(),
    val dismissedSuggestionIds: Set<String> = emptySet(),
    // While false the sentence owns the categories; the first manual change takes them over.
    val categoriasEdited: Boolean = false,
    val amountNeedsReview: Boolean = false,
    val today: LocalDate = fecha,
    val formOpenedFromSummary: Boolean = false,
) : MovimientoFormState {
  private val amountResult by lazy { parseAmountInput(amountInput, moneda) }

  override val amountError: AmountInputError?
    get() = (amountResult as? AmountInputResult.Invalid)?.reason?.takeIf { submitAttempted }

  override val isCategoriaMissing: Boolean
    get() = submitAttempted && effectiveCategoriaIds.isEmpty()

  /** The loaded selection, or the pending fallback when nothing valid is selected. */
  val effectiveCategoriaIds: List<String>
    get() =
        categoriaIds.filter(::isLoaded).ifEmpty {
          listOfNotNull(CATEGORIA_OTROS_ID.takeIf(::isLoaded))
        }

  override val displayedCategoriaIds: List<String>
    get() = effectiveCategoriaIds

  val effectiveCategorias: List<Categoria>
    get() = effectiveCategoriaIds.mapNotNull { id -> categorias.firstOrNull { it.id == id } }

  val categoriasArePending: Boolean
    get() = CATEGORIA_OTROS_ID in effectiveCategoriaIds

  val canSubmit: Boolean
    get() =
        amountResult is AmountInputResult.Valid &&
            effectiveCategoriaIds.isNotEmpty() &&
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
            categorias = effectiveCategorias,
            categoriasArePending = categoriasArePending,
            suggestions = suggestions,
            isEnabled = !isSubmitting,
            actionsEnabled = !isLoading && loadError == null,
        )

  // Without the Otros fallback in the catalog the categories are empty, and the review still shows
  // its actions so the user can add one or edit instead of facing an empty sheet.
  val summary: MovimientoSummaryUi
    get() =
        MovimientoSummaryUi(
            tipo = tipo,
            fecha = fecha,
            monto = (amountResult as? AmountInputResult.Valid)?.money,
            amountNeedsReview = amountNeedsReview,
            descripcion = descripcion,
            categorias = effectiveCategorias,
            categoriasArePending = categoriasArePending,
            suggestions = suggestions,
            isSubmitting = isSubmitting,
        )

  private fun isLoaded(id: String): Boolean = categorias.any { it.id == id }
}

internal data class MovimientoDraft(
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val categoriaIds: List<String>,
    val descripcion: String,
)

internal fun AddMovimientoUiState.toDraft(): MovimientoDraft =
    MovimientoDraft(
        tipo = tipo,
        amountInput = amountInput,
        fecha = fecha,
        categoriaIds = categoriaIds,
        descripcion = descripcion,
    )
