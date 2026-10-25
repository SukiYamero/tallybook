package com.kurobello.tallybook.core.model

sealed interface CuentaDelete {

  data object Deleted : CuentaDelete

  /**
   * Nothing was deleted: [count] live movimientos and transferencias still reference the cuenta.
   */
  data class InUse(val count: Long) : CuentaDelete

  /** Nothing was deleted: the cuenta is the only one not archived. */
  data object LastActive : CuentaDelete
}
