package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal interface MovimientoFormState {
  val tipo: TipoMovimiento
  val amountInput: String
  val fecha: LocalDate
  val moneda: Moneda
  val categorias: List<Categoria>
  val selectedCategoriaId: String?
  val descripcion: String
  val isLoading: Boolean
  val isSubmitting: Boolean
  val submitAttempted: Boolean
  val error: MovimientoFormError?
  val amountError: AmountInputError?
  val isCategoriaMissing: Boolean
  val maxSelectableDate: LocalDate?
    get() = null

  val isFechaInvalid: Boolean
    get() = false
}
