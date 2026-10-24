package com.kurobello.tallybook.core.model

/** The editable fields of a [Cuenta]; its moneda is [saldoInicial]'s, stored signed as received. */
data class CuentaDraft(
    val nombre: String,
    val tipo: TipoCuenta,
    val saldoInicial: Money,
    val diaCorte: Int?,
) {
  val moneda: Moneda
    get() = saldoInicial.moneda
}
