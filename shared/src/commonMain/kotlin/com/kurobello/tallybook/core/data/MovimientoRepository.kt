package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.coroutines.flow.Flow

// Typed lifecycle operations preserve one compatible contract for mutations and their observations.
@Suppress("TooManyFunctions")
interface MovimientoRepository {

  fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>>

  /** Ingresos and gastos in [moneda] dated inside [range], e.g. Home's current month. */
  fun observeSummary(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<MovimientoSummary>>

  /**
   * Cross-year intervals require unsigned four-digit years because SQLite compares fecha as text.
   */
  fun observeRange(moneda: Moneda, range: MovimientoDateRange): Flow<DataOutcome<List<Movimiento>>>

  /**
   * Year metadata validates the canonical ISO year, without deserializing the remaining row fields.
   */
  fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>>

  /** Newest-first titles with their category, the training data for category suggestions. */
  fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>>

  suspend fun insert(movimiento: Movimiento): DataOutcome<Unit>

  suspend fun getAll(): DataOutcome<List<Movimiento>>

  suspend fun getById(id: String): DataOutcome<Movimiento?>

  suspend fun update(id: String, changes: MovimientoChanges): DataOutcome<MovimientoUpdateResult>

  suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?>

  /**
   * [RestoreResult.Conflict] when the id holds other content or the cuenta no longer takes the
   * snapshot's medio or currency; an archived cuenta still takes it back.
   */
  suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult>

  suspend fun delete(id: String): DataOutcome<Unit>

  /**
   * The medio and live, active cuenta a new [tipo] movimiento starts with: the last used pair, then
   * the [primary] cash cuenta, then any cuenta in [primary], then any cuenta, restricted to [medio]
   * when given. Null only when no such cuenta is compatible with [medio]; a [medio] [tipo] does not
   * allow is invalid input.
   */
  suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago? = null,
  ): DataOutcome<CuentaSelection?>
}
