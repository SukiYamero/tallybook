package com.kurobello.tallybook.core.model

/** One read of Search's rows and its currency's years, movimientos and transfers alike. */
data class MovimientoSearchSnapshot(
    val actividades: List<Actividad>,
    val persistedYears: List<Int>,
) {
  val hasCurrencyData: Boolean
    get() = persistedYears.isNotEmpty()

  val movimientos: List<Movimiento>
    get() = actividades.mapNotNull { (it as? Actividad.DeMovimiento)?.movimiento }
}
