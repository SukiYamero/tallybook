package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {

  fun observeAll(): Flow<DataOutcome<List<Categoria>>>

  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>

  /**
   * Rewrites the stored names of existing seeded rows to the given seed names. Seed names are
   * persisted at seeding time, so they must follow later app-language changes; custom rows keep the
   * name the user gave them.
   */
  suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit>

  /**
   * Creates a custom root categoria from the trimmed [draft]. A name whose
   * [com.kurobello.tallybook.core.model.CategoriaRules.nombreKey] matches any stored categoria,
   * hidden or seeded included, returns [CategoriaWrite.Duplicate] without writing.
   */
  suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite>

  /** Same rules as [create], ignoring the row itself; a seeded categoria cannot be edited. */
  suspend fun update(id: String, draft: CategoriaDraft): DataOutcome<CategoriaWrite>

  /** Any categoria except Otros, which must stay available as the fallback. */
  suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit>

  /** Custom categorias only; one referenced by any movimiento returns [CategoriaDelete.InUse]. */
  suspend fun delete(id: String): DataOutcome<CategoriaDelete>

  /** Movimientos in any moneda that carry [id] itself, the count [delete] refuses on. */
  suspend fun countMovimientos(id: String): DataOutcome<Long>

  /**
   * Distinct movimientos in [moneda] per categoria, a parent including its children's. Categorias
   * without movimientos are absent from the map.
   */
  fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>>
}
