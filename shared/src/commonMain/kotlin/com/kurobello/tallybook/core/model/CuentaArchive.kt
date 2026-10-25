package com.kurobello.tallybook.core.model

sealed interface CuentaArchive {

  data object Updated : CuentaArchive

  /** Nothing was written: archiving would leave no cuenta to pick. */
  data object LastActive : CuentaArchive
}
