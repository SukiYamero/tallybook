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
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val moneda: Moneda,
    val categorias: List<Categoria>,
    val selectedCategoriaId: String?,
    val descripcion: String,
    val isLoading: Boolean,
    val isSubmitting: Boolean,
    val submitAttempted: Boolean,
    val error: MovimientoFormError?,
    val hasUnsavedChanges: Boolean = false,
) {
  private val amountResult by lazy { parseAmountInput(amountInput, moneda) }

  val amountError: AmountInputError?
    get() = (amountResult as? AmountInputResult.Invalid)?.reason?.takeIf { submitAttempted }

  val isCategoriaMissing: Boolean
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
