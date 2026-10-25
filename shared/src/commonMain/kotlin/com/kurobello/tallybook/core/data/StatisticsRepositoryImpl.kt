package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.DimensionTotalOrder
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.yearMonth

internal class StatisticsRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : StatisticsRepository {

  private val queries
    get() = database.movimientoQueries

  override fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>> {
    validate(request)?.let {
      return flowOf(DataOutcome.Failure(it))
    }
    return localSnapshots(dispatcher) {
      val (moneda, start, end) = bounds(request.moneda, request.period)
      // Together they watch every table the snapshot reads: movimiento, cuenta, categoria and
      // movimiento_etiqueta.
      rereading(
          dispatcher,
          queries.sumGastoPorCategoriaRaiz(moneda, start, end),
          queries.sumGastoPorEtiqueta(moneda, start, end),
      ) {
        database.transactionWithResult { readSnapshot(request) }
      }
    }
  }

  override fun observeCategory(
      request: CategoryStatisticsRequest
  ): Flow<DataOutcome<CategoryStatisticsSnapshot>> {
    validate(request)?.let {
      return flowOf(DataOutcome.Failure(it))
    }
    return localSnapshots(dispatcher) {
      val (moneda, start, end) = bounds(request.moneda, request.period)
      // The view behind the largest gastos covers every table the detail reads.
      rereading(
          dispatcher,
          queries.selectGastosMasGrandesCategoria(moneda, start, end, request.rootId, LARGEST),
      ) {
        database.transactionWithResult { readCategory(request) }
      }
    }
  }

  private fun readSnapshot(request: StatisticsRequest): StatisticsSnapshot {
    val moneda = request.moneda
    val totals = DimensionReader(moneda, request.period, request.comparison)
    return StatisticsSnapshot(
        current = summary(moneda, request.period),
        previous = request.comparison?.let { summary(moneda, it) },
        categorias =
            totals.read { (m, start, end) ->
              queries.sumGastoPorCategoriaRaiz(m, start, end) { id, total ->
                StatisticsKey.Categoria(id) to total
              }
            },
        etiquetas =
            totals.read { (m, start, end) ->
              queries.sumGastoPorEtiqueta(m, start, end) { id, total ->
                StatisticsKey.Etiqueta(id) to total
              }
            },
        cuentas =
            totals.read { (m, start, end) ->
              queries.sumGastoPorCuenta(m, start, end) { id, total ->
                StatisticsKey.Cuenta(id) to total
              }
            },
        medios =
            totals.read { (m, start, end) ->
              queries.sumGastoPorMedio(m, start, end) { medio, total ->
                StatisticsKey.Medio(MedioPago.valueOf(medio)) to total
              }
            },
        months = months(moneda, request.trend),
        daily = daily(moneda, request.daily),
        hasAnyMovimiento = queries.existsMovimientoEnMoneda(moneda.name).executeAsOne(),
    )
  }

  private fun readCategory(request: CategoryStatisticsRequest): CategoryStatisticsSnapshot {
    val moneda = request.moneda
    val rootId = request.rootId
    val subcategorias =
        DimensionReader(moneda, request.period, request.comparison).read { (m, start, end) ->
          queries.sumGastoPorSubcategoria(m, start, end, rootId) { id, total ->
            StatisticsKey.Categoria(id) to total
          }
        }
    val (m, start, end) = bounds(moneda, request.period)
    val (_, trendStart, trendEnd) = bounds(moneda, request.trend)
    val monthly =
        queries
            .sumGastoCategoriaPorMes(m, trendStart, trendEnd, rootId) { month, total ->
              YearMonth.parse(month) to storedMoney(total, moneda)
            }
            .executeAsList()
            .toMap()
    val zero = Money(0, moneda)
    return CategoryStatisticsSnapshot(
        current = subcategorias.fold(zero) { sum, row -> sum + row.current },
        previous =
            request.comparison?.let {
              subcategorias.fold(zero) { sum, row -> sum + checkNotNull(row.previous) }
            },
        count = queries.countGastoCategoria(m, start, end, rootId).executeAsOne(),
        subcategorias = subcategorias,
        months = monthsOf(request.trend).map { it to (monthly[it] ?: zero) },
        largest =
            queries
                .selectGastosMasGrandesCategoria(m, start, end, rootId, LARGEST)
                .executeAsList()
                .map { it.toDomain() },
    )
  }

  private fun summary(moneda: Moneda, range: MovimientoDateRange): MovimientoSummary {
    val (m, start, end) = bounds(moneda, range)
    val totals =
        queries.sumMovimientosByTipoInRange(m, start, end).executeAsList().associate {
          it.tipo to it.total_minor
        }
    return summaryOf(moneda, totals)
  }

  private fun months(moneda: Moneda, trend: MovimientoDateRange): List<MonthTotals> {
    val (m, start, end) = bounds(moneda, trend)
    val byMonth =
        queries
            .sumPorMes(m, start, end) { month, tipo, total ->
              Triple(YearMonth.parse(month), tipo, total)
            }
            .executeAsList()
            .groupBy({ it.first }, { it.second to it.third })
    return monthsOf(trend).map { month ->
      MonthTotals(month, summaryOf(moneda, byMonth[month].orEmpty().toMap()))
    }
  }

  private fun daily(moneda: Moneda, range: MovimientoDateRange): List<DayGasto> {
    val (m, start, end) = bounds(moneda, range)
    val byDay =
        queries
            .sumGastoPorDia(m, start, end) { fecha, total ->
              LocalDate.parse(fecha) to storedMoney(total, moneda)
            }
            .executeAsList()
            .toMap()
    val zero = Money(0, moneda)
    return (range.start..range.end).map { DayGasto(it, byDay[it] ?: zero) }
  }

  /**
   * Reads one dimension for the period and the comparison and keeps every key with gasto in either,
   * so a group that fell to zero can still be the one that dropped the most.
   */
  private class DimensionReader(
      private val moneda: Moneda,
      private val period: MovimientoDateRange,
      private val comparison: MovimientoDateRange?,
  ) {
    fun read(sums: (Bounds) -> Query<Pair<StatisticsKey, Long?>>): List<DimensionTotal> {
      val current = totalsIn(period, sums)
      val previous = comparison?.let { totalsIn(it, sums) }
      val zero = Money(0, moneda)
      return (current.keys + previous.orEmpty().keys)
          .map { key ->
            DimensionTotal(key, current[key] ?: zero, previous?.let { it[key] ?: zero })
          }
          .sortedWith(DimensionTotalOrder)
    }

    private fun totalsIn(
        range: MovimientoDateRange,
        sums: (Bounds) -> Query<Pair<StatisticsKey, Long?>>,
    ): Map<StatisticsKey, Money> =
        sums(bounds(moneda, range))
            .executeAsList()
            .associate { (key, total) -> key to storedMoney(total, moneda) }
            .filterValues { it.minorUnits > 0 }
  }

  private companion object {
    const val LARGEST = 3L
  }
}

private const val MAX_DAILY_DAYS = 366

private fun validate(request: StatisticsRequest): DataError.InvalidInput? =
    listOfNotNull(request.period, request.comparison, request.trend, request.daily)
        .firstNotNullOfOrNull(::validateRange)
        ?: validateTrend(request.trend)
        ?: if (request.daily.start.daysUntil(request.daily.end) >= MAX_DAILY_DAYS) {
          DataError.InvalidInput("Statistics daily range must span at most $MAX_DAILY_DAYS days")
        } else null

private fun validate(request: CategoryStatisticsRequest): DataError.InvalidInput? =
    if (request.rootId.isBlank()) {
      DataError.InvalidInput("Categoria id must not be blank")
    } else {
      listOfNotNull(request.period, request.comparison, request.trend)
          .firstNotNullOfOrNull(::validateRange) ?: validateTrend(request.trend)
    }

private fun validateTrend(trend: MovimientoDateRange): DataError.InvalidInput? =
    if (trend.start.day != 1) {
      DataError.InvalidInput("Statistics trend must start on the first day of a month")
    } else null

private data class Bounds(val moneda: String, val start: String, val end: String)

private fun bounds(moneda: Moneda, range: MovimientoDateRange) =
    Bounds(moneda.name, range.start.toString(), range.end.toString())

private fun monthsOf(range: MovimientoDateRange): List<YearMonth> =
    (range.start.yearMonth..range.end.yearMonth).toList()

private fun summaryOf(moneda: Moneda, totals: Map<String, Long?>): MovimientoSummary =
    MovimientoSummary(
        ingresos = storedMoney(totals[TipoMovimiento.INGRESO.name], moneda),
        gastos = storedMoney(totals[TipoMovimiento.GASTO.name], moneda),
    )

// A total past the per-movimiento cap fails the read: callers subtract and add these totals with
// Money's overflow-checked arithmetic, which would otherwise throw where no outcome catches it.
private fun storedMoney(total: Long?, moneda: Moneda): Money {
  val minorUnits = total ?: 0
  check(minorUnits in 0..Money.MAX_MINOR_UNITS) { "Stored total is outside the supported range" }
  return Money(minorUnits, moneda)
}
