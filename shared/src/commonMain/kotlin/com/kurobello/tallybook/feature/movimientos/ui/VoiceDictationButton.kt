package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
import tallybook.shared.generated.resources.movimiento_voice_discard
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

// The fade warns that letting the slide go on will cancel, before it does.
private const val SLIDE_FADE = 0.5f

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
    val onDiscard: () -> Unit,
    val onOpenSettings: () -> Unit,
) {
  companion object {
    val Inert =
        VoiceDictationCallbacks(onStart = {}, onStop = {}, onDiscard = {}, onOpenSettings = {})
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
      // Equal weights on both sides keep the disc centered while the guide fills its left.
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SlideToCancelGuide(visible = voice.canDiscard, modifier = Modifier.weight(1f))
        MicrophoneDisc(
            voice = voice,
            enabled = enabled && voice.isAvailable,
            callbacks = callbacks,
        )
        Spacer(Modifier.weight(1f))
      }
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
 * Press and hold to talk; releasing (or a cancelled touch) finishes and keeps what was heard, while
 * sliding left past the slide-to-cancel distance discards it, as in messaging apps.
 */
@Composable
private fun MicrophoneDisc(
    voice: VoiceInputUi,
    enabled: Boolean,
    callbacks: VoiceDictationCallbacks,
) {
  val tapFeedback = rememberTallybookTapFeedback()
  val hold = rememberMicrophoneHold(voice, callbacks, tapFeedback)
  val slideToCancel = TallybookTheme.dimensions.voiceSlideToCancel
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
  val discardLabel = stringResource(Res.string.movimiento_voice_discard)
  TallybookSurface(
      modifier =
          Modifier.size(TallybookTheme.dimensions.voiceDictationButton)
              .testTag(VOICE_DICTATION_BUTTON_TAG)
              .holdToTalk(enabled = enabled, cancelDistance = slideToCancel, gesture = hold)
              .dictationSemantics(enabled, voice, label, discardLabel, callbacks)
              // Drawn after the pointer input, so the disc follows the finger while touches are
              // still measured from where it rests.
              .graphicsLayer {
                translationX = hold.slide
                alpha = 1f - SLIDE_FADE * (hold.slide / -slideToCancel.toPx())
              },
      style =
          TallybookSurfaceStyle(
              color = container,
              contentColor = content,
              shape = CircleShape,
              border = null,
          ),
      isPressed = hold.isHeld,
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

/** A screen reader cannot hold, so activation toggles listening and a custom action discards. */
private fun Modifier.dictationSemantics(
    enabled: Boolean,
    voice: VoiceInputUi,
    label: String,
    discardLabel: String,
    callbacks: VoiceDictationCallbacks,
): Modifier = semantics {
  role = Role.Button
  contentDescription = label
  if (!enabled) {
    disabled()
    return@semantics
  }
  onClick(label) {
    if (voice.isListening) callbacks.onStop() else callbacks.onStart()
    true
  }
  if (voice.canDiscard) {
    customActions =
        listOf(
            CustomAccessibilityAction(discardLabel) {
              callbacks.onDiscard()
              true
            }
        )
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
