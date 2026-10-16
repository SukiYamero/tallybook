package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.UnstyledDialog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

private const val ORIGIN_CENTER_X = 0.5f
private const val ORIGIN_TOP_Y = 0f

/**
 * A modal card anchored near the top of the screen. The anchor keeps the dialog still when the
 * keyboard opens: only the height available to [content] shrinks, so [content] must tolerate a
 * bounded height. Scrim taps and Back call [onDismissRequest]; the caller decides whether that
 * closes the dialog or asks to discard first. [onHidden] runs once the exit animation has finished
 * and the content left composition.
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
    Box(
        modifier =
            Modifier.fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .imePadding()
                .padding(
                    horizontal = TallybookTheme.dimensions.screenHorizontal,
                    vertical = TallybookTheme.spacing.space16,
                ),
        contentAlignment = Alignment.TopCenter,
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
                      transformOrigin = TransformOrigin(ORIGIN_CENTER_X, ORIGIN_TOP_Y),
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
private fun TallybookModalDialogSurface(content: @Composable ColumnScope.() -> Unit) {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
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
