package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher

internal class CategoriaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CategoriaRepository {

  override suspend fun obtenerTodas(): DataOutcome<List<Categoria>> =
      runLocalDataOperation(dispatcher) {
        database.categoriaQueries.selectAllCategorias().executeAsList().map { row ->
          Categoria(
              id = row.id,
              nombre = row.nombre,
              padreId = row.padre_id,
              icono = row.icono,
              color = row.color,
          )
        }
      }
}
