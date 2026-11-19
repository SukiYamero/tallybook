package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.PresupuestoWrite
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * Serves the given [statuses] and records every write. [createResult] scripts what [create]
 * answers, [failure] makes writes and reads fail, and [writeFailure] fails create, update and
 * delete, [history] and [suggestions] hold the answers of their reads.
 */
internal class FakePresupuestoRepository(initial: List<BudgetStatus> = emptyList()) :
    PresupuestoRepository {

  val statuses = MutableStateFlow<DataOutcome<List<BudgetStatus>>>(DataOutcome.Success(initial))
  val created = mutableListOf<Pair<String, Money>>()
  val updated = mutableListOf<Pair<String, Money>>()
  val deleted = mutableListOf<String>()
  val suggestionReads = mutableListOf<Pair<String, Moneda>>()
  val suggestions = mutableMapOf<Pair<String, Moneda>, Money?>()
  var history: List<BudgetMonth> = emptyList()
  var createResult: PresupuestoWrite? = null
  var suggestionFailure: DataError? = null
  var writeFailure: DataError? = null
  var historyFailure: DataError? = null
  val observedDays = mutableListOf<LocalDate>()

  override fun observeStatuses(today: LocalDate): Flow<DataOutcome<List<BudgetStatus>>> {
    observedDays += today
    return statuses
  }

  override suspend fun create(
      categoriaId: String,
      monto: Money,
      today: LocalDate,
  ): DataOutcome<PresupuestoWrite> {
    writeFailure?.let {
      return DataOutcome.Failure(it)
    }
    created += categoriaId to monto
    return DataOutcome.Success(
        createResult
            ?: PresupuestoWrite.Saved(
                Presupuesto("new-${created.size}", categoriaId, monto, Instant.fromEpochSeconds(0))
            )
    )
  }

  override suspend fun updateMonto(id: String, monto: Money): DataOutcome<PresupuestoWrite> {
    writeFailure?.let {
      return DataOutcome.Failure(it)
    }
    updated += id to monto
    val current = (statuses.value as DataOutcome.Success).data
    val target = current.first { it.presupuesto.id == id }
    val presupuesto = target.presupuesto.copy(monto = monto)
    statuses.value =
        DataOutcome.Success(
            current.map { if (it === target) it.copy(presupuesto = presupuesto) else it }
        )
    return DataOutcome.Success(PresupuestoWrite.Saved(presupuesto))
  }

  override suspend fun delete(id: String): DataOutcome<Unit> {
    writeFailure?.let {
      return DataOutcome.Failure(it)
    }
    deleted += id
    val current = (statuses.value as DataOutcome.Success).data
    statuses.value = DataOutcome.Success(current.filterNot { it.presupuesto.id == id })
    return DataOutcome.Success(Unit)
  }

  override suspend fun history(id: String, today: LocalDate): DataOutcome<List<BudgetMonth>> =
      historyFailure?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(history)

  override suspend fun suggestion(
      categoriaId: String,
      moneda: Moneda,
      today: LocalDate,
  ): DataOutcome<Money?> {
    suggestionReads += categoriaId to moneda
    return suggestionFailure?.let { DataOutcome.Failure(it) }
        ?: DataOutcome.Success(suggestions[categoriaId to moneda])
  }

  override suspend fun notice(
      touches: List<BudgetTouch>,
      today: LocalDate,
  ): DataOutcome<BudgetNotice?> = DataOutcome.Success(null)
}
