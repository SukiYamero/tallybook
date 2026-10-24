package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.map
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.updateAndFetch

// Atomics instead of locks: this fixture lives in commonTest, which also compiles for iOS.
/**
 * Deterministic [ChangeClock] that is prepared on construction and issues one stamp per millisecond
 * from [startEpochMillis], so tests can predict and count every stamp a write issues.
 */
@OptIn(ExperimentalAtomicApi::class)
class TestChangeClock(
    private val startEpochMillis: Long = DEFAULT_START_EPOCH_MILLIS,
    private val node: String = DEFAULT_NODE,
) : ChangeClock {
  private val issued = AtomicReference(emptyList<ChangeStamp>())
  private val prepares = AtomicInt(0)

  val issuedStamps: List<ChangeStamp>
    get() = issued.load()

  val prepareCalls: Int
    get() = prepares.load()

  override suspend fun prepare(
      storedMax: suspend () -> DataOutcome<ChangeStamp?>
  ): DataOutcome<Unit> {
    val outcome = storedMax()
    prepares.incrementAndFetch()
    return outcome.map {}
  }

  override fun next(): ChangeStamp =
      issued
          .updateAndFetch { previous ->
            previous +
                ChangeStamp.of(
                    epochMillis = startEpochMillis + previous.size,
                    counter = 0,
                    node = node,
                )
          }
          .last()

  companion object {
    const val DEFAULT_START_EPOCH_MILLIS: Long = 1_700_000_000_000
    const val DEFAULT_NODE: String = "00000000000000aa"
  }
}
