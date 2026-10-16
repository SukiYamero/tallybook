package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.TallybookSkeleton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonCard
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun MovimientoRowSkeleton(modifier: Modifier = Modifier) {
  TallybookSkeletonCard(
      modifier = modifier,
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      )
              ),
  ) {
    Row(
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space14,
                vertical = TallybookTheme.spacing.space12,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookSkeleton(
          modifier = Modifier.size(TallybookTheme.spacing.space40),
          shape = CircleShape,
      )
      Column(modifier = Modifier.weight(1f)) {
        TallybookSkeletonLine(
            sampleText = "Movement",
            style = TallybookTheme.typography.cardTitle,
        )
        TallybookSkeletonLine(
            sampleText = "Movement details",
            style = TallybookTheme.typography.caption,
        )
        TallybookSkeletonLine(
            sampleText = "$ 000.000",
            modifier = Modifier.padding(top = TallybookTheme.spacing.space4),
            style = movementRowAmountStyle(),
        )
      }
    }
  }
}
