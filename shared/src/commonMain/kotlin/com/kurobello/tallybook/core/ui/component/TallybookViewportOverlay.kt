package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

@Stable
internal class TallybookViewportOverlayState {
  private var scrimOwner: Any? by mutableStateOf(null)

  val isScrimVisible: Boolean
    get() = scrimOwner != null

  fun showScrim(owner: Any) {
    scrimOwner = owner
  }

  fun hideScrim(owner: Any) {
    if (scrimOwner === owner) scrimOwner = null
  }
}

internal val LocalTallybookViewportOverlayState =
    staticCompositionLocalOf<TallybookViewportOverlayState?> { null }

@Composable
internal fun TallybookViewportOverlayHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  val overlayState = remember { TallybookViewportOverlayState() }
  val scrimAlpha by
      animateFloatAsState(
          targetValue = if (overlayState.isScrimVisible) VIEWPORT_SCRIM_ALPHA else 0f,
          animationSpec =
              if (overlayState.isScrimVisible) TallybookOverlayMotion.enterAnimationSpec
              else TallybookOverlayMotion.exitAnimationSpec,
          label = "viewport overlay scrim",
      )
  CompositionLocalProvider(LocalTallybookViewportOverlayState provides overlayState) {
    Box(modifier = modifier.fillMaxSize()) {
      content()
      Box(
          modifier =
              Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = scrimAlpha)) },
      )
    }
  }
}

private const val VIEWPORT_SCRIM_ALPHA = 0.32f
