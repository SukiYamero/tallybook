package com.kurobello.tallybook.core.model

import kotlinx.datetime.YearMonth

sealed interface PresupuestoWrite {
  data class Saved(val presupuesto: Presupuesto) : PresupuestoWrite

  data class Duplicate(val existing: Presupuesto) : PresupuestoWrite

  /** The categoria is missing, deleted, or Otros, which holds no budget. */
  data object UnknownCategoria : PresupuestoWrite
}

/** [recorded] is whether the user logged any gasto that month, in any categoria of the moneda. */
data class BudgetMonth(val month: YearMonth, val spent: Money, val recorded: Boolean)
