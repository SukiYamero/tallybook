package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

internal object TallybookOverlayMotion {
  const val initialScale = 0.985f

  val enterAnimationSpec =
      tween<Float>(
          durationMillis = ENTER_DURATION_MILLIS,
          easing = LinearOutSlowInEasing,
      )
  val exitAnimationSpec =
      tween<Float>(
          durationMillis = EXIT_DURATION_MILLIS,
          easing = FastOutLinearInEasing,
      )

  private const val ENTER_DURATION_MILLIS = 200
  private const val EXIT_DURATION_MILLIS = 150
}
