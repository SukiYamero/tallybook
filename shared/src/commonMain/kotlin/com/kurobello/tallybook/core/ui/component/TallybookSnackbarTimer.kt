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
    val nextGeneration = message?.takeIf { isActive && !it.isBusy }?.let { it.token to it.revision }
    val current = window
    if (current?.generation == nextGeneration) {
      current?.let { compactCurrentWindow(it, timeoutMillis) }
      return
    }
    window = nextGeneration?.let { Window(it, timeSource.markNow(), timeoutMillis, compactBatch) }
    schedule(window)
  }

  private fun compactCurrentWindow(current: Window, timeoutMillis: Long?) {
    if (!compactBatch || current.isCompacted) return
    current.isCompacted = true
    val previousTimeout = current.timeoutMillis
    if (current.isDelivered || previousTimeout == null) return
    current.timeoutMillis = timeoutMillis?.coerceAtMost(previousTimeout)
    schedule(current)
  }

  private fun schedule(current: Window?) {
    timeoutJob?.cancel()
    val timeout = current?.timeoutMillis
    timeoutJob =
        if (current == null || timeout == null) null
        else {
          scope.launch {
            val elapsed = current.startedAt.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
            delay((timeout - elapsed).coerceAtLeast(0))
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

  private class Window(
      val generation: Pair<Long, Int>,
      val startedAt: TimeMark,
      var timeoutMillis: Long?,
      var isCompacted: Boolean,
  ) {
    var isDelivered = false
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
