package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Single choice among [options], which the caller filters by tipo with the MedioPago rules. A null
 * [selected] shows every option unselected, as an undecided sentence leaves it. Options wrap
 * because four labels do not fit one row of a phone in every language.
 */
@Composable
internal fun MedioPagoControl(
    selected: MedioPago?,
    options: List<MedioPago>,
    onSelect: (MedioPago) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  FlowRow(
      modifier = modifier.selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    options.forEach { medio ->
      MedioPagoOption(
          medio = medio,
          selected = medio == selected,
          onClick = { onSelect(medio) },
          enabled = enabled,
      )
    }
  }
}

@Composable
private fun MedioPagoOption(
    medio: MedioPago,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
  val colors = TallybookTheme.colors
  val contentColor =
      when {
        !enabled -> colors.foreground.disabled
        selected -> colors.brand.secondary
        else -> colors.foreground.primary
      }
  Box(
      modifier =
          Modifier.defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .selectable(
                  selected = selected,
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  enabled = enabled,
                  role = Role.RadioButton,
                  onClick = onClick,
              ),
      contentAlignment = Alignment.Center,
  ) {
    TallybookSurface(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.choicePillVisualHeight),
        style =
            TallybookSurfaceStyle(
                color = if (selected) colors.surface.deep else colors.surface.card,
                contentColor = contentColor,
                shape = RoundedCornerShape(TallybookTheme.radii.radius12),
                border =
                    BorderStroke(
                        TallybookTheme.dimensions.borderThin,
                        if (selected) colors.brand.secondary else colors.border.subtle,
                    ),
                contentAlignment = Alignment.Center,
            ),
    ) {
      MedioPagoOptionLabel(medio, contentColor)
    }
  }
}

@Composable
private fun MedioPagoOptionLabel(medio: MedioPago, contentColor: Color) {
  Row(
      modifier =
          Modifier.padding(
              horizontal = TallybookTheme.spacing.space12,
              vertical = TallybookTheme.spacing.space4,
          ),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    MedioPagoIcon(
        medio = medio,
        modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
        contentDescription = null,
        tint = contentColor,
    )
    TallybookText(
        stringResource(medio.labelResource),
        style = TallybookTheme.typography.controlValue,
    )
  }
}

@Composable
private fun MedioPagoControlPreviewContent() {
  Column(
      modifier = Modifier.padding(TallybookTheme.spacing.space16),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    MedioPagoControl(MedioPago.CREDITO, MedioPago.entries, onSelect = {})
    MedioPagoControl(
        selected = null,
        options = MedioPago.entries.filter { it.allows(TipoMovimiento.INGRESO) },
        onSelect = {},
    )
    MedioPagoControl(MedioPago.EFECTIVO, MedioPago.entries, onSelect = {}, enabled = false)
  }
}

@Preview
@Composable
internal fun MedioPagoControlLightPreview() {
  TallybookTheme(darkTheme = false) { MedioPagoControlPreviewContent() }
}

@Preview
@Composable
internal fun MedioPagoControlDarkPreview() {
  TallybookTheme(darkTheme = true) { MedioPagoControlPreviewContent() }
}
