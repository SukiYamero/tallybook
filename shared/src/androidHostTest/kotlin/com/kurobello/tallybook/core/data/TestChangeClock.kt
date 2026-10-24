package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.map

/**
 * Deterministic [ChangeClock] that is prepared on construction and issues one stamp per millisecond
 * from [startEpochMillis], so tests can predict and count every stamp a write issues.
 */
class TestChangeClock(
    private val startEpochMillis: Long = DEFAULT_START_EPOCH_MILLIS,
    private val node: String = DEFAULT_NODE,
) : ChangeClock {
  private val issued = mutableListOf<ChangeStamp>()

  val issuedStamps: List<ChangeStamp>
    @Synchronized get() = issued.toList()

  var prepareCalls: Int = 0
    @Synchronized get
    private set

  override suspend fun prepare(
      storedMax: suspend () -> DataOutcome<ChangeStamp?>
  ): DataOutcome<Unit> {
    val outcome = storedMax()
    synchronized(this) { prepareCalls++ }
    return outcome.map {}
  }

  @Synchronized
  override fun next(): ChangeStamp =
      ChangeStamp.of(epochMillis = startEpochMillis + issued.size, counter = 0, node = node).also {
        issued += it
      }

  companion object {
    const val DEFAULT_START_EPOCH_MILLIS: Long = 1_700_000_000_000
    const val DEFAULT_NODE: String = "00000000000000aa"
  }
}
