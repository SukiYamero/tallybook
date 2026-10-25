package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import com.kurobello.tallybook.core.database.Movimiento_con_etiquetas as DatabaseMovimiento
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.database.Transferencia_con_monedas as DatabaseTransferencia
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.ActividadRecentOrder
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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
    return localSnapshots(dispatcher) {
      val movimientos = database.movimientoQueries.selectMovimientosInRange(moneda.name, start, end)
      val transferencias =
          database.transferenciaQueries.selectTransferenciasInRange(
              start,
              end,
              moneda.name,
          )
      rereading(dispatcher, movimientos, transferencias) {
        val (movimientoRows, transferenciaRows) =
            database.transactionWithResult {
              movimientos.executeAsList() to transferencias.executeAsList()
            }
        merge(moneda, movimientoRows, transferenciaRows)
      }
    }
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
      localSnapshots(dispatcher) {
        val years = database.transferenciaQueries.selectActividadYears(moneda.name)
        rereading(dispatcher, years) { parseYears(years.executeAsList()) }
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
    return localSnapshots(dispatcher) {
      val movimientos = filteredMovimientos(criteria, categoryIds, etiquetaIds, cuentaIds)
      val transferencias =
          database.transferenciaQueries.selectFilteredTransferencias(
              criteria.moneda.name,
              criteria.range?.start?.toString(),
              criteria.range?.end?.toString(),
              cuentaFilter,
              cuentaIds,
          )
      // Both queries are observed whatever the criteria exclude, so a write to either table
      // re-reads the years the snapshot carries.
      rereading(dispatcher, movimientos, transferencias) {
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

  private suspend fun merge(
      moneda: Moneda,
      movimientos: List<DatabaseMovimiento>,
      transferencias: List<DatabaseTransferencia>,
  ): List<Actividad> {
    val context = currentCoroutineContext()
    context.ensureActive()
    val rows =
        movimientos.map { row ->
          context.ensureActive()
          Actividad.DeMovimiento(row.toDomain())
        } +
            transferencias.mapNotNull { row ->
              context.ensureActive()
              Actividad.DeTransferencia.of(row.toDomain(), moneda)
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
