package com.kurobello.tallybook.core.model

data class MovimientoSummary(
    val ingresos: Money,
    val gastos: Money,
) {
  val balance: Money
    get() = ingresos - gastos
}
