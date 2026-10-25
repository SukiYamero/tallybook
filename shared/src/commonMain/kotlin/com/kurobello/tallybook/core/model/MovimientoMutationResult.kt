package com.kurobello.tallybook.core.model

enum class MovimientoUpdateResult {
  Updated,
  NotFound,
}

/** Undo of a deleted movimiento or transferencia. */
enum class RestoreResult {
  Restored,
  AlreadyPresent,
  Conflict,

  /** A cuenta the snapshot uses no longer exists; the money never moves to another cuenta. */
  MissingCuenta,
}
