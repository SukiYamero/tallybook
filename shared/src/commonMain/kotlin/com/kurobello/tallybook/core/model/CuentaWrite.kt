package com.kurobello.tallybook.core.model

sealed interface CuentaWrite {

  data class Saved(val cuenta: Cuenta) : CuentaWrite

  /** Nothing was written: live [existing] already has the same [CuentaRules.nombreKey]. */
  data class Duplicate(val existing: Cuenta) : CuentaWrite

  /** Nothing was written: live movimientos or transferencias still use the current moneda. */
  data object CurrencyLocked : CuentaWrite
}
