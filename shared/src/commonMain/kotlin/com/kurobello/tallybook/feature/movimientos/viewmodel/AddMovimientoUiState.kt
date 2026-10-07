package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal enum class MovimientoFormError {
  PREFERENCES_UNAVAILABLE,
  CATEGORIES_UNAVAILABLE,
  SAVE_FAILED,
}

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
) : MovimientoFormState {
  private val amountResult by lazy { parseAmountInput(amountInput, moneda) }

  override val amountError: AmountInputError?
    get() = (amountResult as? AmountInputResult.Invalid)?.reason?.takeIf { submitAttempted }

  override val isCategoriaMissing: Boolean
    get() = submitAttempted && !hasSelectedCategoria

  val canSubmit: Boolean
    get() =
        amountResult is AmountInputResult.Valid &&
            hasSelectedCategoria &&
            !isLoading &&
            !isSubmitting &&
            error == null

  private val hasSelectedCategoria: Boolean
    get() = categorias.any { it.id == selectedCategoriaId }
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
