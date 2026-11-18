package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

interface CorreccionFraseRepository {

  suspend fun insertAll(correcciones: List<CorreccionFrase>): DataOutcome<Unit>

  /** Live records created at or after [since], newest first. */
  fun observeSince(since: Instant): Flow<DataOutcome<List<CorreccionFrase>>>
}
