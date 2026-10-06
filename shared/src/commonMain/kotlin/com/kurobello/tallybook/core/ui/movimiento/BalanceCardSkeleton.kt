package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
internal fun BalanceCardSkeleton(modifier: Modifier = Modifier) {
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
            sampleText = stringResource(Res.string.home_balance),
            style = MaterialTheme.typography.labelLarge,
        )
        TallybookSkeletonLine(
            sampleText = "$ 000.000",
            style = MaterialTheme.typography.headlineMedium,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        SummaryAmountSkeleton(
            labelSample = stringResource(Res.string.home_income),
            modifier = Modifier.weight(1f),
        )
        SummaryAmountSkeleton(
            labelSample = stringResource(Res.string.home_expenses),
            modifier = Modifier.weight(1f),
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
    Column(
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space14,
                vertical = TallybookTheme.spacing.space12,
            ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        TallybookSkeleton(modifier = Modifier.size(TallybookTheme.dimensions.movementSummaryIcon))
        TallybookSkeletonLine(
            sampleText = labelSample,
            style = MaterialTheme.typography.labelMedium,
        )
      }
      TallybookSkeletonLine(
          sampleText = "$ 000.000",
          style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}
