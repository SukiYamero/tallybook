package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.CuentaQueries
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaArchive
import com.kurobello.tallybook.core.model.CuentaDelete
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CuentaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val changeClock: ChangeClock,
) : CuentaRepository {

  private val queries
    get() = database.cuentaQueries

  override fun observeAll(): Flow<DataOutcome<List<Cuenta>>> =
      queries
          .selectAllCuentas()
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.map { it.toDomain() } }
          .asLocalDataOutcome()

  override fun observeReferencedIds(): Flow<DataOutcome<Set<String>>> =
      queries
          .selectReferencedCuentaIds()
          .asFlow()
          .mapToList(dispatcher)
          .map { it.toSet() }
          .asLocalDataOutcome()

  override suspend fun getById(id: String): DataOutcome<Cuenta?> =
      runLocalDataOperation(dispatcher) {
        queries.selectCuentaById(id).executeAsOneOrNull()?.toDomain()
      }

  override suspend fun create(draft: CuentaDraft): DataOutcome<CuentaWrite> {
    val clean =
        when (val normalized = normalize(draft)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
      database.transactionWithResult {
        val duplicate = queries.findDuplicate(clean, excludingId = null)
        if (duplicate != null) {
          CuentaWrite.Duplicate(duplicate)
        } else {
          val cuenta =
              Cuenta(
                  id = idGenerator.next(),
                  nombre = clean.nombre,
                  tipo = clean.tipo,
                  saldoInicial = clean.saldoInicial,
                  diaCorte = clean.diaCorte,
                  archivada = false,
                  createdAt = clock.now(),
              )
          database.insertCuentaRow(cuenta, changeClock.next())
          CuentaWrite.Saved(cuenta)
        }
      }
    }
  }

  override suspend fun update(id: String, draft: CuentaDraft): DataOutcome<CuentaWrite> {
    val clean =
        when (val normalized = normalize(draft)) {
          is DataOutcome.Failure -> return normalized
          is DataOutcome.Success -> normalized.data
        }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current =
                queries.selectCuentaById(id).executeAsOneOrNull()?.toDomain()
                    ?: return@transactionWithResult DataOutcome.Failure(unknownCuenta(id))
            val updated =
                current.copy(
                    nombre = clean.nombre,
                    tipo = clean.tipo,
                    saldoInicial = clean.saldoInicial,
                    diaCorte = clean.diaCorte,
                )
            val duplicate = queries.findDuplicate(clean, excludingId = id)
            val referenced by lazy { id in queries.selectReferencedCuentaIds().executeAsList() }
            DataOutcome.Success(
                when {
                  duplicate != null -> CuentaWrite.Duplicate(duplicate)
                  updated.moneda != current.moneda && (current.isDefaultCash || referenced) ->
                      CuentaWrite.CurrencyLocked
                  // Stored medios were checked against the current tipo, so it cannot move under
                  // them.
                  updated.tipo != current.tipo && referenced -> CuentaWrite.TypeLocked
                  updated == current -> CuentaWrite.Saved(current)
                  else -> {
                    check(
                        queries
                            .updateCuenta(
                                nombre = updated.nombre,
                                tipo = updated.tipo.name,
                                moneda = updated.moneda.name,
                                saldo_inicial_minor = updated.saldoInicial.minorUnits,
                                dia_corte = updated.diaCorte?.toLong(),
                                updated_at = changeClock.next().value,
                                id = id,
                            )
                            .value == 1L
                    )
                    CuentaWrite.Saved(updated)
                  }
                }
            )
          }
        }
        .flatten()
  }

  override suspend fun setArchivada(id: String, archivada: Boolean): DataOutcome<CuentaArchive> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              val current =
                  queries.selectCuentaById(id).executeAsOneOrNull()?.toDomain()
                      ?: return@transactionWithResult DataOutcome.Failure(unknownCuenta(id))
              DataOutcome.Success(
                  when {
                    current.archivada == archivada -> CuentaArchive.Updated
                    archivada && queries.isLastActive(id) -> CuentaArchive.LastActive
                    else -> {
                      check(
                          queries
                              .updateCuentaArchivada(
                                  archivada = if (archivada) 1L else 0L,
                                  updated_at = changeClock.next().value,
                                  id = id,
                              )
                              .value == 1L
                      )
                      CuentaArchive.Updated
                    }
                  }
              )
            }
          }
          .flatten()

  // LastActive wins over InUse: the sheet offers archiving for a cuenta in use, which the last
  // active one cannot take either.
  override suspend fun delete(id: String): DataOutcome<CuentaDelete> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              val current =
                  queries.selectCuentaById(id).executeAsOneOrNull()?.toDomain()
                      ?: return@transactionWithResult DataOutcome.Failure(unknownCuenta(id))
              val references = queries.references(id)
              DataOutcome.Success(
                  when {
                    !current.archivada && queries.isLastActive(id) -> CuentaDelete.LastActive
                    references > 0L -> CuentaDelete.InUse(references)
                    else -> {
                      check(
                          queries
                              .tombstoneCuenta(stamp = changeClock.next().value, id = id)
                              .value == 1L
                      )
                      CuentaDelete.Deleted
                    }
                  }
              )
            }
          }
          .flatten()
}

private fun normalize(draft: CuentaDraft): DataOutcome<CuentaDraft> {
  val errors = CuentaRules.validate(draft.nombre, draft.tipo, draft.diaCorte)
  return if (errors.isValid) {
    DataOutcome.Success(draft.copy(nombre = draft.nombre.trim()))
  } else {
    DataOutcome.Failure(DataError.InvalidInput("Invalid cuenta draft: $errors"))
  }
}

private fun CuentaQueries.findDuplicate(draft: CuentaDraft, excludingId: String?): Cuenta? {
  val key = CuentaRules.nombreKey(draft.nombre, draft.moneda)
  return selectAllCuentas()
      .executeAsList()
      .map { it.toDomain() }
      .firstOrNull { it.id != excludingId && CuentaRules.nombreKey(it.nombre, it.moneda) == key }
}

private fun CuentaQueries.references(id: String): Long = countCuentaReferences(id).executeAsOne()

private fun CuentaQueries.isLastActive(id: String): Boolean =
    countActiveCuentasExcept(id).executeAsOne() == 0L

private fun unknownCuenta(id: String) = DataError.InvalidInput("Unknown cuenta $id")
