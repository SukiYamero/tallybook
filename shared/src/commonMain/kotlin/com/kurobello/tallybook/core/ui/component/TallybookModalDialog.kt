package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.UnstyledDialog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

private const val ORIGIN_CENTER = 0.5f

/**
 * A modal card centered vertically when it opens. That top edge is kept while the content grows, so
 * the card extends downward instead of jumping; only the keyboard or the screen edge moves it up,
 * by the minimum needed, and it returns once they leave. Height growth animates, and the height
 * left above the keyboard bounds [content], which must tolerate that. Scrim taps and Back call
 * [onDismissRequest]; the caller decides whether that closes the dialog or asks to discard first.
 * [onHidden] runs once the exit animation has finished and the content left composition.
 */
@Composable
@Suppress("LongParameterList")
internal fun TallybookModalDialog(
    visible: Boolean,
    paneTitle: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onHidden: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
  val currentOnHidden by rememberUpdatedState(onHidden)
  UnstyledDialog(
      visible = visible,
      onDismissRequest = onDismissRequest,
      overlay = { Scrim(scrimColor = TallybookTheme.colors.interaction.scrim) },
  ) {
    DialogVerticalPlacementLayout(
        modifier =
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
    ) {
      DialogPanel(
          modifier =
              modifier
                  .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                  .fillMaxWidth(),
          paneTitle = paneTitle,
          enter =
              fadeIn(TallybookOverlayMotion.enterAnimationSpec) +
                  scaleIn(
                      animationSpec = TallybookOverlayMotion.enterAnimationSpec,
                      initialScale = TallybookOverlayMotion.initialScale,
                      transformOrigin = TransformOrigin(ORIGIN_CENTER, ORIGIN_CENTER),
                  ),
          exit = fadeOut(TallybookOverlayMotion.exitAnimationSpec),
      ) {
        DisposableEffect(Unit) { onDispose { currentOnHidden() } }
        TallybookModalDialogSurface(content)
      }
    }
  }
}

@Composable
private fun DialogVerticalPlacementLayout(modifier: Modifier, content: @Composable () -> Unit) {
  val staticInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
  val ime = WindowInsets.ime
  val verticalMargin = TallybookTheme.spacing.space16
  val placement = remember { DialogVerticalPlacement() }
  Layout(content = content, modifier = modifier) { measurables, constraints ->
    val margin = verticalMargin.roundToPx()
    val restBand =
        DialogBand(
            top = staticInsets.getTop(this) + margin,
            bottom = constraints.maxHeight - staticInsets.getBottom(this) - margin,
        )
    val currentBand =
        restBand.copy(
            bottom = minOf(restBand.bottom, constraints.maxHeight - ime.getBottom(this) - margin)
        )
    // The panel emits nothing once its exit animation has finished.
    val panel =
        measurables
            .firstOrNull()
            ?.measure(constraints.copy(minWidth = 0, minHeight = 0, maxHeight = currentBand.height))
    layout(constraints.maxWidth, constraints.maxHeight) {
      panel?.place(
          x = (constraints.maxWidth - panel.width) / 2,
          y = placement.top(restBand, currentBand, panel.height),
      )
    }
  }
}

@Composable
private fun TallybookModalDialogSurface(content: @Composable ColumnScope.() -> Unit) {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth().animateContentSize(),
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.card,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius20),
              border = null,
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
        content = content,
    )
  }
}
