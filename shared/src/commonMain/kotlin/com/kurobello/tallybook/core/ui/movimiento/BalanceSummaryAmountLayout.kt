package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun BalanceSummaryAmountLayout(
    labelContent: @Composable () -> Unit,
    amountContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
  val horizontalSpacing = TallybookTheme.spacing.space12
  val verticalSpacing = TallybookTheme.spacing.space6
  Layout(
      modifier = modifier,
      content = {
        Box { labelContent() }
        Box { amountContent() }
      },
  ) { measurables, constraints ->
    val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val label = measurables[0].measure(childConstraints)
    val amount = measurables[1].measure(childConstraints)
    val rowWidth = label.width + horizontalSpacing.roundToPx() + amount.width
    val width =
        if (constraints.hasBoundedWidth) constraints.maxWidth
        else rowWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
    val isInline = rowWidth <= width
    val amountTop = if (isInline) 0 else label.height + verticalSpacing.roundToPx()
    val desiredHeight =
        if (isInline) maxOf(label.height, amount.height) else amountTop + amount.height
    val height = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) {
      label.placeRelative(0, if (isInline) (height - label.height) / 2 else 0)
      amount.placeRelative(
          width - amount.width,
          if (isInline) (height - amount.height) / 2 else amountTop,
      )
    }
  }
}
