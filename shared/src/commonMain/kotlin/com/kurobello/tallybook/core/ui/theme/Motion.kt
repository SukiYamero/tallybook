package com.kurobello.tallybook.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

internal object TallybookMotion {
  const val tapFeedbackMillis = 100
  const val navigationFadeOutMillis = 90
  const val fadeMillis = 200
  const val navigationFadeInMillis = 220
  const val popInMillis = 220
  const val pushInMillis = 280
  const val sheetUpMillis = 320
  const val sheetDownMillis = sheetUpMillis / 2
  const val loadingRotationMillis = 1_000

  val iosEasing = CubicBezierEasing(IOS_EASING_X1, IOS_EASING_Y1, 0f, 1f)

  private const val IOS_EASING_X1 = 0.32f
  private const val IOS_EASING_Y1 = 0.72f
}
