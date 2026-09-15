package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome

interface CategoriaRepository {

  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>
}
