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

/**
 * The one categoria a picker report means while pickers still report whole lists: the newest real
 * pick replaces the current one, and null means the report dropped the current one.
 */
internal fun chosenCategoriaId(
    current: String?,
    reported: List<String>,
    catalog: List<Categoria>,
): String? {
  val selectable = catalog.mapTo(mutableSetOf(), Categoria::id) - CATEGORIA_OTROS_ID
  return reported.lastOrNull { it != current && it in selectable }
      ?: current?.takeIf { it in reported }
}
