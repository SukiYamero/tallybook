package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import com.kurobello.tallybook.core.database.Movimiento_con_etiquetas as DatabaseMovimiento
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.ActividadRecentOrder
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal class ActividadRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ActividadRepository {

  override fun observeActividadRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Actividad>>> {
    validateRange(range)?.let {
      return flowOf(DataOutcome.Failure(it))
    }
    val start = range.start.toString()
    val end = range.end.toString()
    return snapshots {
      val movimientos = database.movimientoQueries.selectMovimientosInRange(moneda.name, start, end)
      val transferencias =
          database.transferenciaQueries.selectTransferenciasInRange(
              start,
              end,
              moneda.name,
              ::transferenciaRow,
          )
      rereading(movimientos, transferencias) {
        val (movimientoRows, transferenciaRows) =
            database.transactionWithResult {
              movimientos.executeAsList() to transferencias.executeAsList()
            }
        merge(moneda, movimientoRows, transferenciaRows)
      }
    }
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = snapshots {
    val years = database.transferenciaQueries.selectActividadYears(moneda.name)
    rereading(years) { parseYears(years.executeAsList()) }
  }

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> {
    val categoryIds = criteria.categoryIds.sorted()
    val etiquetaIds = criteria.etiquetaIds.sorted()
    val cuentaIds = criteria.cuentaIds.sorted()
    val invalidInput =
        when {
          categoryIds.any { it.isBlank() } ->
              DataError.InvalidInput("Categoria ids must not be blank")
          etiquetaIds.any { it.isBlank() } ->
              DataError.InvalidInput("Etiqueta ids must not be blank")
          cuentaIds.any { it.isBlank() } -> DataError.InvalidInput("Cuenta ids must not be blank")
          else -> criteria.range?.let(::validateRange)
        }
    if (invalidInput != null) return flowOf(DataOutcome.Failure(invalidInput))
    val cuentaFilter = if (cuentaIds.isEmpty()) 0L else 1L
    return snapshots {
      val movimientos = filteredMovimientos(criteria, categoryIds, etiquetaIds, cuentaIds)
      val transferencias =
          database.transferenciaQueries.selectFilteredTransferencias(
              criteria.moneda.name,
              criteria.range?.start?.toString(),
              criteria.range?.end?.toString(),
              cuentaFilter,
              cuentaIds,
              ::transferenciaRow,
          )
      // Both queries are observed whatever the criteria exclude, so a write to either table
      // re-reads the years the snapshot carries.
      rereading(movimientos, transferencias) {
        val (rows, years) =
            database.transactionWithResult {
              val movimientoRows =
                  if (criteria.includesMovimientos) movimientos.executeAsList() else emptyList()
              val transferenciaRows =
                  if (criteria.includesTransferencias) transferencias.executeAsList()
                  else emptyList()
              (movimientoRows to transferenciaRows) to
                  database.transferenciaQueries
                      .selectActividadYears(criteria.moneda.name)
                      .executeAsList()
            }
        MovimientoSearchSnapshot(
            merge(criteria.moneda, rows.first, rows.second),
            parseYears(years),
        )
      }
    }
  }

  // Each combination of the optional category and etiqueta sets has its own query: an empty IN
  // list cannot mean "no restriction".
  private fun filteredMovimientos(
      criteria: MovimientoReadCriteria,
      categoryIds: List<String>,
      etiquetaIds: List<String>,
      cuentaIds: List<String>,
  ): Query<DatabaseMovimiento> {
    val queries = database.movimientoQueries
    val args = MovimientoFilterArgs(criteria, cuentaIds)
    return with(args) {
      when {
        categoryIds.isEmpty() && etiquetaIds.isEmpty() ->
            queries.selectFilteredMovimientos(
                moneda,
                start,
                end,
                tipo,
                cuentaFilter,
                cuentaIds,
                medioFilter,
                medios,
            )
        etiquetaIds.isEmpty() ->
            queries.selectFilteredMovimientosByCategory(
                moneda,
                start,
                end,
                tipo,
                cuentaFilter,
                cuentaIds,
                medioFilter,
                medios,
                categoryIds,
            )
        categoryIds.isEmpty() ->
            queries.selectFilteredMovimientosByEtiqueta(
                moneda,
                start,
                end,
                tipo,
                cuentaFilter,
                cuentaIds,
                medioFilter,
                medios,
                etiquetaIds,
            )
        else ->
            queries.selectFilteredMovimientosByCategoryAndEtiqueta(
                moneda,
                start,
                end,
                tipo,
                cuentaFilter,
                cuentaIds,
                medioFilter,
                medios,
                categoryIds,
                etiquetaIds,
            )
      }
    }
  }

  // Queries are built inside the flow so a storage failure while building them is a Local outcome.
  private fun <T> snapshots(source: suspend () -> Flow<T>): Flow<DataOutcome<T>> =
      flow { emitAll(source()) }.flowOn(dispatcher).asLocalDataOutcome()

  /**
   * Runs [read] once and again whenever a table behind [queries] changes. [read] checks
   * cancellation while mapping, and a read finishing after its collector left never publishes.
   */
  private fun <T> rereading(vararg queries: Query<*>, read: suspend () -> T): Flow<T> =
      combine(queries.map { it.asFlow() }) {}
          .map {
            withContext(dispatcher) { read().also { currentCoroutineContext().ensureActive() } }
          }

  private suspend fun merge(
      moneda: Moneda,
      movimientos: List<DatabaseMovimiento>,
      transferencias: List<Transferencia>,
  ): List<Actividad> {
    val context = currentCoroutineContext()
    context.ensureActive()
    val rows =
        movimientos.map { row ->
          context.ensureActive()
          Actividad.DeMovimiento(row.toDomain())
        } +
            transferencias.mapNotNull { transferencia ->
              context.ensureActive()
              Actividad.DeTransferencia.of(transferencia, moneda)
            }
    return if (transferencias.isEmpty()) rows else rows.sortedWith(ActividadRecentOrder)
  }

  private suspend fun parseYears(years: List<String>): List<Int> {
    val context = currentCoroutineContext()
    return years.map { year ->
      context.ensureActive()
      parseStoredYear(year)
    }
  }
}

/** The bound values every movimiento filter query shares; a 0 flag leaves its set unrestricted. */
private class MovimientoFilterArgs(criteria: MovimientoReadCriteria, val cuentaIds: List<String>) {
  val moneda = criteria.moneda.name
  val start = criteria.range?.start?.toString()
  val end = criteria.range?.end?.toString()
  val tipo = criteria.tipo?.tipoMovimiento?.name
  val cuentaFilter = if (cuentaIds.isEmpty()) 0L else 1L
  val medios = criteria.medios.map { it.name }.sorted()
  val medioFilter = if (medios.isEmpty()) 0L else 1L
}

@Suppress("LongParameterList")
private fun transferenciaRow(
    id: String,
    cuentaOrigenId: String,
    cuentaDestinoId: String,
    montoOrigenMinor: Long,
    monedaOrigen: String,
    montoDestinoMinor: Long,
    monedaDestino: String,
    fecha: String,
    descripcion: String?,
    createdAt: String,
): Transferencia =
    Transferencia(
        id = id,
        cuentaOrigenId = cuentaOrigenId,
        cuentaDestinoId = cuentaDestinoId,
        montoOrigen = Money(montoOrigenMinor, Moneda.valueOf(monedaOrigen)),
        montoDestino = Money(montoDestinoMinor, Moneda.valueOf(monedaDestino)),
        fecha = LocalDate.parse(fecha),
        descripcion = descripcion,
        createdAt = Instant.parse(createdAt),
    )
