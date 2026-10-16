package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.coroutines.flow.Flow

/**
 * Delegate for fakes that override only what their test reads (`CategoriaRepository by
 * UnsupportedCategoriaRepository`), so a call the test did not plan for fails loudly.
 */
internal object UnsupportedCategoriaRepository : CategoriaRepository {

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = unsupported()

  override suspend fun getAll(): DataOutcome<List<Categoria>> = unsupported()

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> = unsupported()

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      unsupported()

  override suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite> = unsupported()

  override suspend fun update(id: String, draft: CategoriaDraft): DataOutcome<CategoriaWrite> =
      unsupported()

  override suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit> = unsupported()

  override suspend fun delete(id: String): DataOutcome<CategoriaDelete> = unsupported()

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      unsupported()

  private fun unsupported(): Nothing = error("Not used by this test")
}
