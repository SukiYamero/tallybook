package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Movimiento
import kotlinx.datetime.LocalDate

internal data class EditMovimientoUiState(
    val original: Movimiento,
    val draft: MovimientoDraft,
    override val categorias: List<Categoria>,
    override val maxSelectableDate: LocalDate,
    override val isLoading: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val submitAttempted: Boolean = false,
    override val error: MovimientoFormError? = null,
    val validationDate: LocalDate = maxSelectableDate,
) : MovimientoFormState {
  override val tipo
    get() = draft.tipo

  override val amountInput
    get() = draft.amountInput

  override val fecha
    get() = draft.fecha

  override val moneda
    get() = original.monto.moneda

  override val selectedCategoriaId
    get() = draft.selectedCategoriaId

  override val descripcion
    get() = draft.descripcion

  override val amountError: AmountInputError?
    get() =
        (parseAmountInput(amountInput, moneda) as? AmountInputResult.Invalid)?.reason?.takeIf {
          submitAttempted
        }

  val hasValidCategoria: Boolean
    get() =
        selectedCategoriaId == original.categoriaId ||
            categorias.any { it.id == selectedCategoriaId }

  override val isCategoriaMissing
    get() = submitAttempted && !hasValidCategoria

  override val isFechaInvalid
    get() = submitAttempted && fecha > validationDate
}
