package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.andThen
import com.kurobello.tallybook.core.model.map
import kotlinx.coroutines.flow.first

/** Fails on the first read that fails, so a share never receives a partial export. */
internal class MovimientoExportSource(
    private val movimientos: MovimientoRepository,
    private val transferencias: TransferenciaRepository,
    private val cuentas: CuentaRepository,
    private val categorias: CategoriaRepository,
    private val etiquetas: EtiquetaRepository,
) {
  suspend fun read(): DataOutcome<MovimientoExportContent> =
      movimientos.getAll().andThen { movimientos ->
        transferencias.getAll().andThen { transferencias ->
          cuentas.observeAll().first().andThen { cuentas ->
            categorias.getAll().andThen { categorias ->
              etiquetas.getAll().map { etiquetas ->
                MovimientoExportContent(movimientos, transferencias, cuentas, categorias, etiquetas)
              }
            }
          }
        }
      }
}
