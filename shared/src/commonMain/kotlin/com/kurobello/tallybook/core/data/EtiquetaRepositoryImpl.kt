package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.Etiqueta as DatabaseEtiqueta
import com.kurobello.tallybook.core.database.EtiquetaQueries
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EtiquetaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : EtiquetaRepository {

  private val queries
    get() = database.etiquetaQueries

  override fun observeAll(): Flow<DataOutcome<List<Etiqueta>>> =
      queries
          .selectAllEtiquetas()
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.toSortedDomain() }
          .asLocalDataOutcome()

  override suspend fun getAll(): DataOutcome<List<Etiqueta>> =
      runLocalDataOperation(dispatcher) {
        queries.selectAllEtiquetas().executeAsList().toSortedDomain()
      }

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      queries
          .countMovimientosPorEtiqueta(moneda.name)
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.associate { it.etiqueta_id to it.movimiento_count } }
          .asLocalDataOutcome()

  override suspend fun create(nombre: String): DataOutcome<EtiquetaWrite> {
    val clean =
        when (val normalized = normalizeNombre(nombre)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
      database.transactionWithResult {
        val duplicate = queries.findDuplicate(clean, excludingId = null)
        if (duplicate != null) {
          EtiquetaWrite.Duplicate(duplicate)
        } else {
          val etiqueta = Etiqueta(id = idGenerator.next(), nombre = clean, createdAt = clock.now())
          queries.insertEtiqueta(etiqueta.id, etiqueta.nombre, etiqueta.createdAt.toString())
          EtiquetaWrite.Saved(etiqueta)
        }
      }
    }
  }

  override suspend fun rename(id: String, nombre: String): DataOutcome<EtiquetaWrite> {
    val clean =
        when (val normalized = normalizeNombre(nombre)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current = queries.selectEtiquetaById(id).executeAsOneOrNull()?.toDomain()
            val duplicate = current?.let { queries.findDuplicate(clean, excludingId = id) }
            when {
              current == null -> DataOutcome.Failure(unknownEtiqueta(id))
              duplicate != null -> DataOutcome.Success(EtiquetaWrite.Duplicate(duplicate))
              else -> {
                // Any write notifies every etiqueta observer, so an unchanged name must not write.
                if (current.nombre != clean) {
                  check(queries.updateEtiquetaNombre(nombre = clean, id = id).value == 1L)
                }
                DataOutcome.Success(EtiquetaWrite.Saved(current.copy(nombre = clean)))
              }
            }
          }
        }
        .flatten()
  }

  override suspend fun countMovimientos(id: String): DataOutcome<Long> =
      runLocalDataOperation(dispatcher) { queries.countMovimientosForEtiqueta(id).executeAsOne() }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              if (queries.selectEtiquetaById(id).executeAsOneOrNull() == null) {
                DataOutcome.Failure(unknownEtiqueta(id))
              } else {
                queries.deleteLinksForEtiqueta(id)
                check(queries.deleteEtiqueta(id).value == 1L)
                DataOutcome.Success(Unit)
              }
            }
          }
          .flatten()
}

private fun normalizeNombre(nombre: String): DataOutcome<String> {
  val clean = nombre.trim()
  val invalid =
      when {
        clean.isEmpty() -> "Etiqueta nombre must not be blank"
        clean.length > CategoriaRules.NOMBRE_MAX -> "Etiqueta nombre is too long"
        else -> null
      }
  return if (invalid != null) DataOutcome.Failure(DataError.InvalidInput(invalid))
  else DataOutcome.Success(clean)
}

private fun EtiquetaQueries.findDuplicate(nombre: String, excludingId: String?): Etiqueta? {
  val key = CategoriaRules.nombreKey(nombre)
  return selectAllEtiquetas()
      .executeAsList()
      .firstOrNull { it.id != excludingId && CategoriaRules.nombreKey(it.nombre) == key }
      ?.toDomain()
}

private fun List<DatabaseEtiqueta>.toSortedDomain(): List<Etiqueta> =
    map { it.toDomain() }
        .sortedWith(compareBy({ CategoriaRules.nombreKey(it.nombre) }, Etiqueta::id))

private fun unknownEtiqueta(id: String) = DataError.InvalidInput("Unknown etiqueta $id")

private fun DatabaseEtiqueta.toDomain(): Etiqueta =
    Etiqueta(id = id, nombre = nombre, createdAt = Instant.parse(created_at))
