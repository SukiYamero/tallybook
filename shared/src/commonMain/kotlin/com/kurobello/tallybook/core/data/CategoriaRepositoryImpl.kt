package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.CategoriaQueries
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.fold
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CategoriaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
) : CategoriaRepository {

  private val queries
    get() = database.categoriaQueries

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      queries
          .selectAllCategorias()
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.map { it.toDomain() } }
          .asLocalDataOutcome()

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      runLocalDataOperation(dispatcher) {
        queries.selectAllCategorias().executeAsList().map { it.toDomain() }
      }

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        database.transaction {
          categorias.forEach { categoria ->
            queries.insertCategoriaIfAbsent(
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
            queries
                .selectAllCategorias()
                .executeAsList()
                .filter { it.origen == CategoriaOrigen.SISTEMA.storageKey }
                .associate { it.id to it.nombre }
        val renamed = categorias.filter { it.id in stored && stored[it.id] != it.nombre }
        // Any write notifies every category observer, so an unchanged language must not write.
        if (renamed.isNotEmpty()) {
          database.transaction {
            renamed.forEach { queries.updateCategoriaNombre(nombre = it.nombre, id = it.id) }
          }
        }
      }

  override suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite> {
    val clean =
        when (val normalized = normalize(draft)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
      database.transactionWithResult {
        val duplicate = queries.findDuplicate(clean.nombre, excludingId = null)
        if (duplicate != null) {
          CategoriaWrite.Duplicate(duplicate)
        } else {
          val categoria =
              Categoria(
                  id = idGenerator.next(),
                  nombre = clean.nombre,
                  padreId = null,
                  icono = clean.icono,
                  color = clean.color,
                  descripcion = clean.descripcion,
                  origen = CategoriaOrigen.USUARIO,
              )
          queries.insertCustomCategoria(
              id = categoria.id,
              nombre = categoria.nombre,
              icono = categoria.icono.storageKey,
              color = categoria.color.storageKey,
              descripcion = categoria.descripcion,
          )
          CategoriaWrite.Saved(categoria)
        }
      }
    }
  }

  override suspend fun update(id: String, draft: CategoriaDraft): DataOutcome<CategoriaWrite> {
    val clean =
        when (val normalized = normalize(draft)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current = queries.selectCategoriaById(id).executeAsOneOrNull()?.toDomain()
            val duplicate = current?.let { queries.findDuplicate(clean.nombre, excludingId = id) }
            when {
              current == null -> DataOutcome.Failure(unknownCategoria(id))
              current.origen != CategoriaOrigen.USUARIO ->
                  DataOutcome.Failure(DataError.InvalidInput("A seeded categoria cannot be edited"))
              duplicate != null -> DataOutcome.Success(CategoriaWrite.Duplicate(duplicate))
              else -> {
                val updated =
                    current.copy(
                        nombre = clean.nombre,
                        icono = clean.icono,
                        color = clean.color,
                        descripcion = clean.descripcion,
                    )
                if (updated != current) {
                  check(
                      queries
                          .updateCustomCategoria(
                              nombre = updated.nombre,
                              icono = updated.icono.storageKey,
                              color = updated.color.storageKey,
                              descripcion = updated.descripcion,
                              id = id,
                          )
                          .value == 1L
                  )
                }
                DataOutcome.Success(CategoriaWrite.Saved(updated))
              }
            }
          }
        }
        .flatten()
  }

  override suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit> {
    if (id == CATEGORIA_OTROS_ID) {
      return DataOutcome.Failure(DataError.InvalidInput("Otros cannot be hidden"))
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current = queries.selectCategoriaById(id).executeAsOneOrNull()?.toDomain()
            when {
              current == null -> DataOutcome.Failure(unknownCategoria(id))
              // Any write notifies every category observer, so an unchanged value must not write.
              current.oculta == oculta -> DataOutcome.Success(Unit)
              else -> {
                check(
                    queries.updateCategoriaOculta(oculta = if (oculta) 1L else 0L, id = id).value ==
                        1L
                )
                DataOutcome.Success(Unit)
              }
            }
          }
        }
        .flatten()
  }

  override suspend fun delete(id: String): DataOutcome<CategoriaDelete> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              val current = queries.selectCategoriaById(id).executeAsOneOrNull()?.toDomain()
              when {
                current == null -> DataOutcome.Failure(unknownCategoria(id))
                current.origen != CategoriaOrigen.USUARIO ->
                    DataOutcome.Failure(
                        DataError.InvalidInput("A seeded categoria cannot be deleted")
                    )
                else -> {
                  val count = queries.countMovimientosForCategoria(id).executeAsOne()
                  if (count > 0L) {
                    DataOutcome.Success(CategoriaDelete.InUse(count))
                  } else {
                    check(queries.deleteCustomCategoria(id).value == 1L)
                    DataOutcome.Success(CategoriaDelete.Deleted)
                  }
                }
              }
            }
          }
          .flatten()

  override suspend fun countMovimientos(id: String): DataOutcome<Long> =
      runLocalDataOperation(dispatcher) { queries.countMovimientosForCategoria(id).executeAsOne() }

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      queries
          .countMovimientosPorCategoria(moneda.name)
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.associate { it.categoria_id to it.movimiento_count } }
          .asLocalDataOutcome()
}

private fun normalize(draft: CategoriaDraft): DataOutcome<CategoriaDraft> {
  val nombre = draft.nombre.trim()
  val descripcion = draft.descripcion?.trim()?.ifEmpty { null }
  val invalid =
      when {
        nombre.isEmpty() -> "Categoria nombre must not be blank"
        nombre.length > CategoriaRules.NOMBRE_MAX -> "Categoria nombre is too long"
        (descripcion?.length ?: 0) > CategoriaRules.DESCRIPCION_MAX ->
            "Categoria descripcion is too long"
        else -> null
      }
  return if (invalid != null) {
    DataOutcome.Failure(DataError.InvalidInput(invalid))
  } else {
    DataOutcome.Success(draft.copy(nombre = nombre, descripcion = descripcion))
  }
}

private fun CategoriaQueries.findDuplicate(nombre: String, excludingId: String?): Categoria? {
  val key = CategoriaRules.nombreKey(nombre)
  return selectAllCategorias()
      .executeAsList()
      .firstOrNull { it.id != excludingId && CategoriaRules.nombreKey(it.nombre) == key }
      ?.toDomain()
}

private fun unknownCategoria(id: String) = DataError.InvalidInput("Unknown categoria $id")

private fun <T> DataOutcome<DataOutcome<T>>.flatten(): DataOutcome<T> =
    fold(onSuccess = { it }, onFailure = { DataOutcome.Failure(it) })

private fun com.kurobello.tallybook.core.database.Categoria.toDomain(): Categoria =
    Categoria(
        id = id,
        nombre = nombre,
        padreId = padre_id,
        icono = CategoryIcon.entries.first { it.storageKey == icono },
        color = CategoryTint.entries.first { it.storageKey == color },
        descripcion = descripcion,
        origen = CategoriaOrigen.entries.first { it.storageKey == origen },
        oculta = oculta != 0L,
    )
