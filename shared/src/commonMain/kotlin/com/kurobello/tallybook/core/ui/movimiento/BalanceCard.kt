package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_balance
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income

@Composable
fun BalanceCard(
    summary: MovimientoSummary,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.home_balance),
    secondaryAmountFormatter: (Money, TipoMovimiento) -> String = { amount, _ -> amount.format() },
    footerContent: (@Composable () -> Unit)? = null,
) {
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = TallybookTheme.typography.balanceLabel,
        )
        Text(text = summary.balance.format(), style = TallybookTheme.typography.headline)
      }
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        SummaryAmount(
            label = stringResource(Res.string.home_income),
            amountText = secondaryAmountFormatter(summary.ingresos, TipoMovimiento.INGRESO),
            tipo = TipoMovimiento.INGRESO,
            modifier = Modifier.fillMaxWidth(),
        )
        SummaryAmount(
            label = stringResource(Res.string.home_expenses),
            amountText = secondaryAmountFormatter(summary.gastos, TipoMovimiento.GASTO),
            tipo = TipoMovimiento.GASTO,
            modifier = Modifier.fillMaxWidth(),
        )
      }
      footerContent?.invoke()
    }
  }
}

@Composable
private fun SummaryAmount(
    label: String,
    amountText: String,
    tipo: TipoMovimiento,
    modifier: Modifier = Modifier,
) {
  val accent = tipo.accentColor
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    BalanceSummaryAmountLayout(
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space14,
                vertical = TallybookTheme.spacing.space12,
            ),
        labelContent = {
          Row(
              horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = tipo.presentation.icon,
                contentDescription = null,
                modifier = Modifier.size(TallybookTheme.dimensions.movementSummaryIcon),
                tint = accent,
            )
            Text(text = label, color = accent, style = TallybookTheme.typography.balanceLabel)
          }
        },
        amountContent = {
          Text(
              text = amountText,
              style = TallybookTheme.typography.balanceSecondaryAmount,
              textAlign = TextAlign.End,
          )
        },
    )
  }
}

@Preview
@Composable
internal fun BalanceCardLightPreview() {
  TallybookTheme(darkTheme = false) {
    BalanceCard(summary = previewSummary, modifier = Modifier.widthIn(max = 420.dp))
  }
}

@Preview
@Composable
internal fun BalanceCardDarkPreview() {
  TallybookTheme(darkTheme = true) {
    BalanceCard(summary = previewSummary, modifier = Modifier.widthIn(max = 420.dp))
  }
}

@Suppress("MagicNumber")
private val previewSummary =
    MovimientoSummary(
        ingresos = Money(2_750_000, Moneda.COP),
        gastos = Money(1_225_000, Moneda.COP),
    )
