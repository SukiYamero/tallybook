package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.PresupuestoWrite
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * One live budget per categoria and currency. An unknown presupuesto id or a non-positive or
 * mismatched-currency monto is a [com.kurobello.tallybook.core.model.DataError.InvalidInput].
 */
internal interface PresupuestoRepository {

  /** Live budgets with their spent from the first of [today]'s month through [today]. */
  fun observeStatuses(today: LocalDate): Flow<DataOutcome<List<BudgetStatus>>>

  suspend fun create(
      categoriaId: String,
      monto: Money,
      today: LocalDate,
  ): DataOutcome<PresupuestoWrite>

  suspend fun updateMonto(id: String, monto: Money): DataOutcome<PresupuestoWrite>

  suspend fun delete(id: String): DataOutcome<Unit>

  /** The six months ending at [today]'s, oldest first, with zero for months without gasto. */
  suspend fun history(id: String, today: LocalDate): DataOutcome<List<BudgetMonth>>

  suspend fun suggestion(categoriaId: String, moneda: Moneda, today: LocalDate): DataOutcome<Money?>

  /** Uses [touches] as given; the caller fills each [BudgetTouch.padreId]. */
  suspend fun notice(touches: List<BudgetTouch>, today: LocalDate): DataOutcome<BudgetNotice?>
}
