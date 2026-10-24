package com.kurobello.tallybook.core.model

import kotlin.time.Instant

enum class TipoCuenta {
  EFECTIVO,
  BANCO,
  CREDITO;

  /** The medio implied when only the account is known, e.g. a sentence naming "Nequi". */
  fun defaultMedio(tipo: TipoMovimiento): MedioPago = defaultMedios.getValue(this).forTipo(tipo)
}

// A map instead of a constructor argument: MedioPago's entries already reference TipoCuenta.
private val defaultMedios: Map<TipoCuenta, MedioPago> =
    mapOf(
        TipoCuenta.EFECTIVO to MedioPago.EFECTIVO,
        TipoCuenta.BANCO to MedioPago.DEBITO,
        TipoCuenta.CREDITO to MedioPago.CREDITO,
    )

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
