package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// kotlin.concurrent.atomics still needs opt-in on Kotlin 2.4.10; it is the only lock-free primitive
// in commonMain without adding atomicfu, so the opt-in stays confined to this class.
@OptIn(ExperimentalAtomicApi::class)
class HybridChangeClock(
    private val clock: Clock,
    private val installationIds: InstallationIdRepository,
) : ChangeClock {
  // (millis << COUNTER_BITS) | counter: one Long makes `last + 1` carry a counter overflow into the
  // millis component, and lets a single CAS replace both fields without a lock.
  private val last = AtomicLong(0L)

  @Volatile private var node: String? = null

  private val prepareMutex = Mutex()

  override suspend fun prepare(
      storedMax: suspend () -> DataOutcome<ChangeStamp?>
  ): DataOutcome<Unit> = prepareMutex.withLock {
    val resolvedNode =
        node
            ?: when (val outcome = installationIds.nodeId()) {
              is DataOutcome.Failure -> return outcome
              is DataOutcome.Success -> outcome.data
            }
    when (val outcome = storedMax()) {
      is DataOutcome.Failure -> return outcome
      is DataOutcome.Success -> outcome.data?.let { advanceTo(it.packed()) }
    }
    // Published last so no stamp is issued below the stored maximum on the first prepare.
    node = resolvedNode
    DataOutcome.Success(Unit)
  }

  override fun next(): ChangeStamp {
    val issuingNode =
        checkNotNull(node) { "ChangeClock.next() called before a successful prepare()" }
    val wall = clock.now().toEpochMilliseconds() shl COUNTER_BITS
    val packed = last.updateAndFetch { previous -> maxOf(wall, previous + 1) }
    return ChangeStamp.of(
        epochMillis = packed ushr COUNTER_BITS,
        counter = (packed and COUNTER_MASK).toInt(),
        node = issuingNode,
    )
  }

  private fun advanceTo(packed: Long) {
    last.updateAndFetch { previous -> maxOf(previous, packed) }
  }

  private fun ChangeStamp.packed(): Long = (epochMillis shl COUNTER_BITS) or counter.toLong()

  private companion object {
    const val COUNTER_BITS = 16
    const val COUNTER_MASK = ChangeStamp.MAX_COUNTER.toLong()
  }
}
