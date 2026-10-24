package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.platform.AccessibilityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TallybookSnackbarTimingTest {
  @Test
  fun nullInactiveBusyAndIndefiniteMessagesNeverExpire() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(null, isActive = true, timeoutMillis = 4_000)
    advanceTimeBy(20_000)
    timer.update(message(), isActive = false, timeoutMillis = 4_000)
    advanceTimeBy(20_000)
    timer.update(message().copy(isBusy = true), isActive = true, timeoutMillis = 4_000)
    advanceTimeBy(20_000)
    timer.update(message().copy(duration = TallybookSnackbarDuration.Indefinite), true, null)
    advanceTimeBy(20_000)
    runCurrent()
    assertEquals(emptyList(), expired)
  }

  @Test
  fun revealStartsAFullWindowAndTimeoutIsDeliveredOnce() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), false, 4_000)
    advanceTimeBy(3_000)
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
    timer.update(message(), true, 4_000)
    advanceTimeBy(20_000)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun renderChangesDoNotRestartTheCurrentWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(
        message().copy(message = "Localized message", actionLabel = "New label"),
        true,
        9_000,
    )
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun hidingPausesWithoutDismissalAndRevealingResumesTheRemainingWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message(), false, 4_000)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(message(), true, 4_000)
    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun repeatedCoverageAccumulatesVisibleTimeInsteadOfRestartingTheWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    repeat(3) {
      timer.update(message(), true, 4_000)
      advanceTimeBy(1_000)
      timer.update(message(), false, 4_000)
      advanceTimeBy(5_000)
    }
    timer.update(message().copy(isBusy = true), true, 4_000)
    advanceTimeBy(5_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(message(), true, 4_000)
    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun refusedTimeoutIsRetriedOnceWhenTheStillPresentGenerationIsRevealedAgain() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(4_000)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
    timer.update(message(), false, 4_000)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
    timer.update(message(), true, 4_000)
    runCurrent()
    assertEquals(listOf(1L to 0, 1L to 0), expired)
    timer.update(message(), true, 4_000)
    advanceTimeBy(20_000)
    runCurrent()
    assertEquals(listOf(1L to 0, 1L to 0), expired)
  }

  @Test
  fun generationChangeWhileHiddenDiscardsTheOldWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message(), false, 4_000)
    val next = message().copy(token = 2)
    timer.update(next, false, 4_000)
    advanceTimeBy(10_000)
    timer.update(next, true, 4_000)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(2L to 0), expired)
  }

  @Test
  fun compactionWhileHiddenResumesTheShortenedRemainderOnReveal() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 10_000)
    advanceTimeBy(2_000)
    timer.update(message(), false, 10_000)
    timer.update(message(), false, 5_000, isCompacted = true)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(message(), true, 5_000, isCompacted = true)
    advanceTimeBy(2_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun compactionBelowAccumulatedVisibleTimeExpiresImmediatelyOnReveal() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 10_000)
    advanceTimeBy(6_000)
    timer.update(message(), false, 10_000)
    timer.update(message(), false, 5_000, isCompacted = true)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(message(), true, 5_000, isCompacted = true)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun replacingTokenOrRevisionCancelsTheOldGeneration() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message().copy(revision = 1), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message().copy(token = 2, revision = 1), true, 4_000)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(2L to 1), expired)
  }

  @Test
  fun busyCancelsExpiryAndRetryRevisionGetsTheFullLongWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message().copy(isBusy = true), true, 4_000)
    advanceTimeBy(20_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(
        message().copy(revision = 1, duration = TallybookSnackbarDuration.Long),
        true,
        10_000,
    )
    advanceTimeBy(9_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 1), expired)
  }

  @Test
  fun disposalCancelsTheWindowWithoutDismissal() = runTest {
    var expirations = 0
    val timer = TallybookSnackbarTimer(this, testScheduler.timeSource) { _, _ -> expirations++ }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.dispose()
    advanceTimeBy(20_000)
    runCurrent()
    assertEquals(0, expirations)
  }

  @Test
  fun accessibilityUsesActualContentAndNeverShortensTheBaseline() {
    val manager = RecordingAccessibilityManager(18_000)
    assertEquals(18_000L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, manager))
    assertEquals(9_700L, manager.originalTimeout)
    assertEquals(listOf(true, true, true), manager.flags)
    manager.recommendation = 1
    assertEquals(3_700L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Short, manager))
    assertEquals(9_700L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, null))
  }

  @Test
  fun effectivelyIndefiniteAccessibilityRecommendationsDoNotOverflowOrExpire() = runTest {
    val manager = RecordingAccessibilityManager(Long.MAX_VALUE)
    val timeout = tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, manager)
    assertNull(timeout)
    var expirations = 0
    val timer = TallybookSnackbarTimer(this, testScheduler.timeSource) { _, _ -> expirations++ }
    timer.update(message(), true, timeout)
    advanceTimeBy(100_000)
    runCurrent()
    assertEquals(0, expirations)
    manager.originalTimeout = null
    assertNull(tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Indefinite, manager))
    assertNull(manager.originalTimeout)
  }

  @Test
  fun compactedBaselinesAreHalvedBeforeAccessibilityRecommendations() {
    assertEquals(
        1_850L,
        tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Short, null, true),
    )
    assertEquals(4_850L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, null, true))
    val manager = RecordingAccessibilityManager(7_000)
    assertEquals(
        7_000L,
        tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Short, manager, true),
    )
    assertEquals(1_850L, manager.originalTimeout)
    manager.recommendation = 0
    assertEquals(
        4_850L,
        tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, manager, true),
    )
    manager.recommendation = Long.MAX_VALUE
    assertNull(tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Short, manager, true))
    manager.originalTimeout = null
    assertNull(tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Indefinite, manager, true))
    assertNull(manager.originalTimeout)
  }

  @Test
  fun backlogShortensTheActiveDeadlineFromItsInitialPresentation() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 10_000)
    advanceTimeBy(3_000)
    timer.update(message(), true, 5_000, isCompacted = true)
    advanceTimeBy(1_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun accessibilityExtensionKeepsItsReturnedWindowAnchoredToInitialPresentation() = runTest {
    var expirations = 0
    val manager = RecordingAccessibilityManager(7_000)
    val current = message().copy(duration = TallybookSnackbarDuration.Long)
    val timer = TallybookSnackbarTimer(this, testScheduler.timeSource) { _, _ -> expirations++ }
    timer.update(current, true, tallybookSnackbarTimeoutMillis(current.duration, manager))
    advanceTimeBy(3_000)
    timer.update(
        current,
        true,
        tallybookSnackbarTimeoutMillis(current.duration, manager, true),
        true,
    )
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(0, expirations)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, expirations)
  }

  @Test
  fun repeatedPressureAndRenderChangesNeverHalveAgainOrRestartTheWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(500)
    timer.update(message(), true, 2_000, isCompacted = true)
    advanceTimeBy(500)
    timer.update(message().copy(message = "Localized"), true, 2_000, isCompacted = true)
    timer.update(message(), true, 4_000)
    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun pressureAfterTheShortenedDeadlineExpiresImmediatelyWithoutDuplicateDelivery() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message(), true, 2_000, isCompacted = true)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
    timer.update(message(), true, 2_000, isCompacted = true)
    timer.update(message(), true, 4_000)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun pressureAfterNormalExpiryNeverDeliversTheSameIdentityAgain() = runTest {
    var expirations = 0
    val timer = TallybookSnackbarTimer(this, testScheduler.timeSource) { _, _ -> expirations++ }
    timer.update(message(), true, 4_000)
    advanceTimeBy(4_000)
    runCurrent()
    assertEquals(1, expirations)
    timer.update(message(), true, 2_000, isCompacted = true)
    runCurrent()
    assertEquals(1, expirations)
    timer.dispose()
  }

  @Test
  fun compactionSurvivesBusyHiddenAndLastQueuedMessagesUntilTheQueueIsEmpty() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    assertTrue(timer.shouldCompact(message(), hasWaitingMessages = true))
    timer.update(message(), true, 2_000, isCompacted = true)
    advanceTimeBy(1_000)
    timer.update(message().copy(isBusy = true), true, 2_000, isCompacted = true)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    assertTrue(timer.shouldCompact(message(), hasWaitingMessages = false))
    val retried = message().copy(revision = 1)
    timer.update(retried, false, 2_000, isCompacted = true)
    advanceTimeBy(10_000)
    timer.update(retried, true, 2_000, isCompacted = true)
    advanceTimeBy(1_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 1), expired)
    val last = message().copy(token = 2)
    assertTrue(timer.shouldCompact(last, hasWaitingMessages = false))
    timer.update(last, true, 2_000, isCompacted = true)
    advanceTimeBy(2_000)
    runCurrent()
    assertEquals(listOf(1L to 1, 2L to 0), expired)
    timer.update(null, true, null)
    assertFalse(timer.shouldCompact(message().copy(token = 3), hasWaitingMessages = false))
    timer.update(message().copy(token = 3), true, 4_000)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(listOf(1L to 1, 2L to 0), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 1, 2L to 0, 3L to 0), expired)
  }

  @Test
  fun compactedIndefiniteAndAccessibilityExtendedMessagesRetainTheirUsableWindows() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          expired += token to revision
        }
    timer.update(message().copy(duration = TallybookSnackbarDuration.Indefinite), true, null, true)
    advanceTimeBy(10_000)
    timer.update(message(), true, null, true)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    val timed = message().copy(revision = 1)
    timer.update(timed, true, 7_000, true)
    advanceTimeBy(6_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 1), expired)
  }

  private fun message() =
      TallybookSnackbarMessage(1, 0, "Example saved", TallybookSnackbarDuration.Short, "Undo")
}

private class RecordingAccessibilityManager(var recommendation: Long) : AccessibilityManager {
  var originalTimeout: Long? = null
  var flags: List<Boolean> = emptyList()

  override fun calculateRecommendedTimeoutMillis(
      originalTimeoutMillis: Long,
      containsIcons: Boolean,
      containsText: Boolean,
      containsControls: Boolean,
  ): Long {
    originalTimeout = originalTimeoutMillis
    flags = listOf(containsIcons, containsText, containsControls)
    return recommendation
  }
}
