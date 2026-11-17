package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_voice_slide_to_cancel

private const val GUIDE_CHEVRONS = 3
private const val GUIDE_CYCLE_MILLIS = 1_200
private const val GUIDE_DIM_ALPHA = 0.3f

/**
 * Chevrons lighting up one after another toward the left, beside the label, show where the held
 * microphone can be slid to cancel. Screen readers skip it: they cancel with the disc's action.
 */
@Composable
internal fun SlideToCancelGuide(visible: Boolean, modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
      val color = TallybookTheme.colors.foreground.tertiary
      Row(
          modifier =
              Modifier.padding(end = TallybookTheme.spacing.space8).semantics {
                hideFromAccessibility()
              },
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        TallybookText(
            text = stringResource(Res.string.movimiento_voice_slide_to_cancel),
            modifier = Modifier.weight(1f, fill = false),
            color = color,
            style = TallybookTheme.typography.caption.copy(textAlign = TextAlign.End),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        GuideChevrons(color)
      }
    }
  }
}

@Composable
private fun GuideChevrons(color: Color) {
  val wave =
      rememberInfiniteTransition(label = "slide_to_cancel_wave")
          .animateFloat(
              initialValue = 0f,
              targetValue = GUIDE_CHEVRONS.toFloat(),
              animationSpec = infiniteRepeatable(tween(GUIDE_CYCLE_MILLIS, easing = LinearEasing)),
              label = "slide_to_cancel_wave_position",
          )
  Row {
    repeat(GUIDE_CHEVRONS) { index ->
      Image(
          imageVector = Lucide.ChevronLeft,
          contentDescription = null,
          modifier =
              Modifier.size(TallybookTheme.dimensions.voiceSlideGuideChevron).graphicsLayer {
                alpha = chevronAlpha(wave, GUIDE_CHEVRONS - 1 - index)
              },
          colorFilter = ColorFilter.tint(color),
      )
    }
  }
}

/** [stepFromDisc] counts from the chevron nearest the microphone, which lights first. */
private fun chevronAlpha(wave: State<Float>, stepFromDisc: Int): Float =
    if (wave.value.toInt() == stepFromDisc) 1f else GUIDE_DIM_ALPHA
