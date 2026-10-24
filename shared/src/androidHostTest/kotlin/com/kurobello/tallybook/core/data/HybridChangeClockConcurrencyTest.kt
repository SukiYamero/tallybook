package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class HybridChangeClockConcurrencyTest {

  @Test
  fun `concurrent callers never receive the same stamp and each sees increasing stamps`() =
      runTest {
        val frozenWall =
            object : Clock {
              override fun now(): Instant = Instant.fromEpochMilliseconds(1_000)
            }
        val clock =
            HybridChangeClock(
                frozenWall,
                object : InstallationIdRepository {
                  override suspend fun nodeId() = DataOutcome.Success("0123456789abcdef")
                },
            )
        clock.prepare { DataOutcome.Success(null) }
        val executor = Executors.newFixedThreadPool(THREADS)
        val start = CountDownLatch(1)

        try {
          val futures =
              List(THREADS) {
                executor.submit<List<ChangeStamp>> {
                  start.await()
                  List(STAMPS_PER_THREAD) { clock.next() }
                }
              }
          start.countDown()
          val perThread = futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

          perThread.forEach { stamps ->
            assertTrue(stamps.zipWithNext().all { (previous, next) -> previous < next })
          }
          assertEquals(THREADS * STAMPS_PER_THREAD, perThread.flatten().toSet().size)
        } finally {
          executor.shutdownNow()
        }
      }

  private companion object {
    const val THREADS = 8
    const val STAMPS_PER_THREAD = 20_000
    const val TIMEOUT_SECONDS = 30L
  }
}
