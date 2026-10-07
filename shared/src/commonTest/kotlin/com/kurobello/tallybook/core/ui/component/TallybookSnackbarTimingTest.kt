package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.platform.AccessibilityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TallybookSnackbarTimingTest {
  @Test
  fun nullInactiveBusyAndIndefiniteMessagesNeverExpire() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
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
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
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
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
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
  fun hidingCancelsWithoutDismissalAndRevealingRestartsTheFullWindow() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_000)
    timer.update(message(), false, 4_000)
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(emptyList(), expired)
    timer.update(message(), true, 4_000)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(emptyList(), expired)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf(1L to 0), expired)
  }

  @Test
  fun replacingTokenOrRevisionCancelsTheOldGeneration() = runTest {
    val expired = mutableListOf<Pair<Long, Int>>()
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
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
    val timer = TallybookSnackbarTimer(this) { token, revision -> expired += token to revision }
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
    val timer = TallybookSnackbarTimer(this) { _, _ -> expirations++ }
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
    assertEquals(10_000L, manager.originalTimeout)
    assertEquals(listOf(true, true, true), manager.flags)
    manager.recommendation = 1
    assertEquals(4_000L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Short, manager))
    assertEquals(10_000L, tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, null))
  }

  @Test
  fun effectivelyIndefiniteAccessibilityRecommendationsDoNotOverflowOrExpire() = runTest {
    val manager = RecordingAccessibilityManager(Long.MAX_VALUE)
    val timeout = tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Long, manager)
    assertNull(timeout)
    var expirations = 0
    val timer = TallybookSnackbarTimer(this) { _, _ -> expirations++ }
    timer.update(message(), true, timeout)
    advanceTimeBy(100_000)
    runCurrent()
    assertEquals(0, expirations)
    manager.originalTimeout = null
    assertNull(tallybookSnackbarTimeoutMillis(TallybookSnackbarDuration.Indefinite, manager))
    assertNull(manager.originalTimeout)
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
