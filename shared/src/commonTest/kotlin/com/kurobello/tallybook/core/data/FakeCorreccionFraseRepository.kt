package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory records; a failing [insertResult] stores nothing, as the real transaction would. */
internal class FakeCorreccionFraseRepository(
    var insertResult: DataOutcome<Unit> = DataOutcome.Success(Unit)
) : CorreccionFraseRepository {
  val records = MutableStateFlow<List<CorreccionFrase>>(emptyList())
  var insertCalls = 0
    private set

  override suspend fun insertAll(correcciones: List<CorreccionFrase>): DataOutcome<Unit> {
    insertCalls++
    if (insertResult is DataOutcome.Success) records.value += correcciones
    return insertResult
  }

  override fun observeSince(since: Instant): Flow<DataOutcome<List<CorreccionFrase>>> =
      records.map { all ->
        DataOutcome.Success(
            all.filter { it.createdAt >= since }.sortedByDescending(CorreccionFrase::createdAt)
        )
      }
}
