package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {

  fun observeAll(): Flow<DataOutcome<List<Categoria>>>

  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>

  /**
   * Rewrites the stored names of existing rows to the given seed names. Seed names are persisted at
   * seeding time, so they must follow later app-language changes.
   */
  suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit>
}
