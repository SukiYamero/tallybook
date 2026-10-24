package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ChangeClockTest {

  @Test
  fun stampsAreStrictlyIncreasingAcrossWallClockShapes() = runTest {
    data class Case(
        val name: String,
        val wallMillis: List<Long>,
        val expected: List<Pair<Long, Int>>,
    )
    val cases =
        listOf(
            Case(
                name = "equal millis",
                wallMillis = listOf(1_000, 1_000, 1_000),
                expected = listOf(1_000L to 0, 1_000L to 1, 1_000L to 2),
            ),
            Case(
                name = "wall clock ahead",
                wallMillis = listOf(1_000, 1_005),
                expected = listOf(1_000L to 0, 1_005L to 0),
            ),
            Case(
                name = "backward wall clock",
                wallMillis = listOf(1_000, 400, 1_000, 1_001),
                expected = listOf(1_000L to 0, 1_000L to 1, 1_000L to 2, 1_001L to 0),
            ),
        )

    cases.forEach { case ->
      val wall = ControllableClock(case.wallMillis.first())
      val clock = preparedClock(wall)

      val stamps =
          case.wallMillis.map { millis ->
            wall.millis = millis
            clock.next()
          }

      assertEquals(
          case.expected.map { (millis, counter) -> ChangeStamp.of(millis, counter, NODE) },
          stamps,
          case.name,
      )
      assertStrictlyIncreasing(stamps, case.name)
    }
  }

  @Test
  fun counterOverflowAdvancesMillisAndResetsTheCounter() = runTest {
    val wall = ControllableClock(1_000)
    val clock = preparedClock(wall)

    val stamps = List(ChangeStamp.MAX_COUNTER + 2) { clock.next() }

    assertEquals(
        ChangeStamp.of(1_000, ChangeStamp.MAX_COUNTER, NODE),
        stamps[ChangeStamp.MAX_COUNTER],
    )
    assertEquals(ChangeStamp.of(1_001, 0, NODE), stamps.last())
    assertStrictlyIncreasing(stamps, "overflow")
    wall.millis = 1_001
    assertEquals(ChangeStamp.of(1_001, 1, NODE), clock.next())
  }

  @Test
  fun prepareWithAStoredMaxAheadOfTheWallClockContinuesAfterIt() = runTest {
    val storedMax = ChangeStamp.of(5_000, 3, "ffffffffffffffff")
    val clock = HybridChangeClock(ControllableClock(1_000), FakeInstallationIds())

    assertEquals(DataOutcome.Success(Unit), clock.prepare { DataOutcome.Success(storedMax) })

    val next = clock.next()
    assertEquals(ChangeStamp.of(5_000, 4, NODE), next)
    assertTrue(next > storedMax)
  }

  @Test
  fun preparingAgainOnlyEverAdvancesTheClock() = runTest {
    val clock = preparedClock(ControllableClock(1_000))
    clock.next()

    clock.prepare { DataOutcome.Success(ChangeStamp.of(500, 9, NODE)) }
    assertEquals(ChangeStamp.of(1_000, 1, NODE), clock.next())

    clock.prepare { DataOutcome.Success(ChangeStamp.of(9_000, 9, NODE)) }
    assertEquals(ChangeStamp.of(9_000, 10, NODE), clock.next())

    clock.prepare { DataOutcome.Success(null) }
    assertEquals(ChangeStamp.of(9_000, 11, NODE), clock.next())
  }

  @Test
  fun theNodeIsReadOnceAcrossPrepares() = runTest {
    val installationIds = FakeInstallationIds()
    val clock = HybridChangeClock(ControllableClock(1_000), installationIds)

    repeat(3) { clock.prepare { DataOutcome.Success(null) } }

    assertEquals(1, installationIds.reads)
  }

  @Test
  fun nextBeforePrepareFails() {
    val clock = HybridChangeClock(ControllableClock(1_000), FakeInstallationIds())

    assertFailsWith<IllegalStateException> { clock.next() }
  }

  @Test
  fun aFailedPrepareLeavesTheClockUnpreparedAndReportsTheError() = runTest {
    val storageError = DataError.Local(IllegalStateException("disk"))
    val failingNode =
        HybridChangeClock(
            ControllableClock(1_000),
            FakeInstallationIds(DataOutcome.Failure(storageError)),
        )
    val failingMax = HybridChangeClock(ControllableClock(1_000), FakeInstallationIds())

    assertEquals(
        DataOutcome.Failure(storageError),
        failingNode.prepare { DataOutcome.Success(null) },
    )
    assertEquals(
        DataOutcome.Failure(storageError),
        failingMax.prepare { DataOutcome.Failure(storageError) },
    )
    assertFailsWith<IllegalStateException> { failingNode.next() }
    assertFailsWith<IllegalStateException> { failingMax.next() }
    assertIs<DataOutcome.Success<Unit>>(failingMax.prepare { DataOutcome.Success(null) })
  }

  private suspend fun preparedClock(wall: Clock): HybridChangeClock =
      HybridChangeClock(wall, FakeInstallationIds()).also {
        it.prepare { DataOutcome.Success(null) }
      }

  private fun assertStrictlyIncreasing(stamps: List<ChangeStamp>, message: String) {
    stamps.zipWithNext().forEach { (previous, next) ->
      assertTrue(previous < next && previous.value < next.value, "$message: $previous !< $next")
    }
  }

  private class ControllableClock(var millis: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
  }

  private class FakeInstallationIds(
      private val outcome: DataOutcome<String> = DataOutcome.Success(NODE),
  ) : InstallationIdRepository {
    var reads = 0
      private set

    override suspend fun nodeId(): DataOutcome<String> {
      reads++
      return outcome
    }
  }

  private companion object {
    const val NODE = "0123456789abcdef"
  }
}
