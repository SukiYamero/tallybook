package com.kurobello.tallybook.core.model

/**
 * How a movimiento was paid or received. [TRANSFERENCIA] is money sent to or received from someone
 * else; moving money between the user's own accounts is a [Transferencia], never a movimiento.
 */
enum class MedioPago(
    val compatibleTipos: Set<TipoCuenta>,
    private val allowedTipos: Set<TipoMovimiento>,
) {
  EFECTIVO(setOf(TipoCuenta.EFECTIVO), TipoMovimiento.entries.toSet()),
  DEBITO(setOf(TipoCuenta.BANCO), setOf(TipoMovimiento.GASTO)),
  TRANSFERENCIA(setOf(TipoCuenta.BANCO), TipoMovimiento.entries.toSet()),
  CREDITO(setOf(TipoCuenta.CREDITO), TipoMovimiento.entries.toSet());

  fun allows(tipo: TipoMovimiento): Boolean = tipo in allowedTipos

  /** The medio a draft keeps when its tipo changes; the replacement uses the same account. */
  fun forTipo(tipo: TipoMovimiento): MedioPago =
      if (allows(tipo)) this else replacements.getValue(this)
}

private val replacements: Map<MedioPago, MedioPago> =
    mapOf(MedioPago.DEBITO to MedioPago.TRANSFERENCIA)
