package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.Presupuesto as PresupuestoRow
import com.kurobello.tallybook.core.database.SelectLiveWithSpent
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.PresupuestoWrite
import com.kurobello.tallybook.core.model.budgetNotice
import com.kurobello.tallybook.core.model.budgetOrder
import com.kurobello.tallybook.core.model.budgetStatus
import com.kurobello.tallybook.core.model.suggestedBudget
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.yearMonth

private const val HISTORY_MONTHS = 6
private const val SUGGESTION_LOOKBACK_MONTHS = 5
private const val GASTO = "GASTO"

internal class PresupuestoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val changeClock: ChangeClock,
) : PresupuestoRepository {

  private val queries
    get() = database.presupuestoQueries

  override fun observeStatuses(today: LocalDate): Flow<DataOutcome<List<BudgetStatus>>> =
      queries
          .selectLiveWithSpent(today.firstOfMonth().toString(), today.toString())
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.toStatuses(today) }
          .flowOn(dispatcher)
          .asLocalDataOutcome()

  override suspend fun create(
      categoriaId: String,
      monto: Money,
      today: LocalDate,
  ): DataOutcome<PresupuestoWrite> {
    invalidMonto(monto)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val knownCategoria =
                categoriaId != CATEGORIA_OTROS_ID &&
                    database.categoriaQueries
                        .selectCategoriaById(categoriaId)
                        .executeAsOneOrNull() != null
            val duplicate =
                queries
                    .selectLiveByCategoriaMoneda(categoriaId, monto.moneda.name)
                    .executeAsOneOrNull()
            when {
              !knownCategoria -> DataOutcome.Success(PresupuestoWrite.UnknownCategoria)
              duplicate != null ->
                  DataOutcome.Success(PresupuestoWrite.Duplicate(duplicate.toDomain()))
              else -> {
                val presupuesto =
                    Presupuesto(
                        id = idGenerator.next(),
                        categoriaId = categoriaId,
                        monto = monto,
                        createdAt = clock.now(),
                    )
                queries.insertPresupuesto(
                    presupuesto.id,
                    categoriaId,
                    monto.moneda.name,
                    monto.minorUnits,
                    presupuesto.createdAt.toString(),
                    changeClock.next().value,
                )
                DataOutcome.Success(PresupuestoWrite.Saved(presupuesto))
              }
            }
          }
        }
        .flatten()
  }

  override suspend fun updateMonto(id: String, monto: Money): DataOutcome<PresupuestoWrite> {
    invalidMonto(monto)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current =
                queries.selectLiveById(id).executeAsOneOrNull()
                    ?: return@transactionWithResult DataOutcome.Failure(unknownPresupuesto(id))
            if (current.moneda != monto.moneda.name) {
              return@transactionWithResult DataOutcome.Failure(
                  DataError.InvalidInput("Monto must be in ${current.moneda}")
              )
            }
            check(
                queries
                    .updatePresupuestoMonto(monto.minorUnits, changeClock.next().value, id)
                    .value == 1L
            )
            DataOutcome.Success(PresupuestoWrite.Saved(current.toDomain().copy(monto = monto)))
          }
        }
        .flatten()
  }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              if (queries.selectLiveById(id).executeAsOneOrNull() == null) {
                DataOutcome.Failure(unknownPresupuesto(id))
              } else {
                check(queries.tombstonePresupuesto(changeClock.next().value, id).value == 1L)
                DataOutcome.Success(Unit)
              }
            }
          }
          .flatten()

  override suspend fun history(id: String, today: LocalDate): DataOutcome<List<BudgetMonth>> =
      runLocalDataOperation(dispatcher) {
            val current =
                queries.selectLiveById(id).executeAsOneOrNull()
                    ?: return@runLocalDataOperation DataOutcome.Failure(unknownPresupuesto(id))
            val moneda = Moneda.valueOf(current.moneda)
            val spentByMonth =
                queries
                    .sumGastoPresupuestoPorMes(
                        categoria_id = current.categoria_id,
                        moneda = current.moneda,
                        start_date = today.monthsBack(HISTORY_MONTHS - 1).toString(),
                        end_date = today.toString(),
                    )
                    .executeAsList()
                    .associate { YearMonth.parse(it.month) to it.total_minor }
            DataOutcome.Success(
                (HISTORY_MONTHS - 1 downTo 0).map { back ->
                  val month = today.monthsBack(back).yearMonth
                  BudgetMonth(month, Money(spentByMonth[month] ?: 0L, moneda))
                }
            )
          }
          .flatten()

  override suspend fun suggestion(
      categoriaId: String,
      moneda: Moneda,
      today: LocalDate,
  ): DataOutcome<Money?> =
      runLocalDataOperation(dispatcher) {
        val start = today.monthsBack(SUGGESTION_LOOKBACK_MONTHS).toString()
        val categoryGasto =
            queries
                .sumGastoPresupuestoPorMes(
                    categoria_id = categoriaId,
                    moneda = moneda.name,
                    start_date = start,
                    end_date = today.toString(),
                )
                .executeAsList()
                .associate { YearMonth.parse(it.month) to Money(it.total_minor ?: 0L, moneda) }
        val monthsWithGasto =
            database.movimientoQueries
                .sumPorMes(moneda.name, start, today.toString())
                .executeAsList()
                .filter { it.tipo == GASTO }
                .map { YearMonth.parse(it.month) }
                .toSet()
        suggestedBudget(categoryGasto, monthsWithGasto, moneda, today)
      }

  override suspend fun notice(
      touches: List<BudgetTouch>,
      today: LocalDate,
  ): DataOutcome<BudgetNotice?> =
      runLocalDataOperation(dispatcher) {
        val statuses =
            queries
                .selectLiveWithSpent(today.firstOfMonth().toString(), today.toString())
                .executeAsList()
                .toStatuses(today)
        budgetNotice(statuses, touches, today)
      }
}

private fun List<SelectLiveWithSpent>.toStatuses(today: LocalDate): List<BudgetStatus> =
    map { row ->
          val presupuesto =
              Presupuesto(
                  id = row.id,
                  categoriaId = row.categoria_id,
                  monto = Money(row.monto_minor, Moneda.valueOf(row.moneda)),
                  createdAt = Instant.parse(row.created_at),
              )
          budgetStatus(
              presupuesto = presupuesto,
              categoriaNombre = row.categoria_nombre,
              padreId = row.categoria_padre_id,
              spent = Money(row.spent_minor, presupuesto.monto.moneda),
              today = today,
          )
        }
        .sortedWith(budgetOrder)

private fun PresupuestoRow.toDomain() =
    Presupuesto(
        id = id,
        categoriaId = categoria_id,
        monto = Money(monto_minor, Moneda.valueOf(moneda)),
        createdAt = Instant.parse(created_at),
    )

private fun LocalDate.firstOfMonth() = LocalDate(year, month, 1)

private fun LocalDate.monthsBack(months: Int) = firstOfMonth().minus(months, DateTimeUnit.MONTH)

private fun invalidMonto(monto: Money): DataError.InvalidInput? =
    if (monto.minorUnits > 0) null else DataError.InvalidInput("Monto must be positive")

private fun unknownPresupuesto(id: String) = DataError.InvalidInput("Unknown presupuesto $id")
