package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalInspectionMode
import com.kurobello.tallybook.core.ui.theme.TallybookMotion
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

internal data class LoadingPresentation<T>(val state: T, val showLoading: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T, K : Any> Flow<T>.deferLoading(
    isLoading: (T) -> Boolean,
    requestKey: (T) -> K,
    delayMillis: Long = TallybookMotion.loadingFeedbackDelayMillis.toLong(),
    timeSource: TimeSource = TimeSource.Monotonic,
): Flow<LoadingPresentation<T>> = flow {
  var pendingRead: K? = null
  var startedAt: TimeMark? = null
  emitAll(
      this@deferLoading.transformLatest { state ->
        val loading = isLoading(state)
        if (loading) {
          val request = requestKey(state)
          if (request != pendingRead) {
            pendingRead = request
            startedAt = timeSource.markNow()
          }
          val remaining = delayMillis - requireNotNull(startedAt).elapsedNow().inWholeMilliseconds
          if (remaining > 0) delay(remaining)
        } else {
          pendingRead = null
          startedAt = null
        }
        emit(LoadingPresentation(state, loading))
      }
  )
}

@Composable
internal fun <T, K : Any> rememberLoadingPresentation(
    state: T,
    isLoading: (T) -> Boolean,
    requestKey: (T) -> K,
): State<LoadingPresentation<T>> {
  val inspecting = LocalInspectionMode.current
  val latest = rememberUpdatedState(state)
  val latestIsLoading = rememberUpdatedState(isLoading)
  val latestRequestKey = rememberUpdatedState(requestKey)
  val delayMillis = TallybookMotion.loadingFeedbackDelayMillis.toLong()
  return produceState(
      initialValue = LoadingPresentation(state, inspecting && isLoading(state)),
      key1 = inspecting,
  ) {
    snapshotFlow { latest.value }
        .deferLoading(
            isLoading = { latestIsLoading.value(it) },
            requestKey = { latestRequestKey.value(it) },
            delayMillis = if (inspecting) 0L else delayMillis,
        )
        .collect { value = it }
  }
}
