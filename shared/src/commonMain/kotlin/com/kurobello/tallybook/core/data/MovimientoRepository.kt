package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import kotlinx.coroutines.flow.Flow

// Typed lifecycle operations preserve one compatible contract for mutations and their observations.
@Suppress("TooManyFunctions")
interface MovimientoRepository {

  fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>>

  /**
   * Cross-year intervals require unsigned four-digit years because SQLite compares fecha as text.
   */
  fun observeRange(moneda: Moneda, range: MovimientoDateRange): Flow<DataOutcome<List<Movimiento>>>

  /**
   * Year metadata validates the canonical ISO year, without deserializing the remaining row fields.
   */
  fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>>

  /** Currency years cover all history and are read in the same transaction as structural rows. */
  fun observeFiltered(criteria: MovimientoReadCriteria): Flow<DataOutcome<MovimientoSearchSnapshot>>

  /** Newest-first titles with their category, the training data for category suggestions. */
  fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>>

  suspend fun insert(movimiento: Movimiento): DataOutcome<Unit>

  suspend fun getAll(): DataOutcome<List<Movimiento>>

  suspend fun getById(id: String): DataOutcome<Movimiento?>

  suspend fun update(id: String, changes: MovimientoChanges): DataOutcome<MovimientoUpdateResult>

  suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?>

  suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<MovimientoRestoreResult>

  suspend fun delete(id: String): DataOutcome<Unit>
}
