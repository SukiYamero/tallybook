package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource

private const val BADGE_CONTAINER_ALPHA = 0.15f

@Composable
internal fun MovimientoTypeBadge(
    tipo: TipoMovimiento,
    modifier: Modifier = Modifier,
    size: MovimientoBadgeSize = MovimientoBadgeSize.Regular,
) {
  val accent = tipo.accentColor
  MovimientoBadge(
      icon = tipo.presentation.icon,
      text = stringResource(tipo.presentation.label),
      colors =
          MovimientoBadgeColors(
              content = accent,
              container = accent.copy(alpha = BADGE_CONTAINER_ALPHA),
          ),
      size = size,
      modifier = modifier,
  )
}

@Composable
private fun TypeBadgePreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    MovimientoBadgeSize.entries.forEach { size ->
      Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
        TipoMovimiento.entries.forEach { MovimientoTypeBadge(it, size = size) }
      }
    }
  }
}

@Preview
@Composable
internal fun MovimientoTypeBadgeLightPreview() {
  TallybookTheme(darkTheme = false) { TypeBadgePreviewMatrix() }
}

@Preview
@Composable
internal fun MovimientoTypeBadgeDarkPreview() {
  TallybookTheme(darkTheme = true) { TypeBadgePreviewMatrix() }
}
