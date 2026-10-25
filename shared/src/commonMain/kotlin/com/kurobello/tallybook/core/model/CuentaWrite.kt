package com.kurobello.tallybook.core.model

sealed interface CuentaWrite {

  data class Saved(val cuenta: Cuenta) : CuentaWrite

  /** Nothing was written: live [existing] already has the same [CuentaRules.nombreKey]. */
  data class Duplicate(val existing: Cuenta) : CuentaWrite

  /**
   * Nothing was written: live movimientos or transferencias still use the current moneda, or the
   * cuenta is a default cash cuenta, whose id names its moneda.
   */
  data object CurrencyLocked : CuentaWrite

  /** Nothing was written: live movimientos or transferencias still rely on the current tipo. */
  data object TypeLocked : CuentaWrite
}
