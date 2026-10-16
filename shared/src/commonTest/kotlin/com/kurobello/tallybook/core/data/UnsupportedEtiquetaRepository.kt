package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.coroutines.flow.Flow

/**
 * Delegate for fakes that override only what their test reads (`EtiquetaRepository by
 * UnsupportedEtiquetaRepository`), so a call the test did not plan for fails loudly.
 */
internal object UnsupportedEtiquetaRepository : EtiquetaRepository {

  override fun observeAll(): Flow<DataOutcome<List<Etiqueta>>> = unsupported()

  override suspend fun getAll(): DataOutcome<List<Etiqueta>> = unsupported()

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      unsupported()

  override suspend fun create(nombre: String): DataOutcome<EtiquetaWrite> = unsupported()

  override suspend fun rename(id: String, nombre: String): DataOutcome<EtiquetaWrite> =
      unsupported()

  override suspend fun countMovimientos(id: String): DataOutcome<Long> = unsupported()

  override suspend fun delete(id: String): DataOutcome<Unit> = unsupported()

  private fun unsupported(): Nothing = error("Not used by this test")
}
