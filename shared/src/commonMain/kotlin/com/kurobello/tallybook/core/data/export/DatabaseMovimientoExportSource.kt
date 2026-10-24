package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.data.runLocalDataOperation
import com.kurobello.tallybook.core.data.toDomain
import com.kurobello.tallybook.core.data.toSortedDomain
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Reads every list in one transaction: separate reads could pair a movimiento with a cuenta, a
 * categoria or an etiqueta a concurrent write already changed.
 */
internal class DatabaseMovimientoExportSource(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MovimientoExportSource {

  override suspend fun read(): DataOutcome<MovimientoExportContent> =
      runLocalDataOperation(dispatcher) {
        database.transactionWithResult {
          MovimientoExportContent(
              movimientos =
                  database.movimientoQueries.selectAllMovimientos().executeAsList().map {
                    it.toDomain()
                  },
              transferencias =
                  database.transferenciaQueries.selectAllTransferencias().executeAsList().map {
                    it.toDomain()
                  },
              cuentas =
                  database.cuentaQueries.selectAllCuentas().executeAsList().map { it.toDomain() },
              categorias =
                  database.categoriaQueries.selectAllCategorias().executeAsList().map {
                    it.toDomain()
                  },
              etiquetas =
                  database.etiquetaQueries.selectAllEtiquetas().executeAsList().toSortedDomain(),
          )
        }
      }
}
