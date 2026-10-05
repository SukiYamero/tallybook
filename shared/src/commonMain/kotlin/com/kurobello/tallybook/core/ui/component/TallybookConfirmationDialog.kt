package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.UnstyledDialog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookConfirmationDialog(
    visible: Boolean,
    copy: TallybookConfirmationDialogCopy,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
  UnstyledDialog(
      visible = visible,
      onDismissRequest = onDismiss,
      overlay = { Scrim(scrimColor = TallybookTheme.colors.interaction.scrim) },
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      DialogPanel(
          modifier =
              Modifier.padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
                  .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth),
          paneTitle = copy.title,
      ) {
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
              modifier = Modifier.padding(TallybookTheme.spacing.space20),
              verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
          ) {
            TallybookText(text = copy.title, style = TallybookTheme.typography.title)
            TallybookText(
                text = copy.message,
                color = TallybookTheme.colors.foreground.secondary,
                style = TallybookTheme.typography.body,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              TallybookTextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                TallybookText(text = copy.dismissLabel)
              }
              TallybookButton(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                TallybookText(text = copy.confirmLabel)
              }
            }
          }
        }
      }
    }
  }
}

internal data class TallybookConfirmationDialogCopy(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String,
)
