package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * History and Search reads over movimientos and transfers. Each emission reads both kinds in one
 * transaction and every write to either re-emits, so rows, counts and years never mix two states.
 */
interface ActividadRepository {

  /**
   * Movimientos in [moneda] and transfers with a side in it, dated inside [range]. Cross-year
   * intervals require unsigned four-digit years because SQLite compares fecha as text.
   */
  fun observeActividadRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Actividad>>>

  /** Years with a movimiento or a transfer side in [moneda], newest first. */
  fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>>

  /** Search's rows for [criteria] and the currency's years, in the same snapshot. */
  fun observeFiltered(criteria: MovimientoReadCriteria): Flow<DataOutcome<MovimientoSearchSnapshot>>
}
