package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.feature.movimientos.viewmodel.VoiceInputUi

internal interface HoldToTalkGesture {
  fun onPress()

  /** How far left of its first touch the finger is: zero or less, short of the cancel distance. */
  fun onSlide(offset: Float)

  /** Replaces [onRelease] once the finger has slid the whole cancel distance left. */
  fun onSlideCancel()

  /** Every finger lifted or the touch was cancelled. */
  fun onRelease()
}

@Composable
internal fun rememberMicrophoneHold(
    voice: VoiceInputUi,
    callbacks: VoiceDictationCallbacks,
    tapFeedback: TallybookTapFeedback,
): MicrophoneHold {
  val haptics = LocalHapticFeedback.current
  val hold = remember(tapFeedback, haptics) { MicrophoneHold(tapFeedback, haptics) }
  SideEffect {
    hold.canDiscard = voice.canDiscard
    hold.callbacks = callbacks
  }
  return hold
}

/** The disc's touch state; the gesture outlives compositions, so it reads the latest callbacks. */
internal class MicrophoneHold(
    private val tapFeedback: TallybookTapFeedback,
    private val haptics: HapticFeedback,
) : HoldToTalkGesture {
  var isHeld by mutableStateOf(false)
    private set

  var slide by mutableFloatStateOf(0f)
    private set

  var canDiscard = false
  var callbacks = VoiceDictationCallbacks.Inert

  override fun onPress() {
    isHeld = true
    tapFeedback.trigger()
    callbacks.onStart()
  }

  // A session that ends under a still-held finger snaps the disc back instead of leaving it adrift.
  override fun onSlide(offset: Float) {
    slide = if (canDiscard) offset else 0f
  }

  override fun onSlideCancel() {
    isHeld = false
    slide = 0f
    if (canDiscard) {
      haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
      callbacks.onDiscard()
    }
  }

  override fun onRelease() {
    isHeld = false
    slide = 0f
    callbacks.onStop()
  }
}

/**
 * A finger drifting off the disc keeps holding, and its movement is consumed so the scrolling
 * dialog does not take it over; only a slide [cancelDistance] to the left cancels.
 */
internal fun Modifier.holdToTalk(
    enabled: Boolean,
    cancelDistance: Dp,
    gesture: HoldToTalkGesture,
): Modifier =
    pointerInput(enabled, cancelDistance, gesture) {
      if (!enabled) return@pointerInput
      val cancelAt = cancelDistance.toPx()
      awaitEachGesture {
        val down = awaitFirstDown().also { it.consume() }
        gesture.onPress()
        var cancelled = false
        try {
          do {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            val finger = event.changes.firstOrNull { it.id == down.id }
            if (!cancelled && finger != null) {
              val offset = (finger.position.x - down.position.x).coerceIn(-cancelAt, 0f)
              cancelled = offset <= -cancelAt
              if (cancelled) gesture.onSlideCancel() else gesture.onSlide(offset)
            }
          } while (event.changes.any { it.pressed })
        } finally {
          if (!cancelled) gesture.onRelease()
        }
      }
    }
