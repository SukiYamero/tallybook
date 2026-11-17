package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.MicOff
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.VoiceInputUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.VoiceNotice
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_voice_failed
import tallybook.shared.generated.resources.movimiento_voice_hint
import tallybook.shared.generated.resources.movimiento_voice_language_downloading
import tallybook.shared.generated.resources.movimiento_voice_language_unavailable
import tallybook.shared.generated.resources.movimiento_voice_listening
import tallybook.shared.generated.resources.movimiento_voice_not_heard
import tallybook.shared.generated.resources.movimiento_voice_open_settings
import tallybook.shared.generated.resources.movimiento_voice_or
import tallybook.shared.generated.resources.movimiento_voice_permission_blocked
import tallybook.shared.generated.resources.movimiento_voice_permission_denied
import tallybook.shared.generated.resources.movimiento_voice_preparing
import tallybook.shared.generated.resources.movimiento_voice_start
import tallybook.shared.generated.resources.movimiento_voice_stop
import tallybook.shared.generated.resources.movimiento_voice_title
import tallybook.shared.generated.resources.movimiento_voice_unavailable

internal const val VOICE_DICTATION_BUTTON_TAG = "movimiento_voice_button"

private val noticeMessages: Map<VoiceNotice, StringResource> =
    mapOf(
        VoiceNotice.NOT_HEARD to Res.string.movimiento_voice_not_heard,
        VoiceNotice.PERMISSION_DENIED to Res.string.movimiento_voice_permission_denied,
        VoiceNotice.PERMISSION_BLOCKED to Res.string.movimiento_voice_permission_blocked,
        VoiceNotice.LANGUAGE_UNAVAILABLE to Res.string.movimiento_voice_language_unavailable,
        VoiceNotice.LANGUAGE_DOWNLOADING to Res.string.movimiento_voice_language_downloading,
        VoiceNotice.FAILED to Res.string.movimiento_voice_failed,
    )

internal data class VoiceDictationCallbacks(
    val onStart: () -> Unit,
    val onStop: () -> Unit,
    val onOpenSettings: () -> Unit,
) {
  companion object {
    val Inert = VoiceDictationCallbacks(onStart = {}, onStop = {}, onOpenSettings = {})
  }
}

@Composable
internal fun VoiceDictationButton(
    voice: VoiceInputUi,
    enabled: Boolean,
    callbacks: VoiceDictationCallbacks,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    OrDivider()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      TallybookText(
          text = stringResource(Res.string.movimiento_voice_title),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      MicrophoneDisc(
          voice = voice,
          enabled = enabled && voice.isAvailable,
          onPress = callbacks.onStart,
          onRelease = callbacks.onStop,
      )
      TallybookText(
          text = stringResource(captionOf(voice)),
          // Announcing "listening" would let the microphone transcribe the screen reader itself.
          modifier =
              if (voice.notice != null && !voice.isListening)
                  Modifier.semantics { liveRegion = LiveRegionMode.Polite }
              else Modifier,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption.copy(textAlign = TextAlign.Center),
      )
      if (voice.notice == VoiceNotice.PERMISSION_BLOCKED && !voice.isListening) {
        TallybookTextButton(onClick = callbacks.onOpenSettings, enabled = enabled) {
          TallybookText(stringResource(Res.string.movimiento_voice_open_settings))
        }
      }
    }
  }
}

private fun captionOf(voice: VoiceInputUi): StringResource =
    when {
      !voice.isAvailable -> Res.string.movimiento_voice_unavailable
      voice.isListening && !voice.isMicReady -> Res.string.movimiento_voice_preparing
      voice.isListening -> Res.string.movimiento_voice_listening
      else -> voice.notice?.let(noticeMessages::getValue) ?: Res.string.movimiento_voice_hint
    }

/**
 * Press and hold to talk; releasing (or a cancelled touch) finishes and keeps what was heard.
 * Screen readers cannot hold a press, so their activation toggles listening instead.
 */
@Composable
private fun MicrophoneDisc(
    voice: VoiceInputUi,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
  var isHeld by remember { mutableStateOf(false) }
  val tapFeedback = rememberTallybookTapFeedback()
  val currentOnPress by rememberUpdatedState(onPress)
  val currentOnRelease by rememberUpdatedState(onRelease)
  val colors = TallybookTheme.colors
  // The disc turns green only once the microphone is open, which is when speaking starts to count.
  val (container, content) =
      when {
        voice.isListening && voice.isMicReady -> colors.brand.primary to colors.brand.onPrimary
        enabled -> colors.surface.raised to colors.brand.accent
        else -> colors.surface.deep to colors.foreground.disabled
      }
  val label =
      stringResource(
          if (voice.isListening) Res.string.movimiento_voice_stop
          else Res.string.movimiento_voice_start
      )
  TallybookSurface(
      modifier =
          Modifier.size(TallybookTheme.dimensions.voiceDictationButton)
              .testTag(VOICE_DICTATION_BUTTON_TAG)
              .holdToTalk(
                  enabled = enabled,
                  label = label,
                  onHeld = { held ->
                    isHeld = held
                    if (held) {
                      tapFeedback.trigger()
                      currentOnPress()
                    } else {
                      currentOnRelease()
                    }
                  },
                  onToggle = { if (voice.isListening) onRelease() else onPress() },
              ),
      style =
          TallybookSurfaceStyle(
              color = container,
              contentColor = content,
              shape = CircleShape,
              border = null,
          ),
      isPressed = isHeld,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Image(
        imageVector = if (voice.isAvailable) Lucide.Mic else Lucide.MicOff,
        contentDescription = null,
        modifier =
            Modifier.align(Alignment.Center).size(TallybookTheme.dimensions.voiceDictationIcon),
        colorFilter = ColorFilter.tint(content),
    )
  }
}

/**
 * [onHeld] runs with true on the first touch and false only when every finger lifts or the gesture
 * is cancelled; a screen reader's activation runs [onToggle] instead. A finger drifting off the
 * disc keeps holding, and its movement is consumed so the scrolling dialog does not take it over.
 */
private fun Modifier.holdToTalk(
    enabled: Boolean,
    label: String,
    onHeld: (Boolean) -> Unit,
    onToggle: () -> Unit,
): Modifier =
    pointerInput(enabled) {
          if (enabled) {
            awaitEachGesture {
              awaitFirstDown().consume()
              onHeld(true)
              try {
                do {
                  val event = awaitPointerEvent()
                  event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
              } finally {
                onHeld(false)
              }
            }
          }
        }
        .semantics {
          role = Role.Button
          contentDescription = label
          if (enabled) {
            onClick(label) {
              onToggle()
              true
            }
          } else {
            disabled()
          }
        }

@Composable
private fun OrDivider() {
  Row(
      modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    DividerLine(Modifier.weight(1f))
    TallybookText(
        text = stringResource(Res.string.movimiento_voice_or),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
    DividerLine(Modifier.weight(1f))
  }
}

@Composable
private fun DividerLine(modifier: Modifier) {
  Box(
      modifier =
          modifier
              .height(TallybookTheme.dimensions.borderThin)
              .background(TallybookTheme.colors.border.base)
  )
}

@Preview
@Composable
internal fun VoiceDictationButtonIdlePreview() {
  TallybookTheme(darkTheme = false) {
    VoiceDictationButton(VoiceInputUi(isAvailable = true), true, VoiceDictationCallbacks.Inert)
  }
}

@Preview
@Composable
internal fun VoiceDictationButtonListeningDarkPreview() {
  TallybookTheme(darkTheme = true) {
    VoiceDictationButton(
        VoiceInputUi(isAvailable = true, isListening = true),
        true,
        VoiceDictationCallbacks.Inert,
    )
  }
}

@Preview
@Composable
internal fun VoiceDictationButtonBlockedPreview() {
  TallybookTheme(darkTheme = false) {
    VoiceDictationButton(
        VoiceInputUi(isAvailable = true, notice = VoiceNotice.PERMISSION_BLOCKED),
        true,
        VoiceDictationCallbacks.Inert,
    )
  }
}

@Preview
@Composable
internal fun VoiceDictationButtonUnavailablePreview() {
  TallybookTheme(darkTheme = false) {
    VoiceDictationButton(VoiceInputUi(), true, VoiceDictationCallbacks.Inert)
  }
}
