package com.kurobello.tallybook.core.model

enum class MovimientoUpdateResult {
  Updated,
  NotFound,
}

enum class MovimientoRestoreResult {
  Restored,
  AlreadyPresent,
  Conflict,
}
