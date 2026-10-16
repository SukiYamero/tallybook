package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
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
  val categoriaId: String?
  val etiquetaIds: List<String>
  val titulo: String
  val descripcion: String
  val isLoading: Boolean
  val isSubmitting: Boolean
  val submitAttempted: Boolean
  val error: MovimientoFormError?
  val amountError: AmountInputError?
  val isCategoriaMissing: Boolean

  /** The category the form presents, which is the Otros fallback while none is chosen. */
  val displayedCategoriaId: String?
    get() = categoriaId

  val maxSelectableDate: LocalDate?
    get() = null

  val isFechaInvalid: Boolean
    get() = false
}

/** Otros stands for "no category", so choosing it explicitly would hide a pending draft. */
internal fun isSelectableCategoria(id: String, catalog: List<Categoria>): Boolean =
    id != CATEGORIA_OTROS_ID && catalog.any { it.id == id }
