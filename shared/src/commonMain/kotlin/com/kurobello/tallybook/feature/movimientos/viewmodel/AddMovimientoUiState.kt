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
    override val selectedCategoriaId: String?,
    override val descripcion: String,
    override val isLoading: Boolean,
    override val isSubmitting: Boolean,
    override val submitAttempted: Boolean,
    override val error: MovimientoFormError?,
    val hasUnsavedChanges: Boolean = false,
    val step: AddMovimientoStep = AddMovimientoStep.PHRASE,
    val phrase: String = "",
    val preview: PhrasePreviewUi? = null,
    val suggestions: List<Categoria> = emptyList(),
    val dismissedSuggestionIds: Set<String> = emptySet(),
    val amountNeedsReview: Boolean = false,
    val today: LocalDate = fecha,
    val formOpenedFromSummary: Boolean = false,
) : MovimientoFormState {
  private val amountResult by lazy { parseAmountInput(amountInput, moneda) }

  override val amountError: AmountInputError?
    get() = (amountResult as? AmountInputResult.Invalid)?.reason?.takeIf { submitAttempted }

  override val isCategoriaMissing: Boolean
    get() = submitAttempted && effectiveCategoriaId == null

  /** The selection, or the pending fallback when nothing valid is selected. */
  val effectiveCategoriaId: String?
    get() = selectedCategoriaId?.takeIf(::isLoaded) ?: CATEGORIA_OTROS_ID.takeIf(::isLoaded)

  val canSubmit: Boolean
    get() =
        amountResult is AmountInputResult.Valid &&
            effectiveCategoriaId != null &&
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

  val summary: MovimientoSummaryUi?
    get() {
      val categoria = categorias.firstOrNull { it.id == effectiveCategoriaId } ?: return null
      return MovimientoSummaryUi(
          tipo = tipo,
          fecha = fecha,
          monto = (amountResult as? AmountInputResult.Valid)?.money,
          amountNeedsReview = amountNeedsReview,
          descripcion = descripcion,
          categoria = categoria,
          categoriaIsPending = categoria.id == CATEGORIA_OTROS_ID,
          suggestions = suggestions,
          isSubmitting = isSubmitting,
      )
    }

  private fun isLoaded(id: String): Boolean = categorias.any { it.id == id }
}

internal data class MovimientoDraft(
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val selectedCategoriaId: String?,
    val descripcion: String,
)

internal fun AddMovimientoUiState.toDraft(): MovimientoDraft =
    MovimientoDraft(
        tipo = tipo,
        amountInput = amountInput,
        fecha = fecha,
        selectedCategoriaId = selectedCategoriaId,
        descripcion = descripcion,
    )
