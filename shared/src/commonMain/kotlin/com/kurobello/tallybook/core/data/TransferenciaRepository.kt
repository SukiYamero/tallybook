package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult

interface TransferenciaRepository {

  suspend fun getById(id: String): DataOutcome<Transferencia?>

  /**
   * Validates with [com.kurobello.tallybook.core.model.TransferenciaRules] against both live
   * cuentas, archived ones included; any error is invalid input.
   */
  suspend fun insert(transferencia: Transferencia): DataOutcome<Unit>

  /** Same rules as [insert]; [changes] must carry [id], and the stored creation instant stays. */
  suspend fun update(id: String, changes: Transferencia): DataOutcome<TransferenciaUpdateResult>

  /** Tombstones the live row and returns it for Undo, or null when there is none. */
  suspend fun deleteAndCapture(id: String): DataOutcome<Transferencia?>

  /**
   * Undo of [deleteAndCapture]: an archived cuenta stays archived, a missing one is
   * [RestoreResult.MissingCuenta] and a cuenta whose moneda changed is [RestoreResult.Conflict].
   */
  suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult>

  /** Live rows newest first, for CSV export and the future backup. */
  suspend fun getAll(): DataOutcome<List<Transferencia>>

  /**
   * The origin of the newest live transfer if active and not a credit card, else the [primary] cash
   * cuenta, else the first active non-credit cuenta in [primary], then in any moneda. Null only
   * when no active non-credit cuenta exists.
   */
  suspend fun defaultOrigin(primary: Moneda): DataOutcome<String?>
}
