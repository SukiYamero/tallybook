package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadge
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeColors
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeSize
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Composable
internal fun MovimientoDateBadge(
    fecha: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
    size: MovimientoBadgeSize = MovimientoBadgeSize.Regular,
) {
  MovimientoBadge(
      icon = Lucide.Calendar,
      text = relativeDateLabel(fecha, today),
      colors =
          MovimientoBadgeColors(
              content = TallybookTheme.colors.foreground.secondary,
              container = TallybookTheme.colors.surface.deep,
          ),
      size = size,
      modifier = modifier,
  )
}

@Composable
private fun DateBadgePreviewMatrix() {
  val today = MovimientoEntrySamples.today
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    MovimientoBadgeSize.entries.forEach { size ->
      FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
        MovimientoDateBadge(today, today, size = size)
        MovimientoDateBadge(MovimientoEntrySamples.yesterday, today, size = size)
        MovimientoDateBadge(today.minus(1, DateTimeUnit.YEAR), today, size = size)
        MovimientoDateBadge(today.minus(2, DateTimeUnit.MONTH), today, size = size)
      }
    }
  }
}

@Preview
@Composable
internal fun MovimientoDateBadgeLightPreview() {
  TallybookTheme(darkTheme = false) { DateBadgePreviewMatrix() }
}

@Preview
@Composable
internal fun MovimientoDateBadgeDarkPreview() {
  TallybookTheme(darkTheme = true) { DateBadgePreviewMatrix() }
}
