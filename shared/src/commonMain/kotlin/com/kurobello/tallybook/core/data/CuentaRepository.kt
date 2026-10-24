package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaArchive
import com.kurobello.tallybook.core.model.CuentaDelete
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.flow.Flow

interface CuentaRepository {

  /** Live cuentas, archived included, oldest first. */
  fun observeAll(): Flow<DataOutcome<List<Cuenta>>>

  suspend fun getById(id: String): DataOutcome<Cuenta?>

  /**
   * Creates an active cuenta from the trimmed [draft]. A live cuenta, archived included, with the
   * same [com.kurobello.tallybook.core.model.CuentaRules.nombreKey] returns
   * [CuentaWrite.Duplicate]; a tombstoned one never blocks the name.
   */
  suspend fun create(draft: CuentaDraft): DataOutcome<CuentaWrite>

  /**
   * Same rules as [create], ignoring the cuenta itself. Changing the moneda while live movimientos
   * or transferencias reference the cuenta returns [CuentaWrite.CurrencyLocked].
   */
  suspend fun update(id: String, draft: CuentaDraft): DataOutcome<CuentaWrite>

  /** Archiving the only cuenta not archived returns [CuentaArchive.LastActive]. */
  suspend fun setArchivada(id: String, archivada: Boolean): DataOutcome<CuentaArchive>

  /**
   * Tombstones a cuenta no live movimiento or transferencia references, unless it is the only one
   * not archived.
   */
  suspend fun delete(id: String): DataOutcome<CuentaDelete>
}
