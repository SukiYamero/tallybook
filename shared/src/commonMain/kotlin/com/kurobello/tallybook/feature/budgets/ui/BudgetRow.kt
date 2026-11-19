package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.chart.TallybookChartTheme
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budgets_line_left
import tallybook.shared.generated.resources.budgets_line_over
import tallybook.shared.generated.resources.budgets_line_pace
import tallybook.shared.generated.resources.budgets_line_per_day
import tallybook.shared.generated.resources.budgets_line_reached
import tallybook.shared.generated.resources.budgets_name_with_moneda
import tallybook.shared.generated.resources.budgets_open
import tallybook.shared.generated.resources.budgets_spent_of

/** What a budget's supporting line says, chosen by its state. */
internal sealed interface BudgetLine {
  data class Over(val excess: Money) : BudgetLine

  data class Left(val remaining: Money) : BudgetLine

  data object Reached : BudgetLine

  data class Pace(val day: Int) : BudgetLine

  data class PerDay(val remaining: Money, val perDay: Money) : BudgetLine
}

internal fun BudgetStatus.line(): BudgetLine {
  val pace = paceDay
  val daily = perDay
  return when {
    state == BudgetState.OVER -> BudgetLine.Over(-remaining)
    state == BudgetState.PACE && pace != null -> BudgetLine.Pace(pace)
    state == BudgetState.FINE && daily != null -> BudgetLine.PerDay(remaining, daily)
    remaining.minorUnits > 0 -> BudgetLine.Left(remaining)
    else -> BudgetLine.Reached
  }
}

@Composable
internal fun BudgetLine.text(): String =
    when (this) {
      is BudgetLine.Over -> stringResource(Res.string.budgets_line_over, excess.format())
      is BudgetLine.Left -> stringResource(Res.string.budgets_line_left, remaining.format())
      BudgetLine.Reached -> stringResource(Res.string.budgets_line_reached)
      is BudgetLine.Pace -> stringResource(Res.string.budgets_line_pace, day)
      is BudgetLine.PerDay ->
          stringResource(Res.string.budgets_line_per_day, remaining.format(), perDay.format())
    }

/** The category name, with the currency code when the budget is outside [primaryMoneda]. */
@Composable
internal fun budgetName(status: BudgetStatus, primaryMoneda: Moneda?): String {
  val moneda = status.presupuesto.monto.moneda
  return if (primaryMoneda == null || moneda == primaryMoneda) status.categoriaNombre
  else stringResource(Res.string.budgets_name_with_moneda, status.categoriaNombre, moneda.name)
}

@Composable
internal fun BudgetRow(
    status: BudgetStatus,
    categoria: Categoria?,
    primaryMoneda: Moneda?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          modifier
              .defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClickLabel = stringResource(Res.string.budgets_open),
                  onClick = {
                    tapFeedback.trigger()
                    onOpen(status.presupuesto.id)
                  },
              ),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      )
              ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier =
            Modifier.padding(
                    horizontal = TallybookTheme.spacing.space14,
                    vertical = TallybookTheme.spacing.space12,
                )
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.Top,
    ) {
      if (categoria != null) CategoryAvatar(categoria)
      BudgetSummary(status, budgetName(status, primaryMoneda), Modifier.weight(1f))
    }
  }
}

@Composable
private fun BudgetSummary(status: BudgetStatus, name: String, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          name,
          modifier = Modifier.weight(1f),
          style = TallybookTheme.typography.bodyEmphasis,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      SpentOfMonto(status)
    }
    BudgetProgress(status)
  }
}

@Composable
internal fun SpentOfMonto(status: BudgetStatus, modifier: Modifier = Modifier) {
  TallybookText(
      stringResource(
          Res.string.budgets_spent_of,
          status.spent.format(),
          status.presupuesto.monto.format(),
      ),
      modifier = modifier,
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.caption,
  )
}

/** The progress bar colored by state, and the line that says what it means. */
@Composable
internal fun BudgetProgress(status: BudgetStatus, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    BudgetProgressBar(status.spentFraction(), status.state.barColor())
    TallybookText(
        status.line().text(),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
  }
}

/** Overspending fills the bar; the line under it says by how much. */
private fun BudgetStatus.spentFraction(): Float =
    (spent.minorUnits.toDouble() / presupuesto.monto.minorUnits).toFloat().coerceIn(0f, 1f)

@Composable
private fun BudgetState.barColor(): Color =
    when (this) {
      BudgetState.OVER -> TallybookTheme.colors.status.danger.base
      BudgetState.NEAR,
      BudgetState.PACE -> TallybookTheme.colors.status.warning
      BudgetState.FINE -> TallybookTheme.colors.foreground.tertiary
    }

@Composable
private fun BudgetProgressBar(fraction: Float, color: Color) {
  Box(
      Modifier.fillMaxWidth()
          .height(TallybookTheme.dimensions.chartDistributionBarHeight)
          .background(color = TallybookChartTheme.colors.track, shape = CircleShape)
  ) {
    if (fraction > 0f) {
      Box(
          Modifier.fillMaxWidth(fraction)
              .height(TallybookTheme.dimensions.chartDistributionBarHeight)
              .background(color = color, shape = CircleShape)
      )
    }
  }
}
