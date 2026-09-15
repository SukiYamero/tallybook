package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher

internal class CategoriaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CategoriaRepository {

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      runLocalDataOperation(dispatcher) {
        database.categoriaQueries.selectAllCategorias().executeAsList().map { row ->
          Categoria(
              id = row.id,
              nombre = row.nombre,
              padreId = row.padre_id,
              icono = CategoryIcon.entries.first { it.storageKey == row.icono },
              color = CategoryTint.entries.first { it.storageKey == row.color },
          )
        }
      }

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        database.transaction {
          categorias.forEach { categoria ->
            database.categoriaQueries.insertCategoriaIfAbsent(
                id = categoria.id,
                nombre = categoria.nombre,
                padre_id = categoria.padreId,
                icono = categoria.icono.storageKey,
                color = categoria.color.storageKey,
            )
          }
        }
      }
}
