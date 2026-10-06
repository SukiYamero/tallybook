package com.kurobello.tallybook.core.model

data class MovimientoSearchSnapshot(
    val movimientos: List<Movimiento>,
    val persistedYears: List<Int>,
) {
  val hasCurrencyData: Boolean
    get() = persistedYears.isNotEmpty()
}
