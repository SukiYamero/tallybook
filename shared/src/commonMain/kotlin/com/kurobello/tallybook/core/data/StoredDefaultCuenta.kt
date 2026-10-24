package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Inserts [cuenta] with [ChangeStamp.SEED] only while no live cuenta exists, so only a fresh
 * install gets one: a migrated or user-created cuenta, in any currency, prevents it.
 */
internal suspend fun TallybookDatabase.seedDefaultCuenta(
    cuenta: Cuenta,
    dispatcher: CoroutineDispatcher,
): DataOutcome<Unit> =
    runLocalDataOperation(dispatcher) {
      transaction {
        if (cuentaQueries.countLiveCuentas().executeAsOne() == 0L) {
          insertCuentaRow(cuenta, ChangeStamp.SEED)
        }
      }
    }
