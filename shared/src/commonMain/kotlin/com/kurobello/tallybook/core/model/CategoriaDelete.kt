package com.kurobello.tallybook.core.model

sealed interface CategoriaDelete {

  data object Deleted : CategoriaDelete

  /** Nothing was deleted: [count] movimientos in any moneda still reference the categoria. */
  data class InUse(val count: Long) : CategoriaDelete
}
