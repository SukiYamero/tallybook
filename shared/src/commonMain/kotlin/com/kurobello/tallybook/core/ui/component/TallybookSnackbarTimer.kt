package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.platform.AccessibilityManager
import com.kurobello.tallybook.core.ui.theme.TallybookMotion
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TallybookSnackbarTimer(
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val onTimeout: (Long, Int) -> Unit,
) {
  private var window: Window? = null
  private var timeoutJob: Job? = null
  private var compactBatch = false

  fun shouldCompact(message: TallybookSnackbarMessage?, hasWaitingMessages: Boolean): Boolean =
      message != null && (compactBatch || hasWaitingMessages)

  fun update(
      message: TallybookSnackbarMessage?,
      isActive: Boolean,
      timeoutMillis: Long?,
      isCompacted: Boolean = false,
  ) {
    compactBatch = shouldCompact(message, isCompacted)
    val generation = message?.let { it.token to it.revision }
    val isVisible = isActive && message?.isBusy == false
    val current = window?.takeIf { it.generation == generation }
    if (current == null) {
      window =
          generation
              ?.takeIf { isVisible }
              ?.let { Window(it, timeoutMillis, compactBatch, timeSource.markNow()) }
      schedule(window)
      return
    }
    val compacted = compactCurrentWindow(current, timeoutMillis)
    val visibilityChanged = current.setVisible(isVisible)
    if (compacted || visibilityChanged) schedule(current)
  }

  private fun compactCurrentWindow(current: Window, timeoutMillis: Long?): Boolean {
    if (!compactBatch || current.isCompacted) return false
    current.isCompacted = true
    val previousTimeout = current.timeoutMillis
    val shortens = !current.isDelivered && previousTimeout != null
    if (shortens) current.timeoutMillis = timeoutMillis?.coerceAtMost(previousTimeout)
    return shortens
  }

  private fun schedule(current: Window?) {
    timeoutJob?.cancel()
    val remaining = current?.remainingMillis()
    timeoutJob =
        if (current == null || remaining == null) null
        else {
          scope.launch {
            delay(remaining)
            if (window === current && !current.isDelivered) {
              current.isDelivered = true
              onTimeout(current.generation.first, current.generation.second)
            }
          }
        }
  }

  fun dispose() {
    timeoutJob?.cancel()
    timeoutJob = null
    window = null
    compactBatch = false
  }

  /**
   * Deadlines are measured against accumulated on-screen time, so coverage or busy progress pauses
   * the window instead of consuming or restarting it.
   */
  private inner class Window(
      val generation: Pair<Long, Int>,
      var timeoutMillis: Long?,
      var isCompacted: Boolean,
      private var visibleSince: TimeMark?,
  ) {
    var isDelivered = false
    private var accumulatedMillis = 0L

    val isVisible: Boolean
      get() = visibleSince != null

    fun remainingMillis(): Long? =
        timeoutMillis
            ?.takeIf { isVisible && !isDelivered }
            ?.let { (it - visibleMillis()).coerceAtLeast(0) }

    private fun visibleMillis(): Long =
        accumulatedMillis + (visibleSince?.elapsedNow()?.inWholeMilliseconds?.coerceAtLeast(0) ?: 0)

    fun setVisible(visible: Boolean): Boolean {
      if (visible == isVisible) return false
      if (visible) {
        visibleSince = timeSource.markNow()
        // Callers may refuse a delivery made while their live state was already covered; a
        // generation that is still present must expire again once it is revealed.
        isDelivered = false
      } else {
        accumulatedMillis = visibleMillis()
        visibleSince = null
      }
      return true
    }
  }
}

internal fun tallybookSnackbarTimeoutMillis(
    duration: TallybookSnackbarDuration,
    accessibilityManager: AccessibilityManager?,
    isCompacted: Boolean = false,
): Long? {
  val fullBaseline =
      when (duration) {
        TallybookSnackbarDuration.Short -> TallybookMotion.snackbarShortMillis.toLong()
        TallybookSnackbarDuration.Long -> TallybookMotion.snackbarLongMillis.toLong()
        TallybookSnackbarDuration.Indefinite -> return null
      }
  val baseline = if (isCompacted) fullBaseline / 2 else fullBaseline
  val recommended =
      accessibilityManager?.calculateRecommendedTimeoutMillis(
          originalTimeoutMillis = baseline,
          containsIcons = true,
          containsText = true,
          containsControls = true,
      ) ?: baseline
  return recommended.coerceAtLeast(baseline).takeUnless { it == Long.MAX_VALUE }
}
