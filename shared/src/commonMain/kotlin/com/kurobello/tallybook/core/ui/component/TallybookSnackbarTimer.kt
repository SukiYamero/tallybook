package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.platform.AccessibilityManager
import com.kurobello.tallybook.core.ui.theme.TallybookMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TallybookSnackbarTimer(
    private val scope: CoroutineScope,
    private val onTimeout: (Long, Int) -> Unit,
) {
  private var generation: Pair<Long, Int>? = null
  private var timeoutJob: Job? = null

  fun update(message: TallybookSnackbarMessage?, isActive: Boolean, timeoutMillis: Long?) {
    val nextGeneration = message?.takeIf { isActive && !it.isBusy }?.let { it.token to it.revision }
    if (generation == nextGeneration) return
    timeoutJob?.cancel()
    generation = nextGeneration
    timeoutJob =
        if (nextGeneration == null || timeoutMillis == null) null
        else {
          scope.launch {
            delay(timeoutMillis)
            if (generation == nextGeneration) {
              onTimeout(nextGeneration.first, nextGeneration.second)
            }
          }
        }
  }

  fun dispose() {
    timeoutJob?.cancel()
    timeoutJob = null
    generation = null
  }
}

internal fun tallybookSnackbarTimeoutMillis(
    duration: TallybookSnackbarDuration,
    accessibilityManager: AccessibilityManager?,
): Long? {
  val baseline =
      when (duration) {
        TallybookSnackbarDuration.Short -> TallybookMotion.snackbarShortMillis.toLong()
        TallybookSnackbarDuration.Long -> TallybookMotion.snackbarLongMillis.toLong()
        TallybookSnackbarDuration.Indefinite -> return null
      }
  val recommended =
      accessibilityManager?.calculateRecommendedTimeoutMillis(
          originalTimeoutMillis = baseline,
          containsIcons = true,
          containsText = true,
          containsControls = true,
      ) ?: baseline
  return recommended.coerceAtLeast(baseline).takeUnless { it == Long.MAX_VALUE }
}
