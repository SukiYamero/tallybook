package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CategoriaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CategoriaRepository {

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      database.categoriaQueries
          .selectAllCategorias()
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.map { it.toDomain() } }
          .asLocalDataOutcome()

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      runLocalDataOperation(dispatcher) {
        database.categoriaQueries.selectAllCategorias().executeAsList().map { it.toDomain() }
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

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        val stored =
            database.categoriaQueries.selectAllCategorias().executeAsList().associate {
              it.id to it.nombre
            }
        val renamed = categorias.filter { it.id in stored && stored[it.id] != it.nombre }
        // Any write notifies every category observer, so an unchanged language must not write.
        if (renamed.isNotEmpty()) {
          database.transaction {
            renamed.forEach {
              database.categoriaQueries.updateCategoriaNombre(nombre = it.nombre, id = it.id)
            }
          }
        }
      }
}

private fun com.kurobello.tallybook.core.database.Categoria.toDomain(): Categoria =
    Categoria(
        id = id,
        nombre = nombre,
        padreId = padre_id,
        icono = CategoryIcon.entries.first { it.storageKey == icono },
        color = CategoryTint.entries.first { it.storageKey == color },
    )
