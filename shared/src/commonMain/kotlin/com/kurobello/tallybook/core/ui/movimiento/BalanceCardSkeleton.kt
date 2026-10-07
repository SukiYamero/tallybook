package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.kurobello.tallybook.core.ui.component.TallybookSkeleton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_balance
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income

@Composable
internal fun BalanceCardSkeleton(
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.home_balance),
) {
  Surface(
      modifier = modifier.clearAndSetSemantics {},
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
        TallybookSkeletonLine(
            sampleText = label,
            style = TallybookTheme.typography.balanceLabel,
        )
        TallybookSkeletonLine(
            sampleText = "$ 000.000",
            style = TallybookTheme.typography.headline,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        SummaryAmountSkeleton(
            labelSample = stringResource(Res.string.home_income),
            modifier = Modifier.fillMaxWidth(),
        )
        SummaryAmountSkeleton(
            labelSample = stringResource(Res.string.home_expenses),
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun SummaryAmountSkeleton(labelSample: String, modifier: Modifier) {
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
            TallybookSkeleton(
                modifier = Modifier.size(TallybookTheme.dimensions.movementSummaryIcon)
            )
            TallybookSkeletonLine(
                sampleText = labelSample,
                style = TallybookTheme.typography.balanceLabel,
            )
          }
        },
        amountContent = {
          TallybookSkeletonLine(
              sampleText = "$ -0.000.000,00",
              style = TallybookTheme.typography.balanceSecondaryAmount,
          )
        },
    )
  }
}
