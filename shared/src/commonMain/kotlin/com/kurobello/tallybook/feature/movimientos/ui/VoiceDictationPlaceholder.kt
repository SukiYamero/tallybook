package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_voice_coming_soon
import tallybook.shared.generated.resources.movimiento_voice_or
import tallybook.shared.generated.resources.movimiento_voice_title

/** Non-functional entry point for dictation, announced as a disabled button until it ships. */
@Composable
internal fun VoiceDictationPlaceholder(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    OrDivider()
    Column(
        modifier =
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
              role = Role.Button
              disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      TallybookText(
          text = stringResource(Res.string.movimiento_voice_title),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      MicrophoneDisc()
      TallybookText(
          text = stringResource(Res.string.movimiento_voice_coming_soon),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
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

@Composable
private fun MicrophoneDisc() {
  Box(
      modifier =
          Modifier.size(TallybookTheme.dimensions.voiceDictationButton)
              .clip(CircleShape)
              .background(TallybookTheme.colors.surface.deep),
      contentAlignment = Alignment.Center,
  ) {
    Image(
        imageVector = Lucide.Mic,
        contentDescription = null,
        modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
        colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
    )
  }
}

@Preview
@Composable
internal fun VoiceDictationPlaceholderLightPreview() {
  TallybookTheme(darkTheme = false) { VoiceDictationPlaceholder() }
}

@Preview
@Composable
internal fun VoiceDictationPlaceholderDarkPreview() {
  TallybookTheme(darkTheme = true) { VoiceDictationPlaceholder() }
}
