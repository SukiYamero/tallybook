package com.kurobello.tallybook.core.model

import kotlin.time.Instant

enum class TipoCuenta {
  EFECTIVO,
  BANCO,
  CREDITO,
}

/**
 * [saldoInicial] is signed and stored as received: a credit card's opening debt is already negative
 * here. [diaCorte] is the statement day of a [TipoCuenta.CREDITO] account, see [creditCycle].
 */
data class Cuenta(
    val id: String,
    val nombre: String,
    val tipo: TipoCuenta,
    val saldoInicial: Money,
    val diaCorte: Int?,
    val archivada: Boolean,
    val createdAt: Instant,
) {
  val moneda: Moneda
    get() = saldoInicial.moneda

  companion object {
    /** Deterministic so two installations seeding the same default produce the same entity. */
    fun defaultCashId(moneda: Moneda): String = "cuenta_efectivo_${moneda.name.lowercase()}"
  }
}
