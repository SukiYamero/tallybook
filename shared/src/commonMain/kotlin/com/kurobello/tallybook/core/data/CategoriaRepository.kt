package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome

interface CategoriaRepository {

  suspend fun obtenerTodas(): DataOutcome<List<Categoria>>
}
