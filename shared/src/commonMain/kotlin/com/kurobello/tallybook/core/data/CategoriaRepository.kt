package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {

  fun observeAll(): Flow<DataOutcome<List<Categoria>>>

  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>
}
