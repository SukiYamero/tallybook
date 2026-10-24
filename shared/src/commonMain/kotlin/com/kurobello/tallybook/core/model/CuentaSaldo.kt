package com.kurobello.tallybook.core.model

data class CuentaSaldo(val cuenta: Cuenta, val saldo: Money)

/** Both values are estimates: the user may not record every purchase on the card. */
data class CreditEstimate(
    val deuda: Money,
    val cycleSpending: Money,
    val cycle: MovimientoDateRange,
) {
  companion object {
    /** A positive card balance is a credit in the user's favour, not a negative debt. */
    fun deudaOf(saldo: Money): Money = if (saldo.minorUnits < 0) -saldo else Money(0, saldo.moneda)
  }
}
