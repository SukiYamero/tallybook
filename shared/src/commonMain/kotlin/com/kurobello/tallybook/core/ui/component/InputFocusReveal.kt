package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

internal expect val needsManualInputReveal: Boolean

@Composable
internal fun rememberScrollableInputViewport(): ScrollableInputViewport = remember {
  ScrollableInputViewport()
}

internal class ScrollableInputViewport {
  var boundsInRoot: Rect = Rect.Zero
  var rootBottom: Float = 0f
}

internal fun Modifier.captureScrollableInputViewport(
    viewport: ScrollableInputViewport,
): Modifier = onGloballyPositioned {
  viewport.boundsInRoot = it.boundsInRoot()
  viewport.rootBottom = it.findRootCoordinates().size.height.toFloat()
}

@Composable
internal fun Modifier.centerFocusedInputIn(
    scrollState: ScrollState,
    viewport: ScrollableInputViewport,
): Modifier {
  if (!needsManualInputReveal) return this

  val density = LocalDensity.current
  val imeInsets = WindowInsets.ime
  val inputGeometry = remember { FocusedInputGeometry() }
  var isFocused by remember { mutableStateOf(false) }

  LaunchedEffect(isFocused, density) {
    if (!isFocused) return@LaunchedEffect

    snapshotFlow { imeInsets.getBottom(density) }
        .filter { it > 0 }
        .collectLatest { imeBottom ->
          withFrameNanos {}
          val inputBounds = inputGeometry.boundsInRoot ?: return@collectLatest
          val viewportBounds = viewport.boundsInRoot
          val visibleViewportBottom =
              maxOf(
                  viewportBounds.top,
                  visibleViewportBottom(
                      viewportBottom = viewportBounds.bottom,
                      rootBottom = viewport.rootBottom,
                      imeBottom = imeBottom,
                  ),
              )
          val target =
              centeredScrollTarget(
                  currentScroll = scrollState.value,
                  maxScroll = scrollState.maxValue,
                  inputCenter = inputBounds.center.y,
                  visibleViewportCenter = (viewportBounds.top + visibleViewportBottom) / 2,
              )
          if (target != scrollState.value) scrollState.animateScrollTo(target)
        }
  }

  return onGloballyPositioned { inputGeometry.boundsInRoot = it.boundsInRoot() }
      .onFocusChanged { isFocused = it.hasFocus }
}

internal fun centeredScrollTarget(
    currentScroll: Int,
    maxScroll: Int,
    inputCenter: Float,
    visibleViewportCenter: Float,
): Int = (currentScroll + inputCenter - visibleViewportCenter).roundToInt().coerceIn(0, maxScroll)

internal fun visibleViewportBottom(
    viewportBottom: Float,
    rootBottom: Float,
    imeBottom: Int,
): Float = minOf(viewportBottom, rootBottom - imeBottom)

private class FocusedInputGeometry {
  var boundsInRoot: Rect? = null
}
