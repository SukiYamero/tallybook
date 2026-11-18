package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.core.ui.theme.TallybookTypeScale
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_summary_amount_missing_hint
import tallybook.shared.generated.resources.movimiento_summary_amount_review

@Composable
internal fun SummaryAmount(summary: MovimientoSummaryUi) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    val monto = summary.monto
    if (monto == null) MissingAmount() else SummaryAmountText(monto, summary.tipo)
    if (monto != null && summary.amountNeedsReview) AmountReviewHint()
  }
}

@Composable
private fun SummaryAmountText(monto: Money, tipo: TipoMovimiento) {
  TallybookText(
      text = signedMovimientoAmountText(monto, tipo),
      color =
          if (tipo == TipoMovimiento.INGRESO) TallybookTheme.colors.status.success.base
          else TallybookTheme.colors.foreground.primary,
      style = summaryAmountStyle(),
  )
}

@Composable
private fun ColumnScope.MissingAmount() {
  val message = stringResource(Res.string.movimiento_amount_not_detected)
  TallybookText(
      text = message,
      modifier =
          Modifier.semantics {
            error(message)
            liveRegion = LiveRegionMode.Polite
          },
      color = TallybookTheme.colors.status.danger.base,
      style = TallybookTheme.typography.title,
  )
  TallybookText(
      text = stringResource(Res.string.movimiento_summary_amount_missing_hint),
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.caption,
  )
}

@Composable
internal fun AmountReviewHint() {
  val hint = stringResource(Res.string.movimiento_summary_amount_review)
  Row(
      modifier =
          Modifier.semantics(mergeDescendants = true) {
            contentDescription = hint
            liveRegion = LiveRegionMode.Polite
          },
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.Top,
  ) {
    Image(
        imageVector = Lucide.TriangleAlert,
        contentDescription = null,
        modifier =
            Modifier.padding(top = TallybookTheme.spacing.space2)
                .size(TallybookTheme.dimensions.iconSmall),
        colorFilter = ColorFilter.tint(TallybookTheme.colors.status.warning),
    )
    TallybookText(
        text = hint,
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
  }
}

@Composable
@ReadOnlyComposable
private fun summaryAmountStyle(): TextStyle =
    TallybookTheme.typography.headline.copy(
        fontSize = TallybookTypeScale.size28,
        lineHeight = TallybookTypeScale.size30,
    )
