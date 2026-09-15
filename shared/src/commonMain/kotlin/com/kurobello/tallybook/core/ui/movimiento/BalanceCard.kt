package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.theme.LocalTallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_balance
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income

@Composable
fun BalanceCard(summary: MovimientoSummary, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.home_balance),
            color = LocalTallybookColors.current.status.success.base,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(text = summary.balance.format(), style = MaterialTheme.typography.headlineMedium)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryAmount(
            label = stringResource(Res.string.home_income),
            amount = summary.ingresos,
            positive = true,
            modifier = Modifier.weight(1f),
        )
        SummaryAmount(
            label = stringResource(Res.string.home_expenses),
            amount = summary.gastos,
            positive = false,
            modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SummaryAmount(
    label: String,
    amount: Money,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
  val accent =
      if (positive) LocalTallybookColors.current.status.success.base
      else LocalTallybookColors.current.status.danger.base
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            imageVector = if (positive) Lucide.ArrowDownLeft else Lucide.ArrowUpRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = accent,
        )
        Text(text = label, color = accent, style = MaterialTheme.typography.labelMedium)
      }
      Text(text = amount.format(), style = MaterialTheme.typography.titleSmall)
    }
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
