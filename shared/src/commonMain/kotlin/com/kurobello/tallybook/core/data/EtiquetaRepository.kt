package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.coroutines.flow.Flow

interface EtiquetaRepository {

  /** Sorted by name, ignoring case and supported accents. */
  fun observeAll(): Flow<DataOutcome<List<Etiqueta>>>

  suspend fun getAll(): DataOutcome<List<Etiqueta>>

  /**
   * Movimientos in [moneda] per etiqueta. Etiquetas without movimientos are absent from the map.
   */
  fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>>

  /**
   * Creates an etiqueta with the trimmed [nombre] and [descripcion] with its whitespace runs
   * collapsed, a blank one stored as null. A name whose
   * [com.kurobello.tallybook.core.model.CategoriaRules.nombreKey] matches a stored etiqueta returns
   * [EtiquetaWrite.Duplicate] without writing.
   */
  suspend fun create(nombre: String, descripcion: String? = null): DataOutcome<EtiquetaWrite>

  /** Same rules as [create], ignoring the etiqueta itself; unchanged values do not write. */
  suspend fun update(id: String, nombre: String, descripcion: String?): DataOutcome<EtiquetaWrite>

  /** Movimientos in any moneda that carry [id]. */
  suspend fun countMovimientos(id: String): DataOutcome<Long>

  /** Removes [id] from every movimiento and deletes it in one transaction; movimientos stay. */
  suspend fun delete(id: String): DataOutcome<Unit>
}
