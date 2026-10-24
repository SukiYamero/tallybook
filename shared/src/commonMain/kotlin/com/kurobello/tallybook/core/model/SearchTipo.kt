package com.kurobello.tallybook.core.model

/**
 * Search's type dimension. Transfers are neither gasto nor ingreso, so [TipoMovimiento] stays
 * two-valued and only Search adds [TRANSFERENCIA].
 */
enum class SearchTipo(val tipoMovimiento: TipoMovimiento?) {
  GASTO(TipoMovimiento.GASTO),
  INGRESO(TipoMovimiento.INGRESO),
  TRANSFERENCIA(null),
}
