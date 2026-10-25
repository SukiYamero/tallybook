package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.resolveTallybookSurfaceContainerColor
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_balance
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income

/**
 * [balance] defaults to the period's flow; Home passes the accounts' total balance instead. With
 * [action] the whole card is one button that keeps the shared press feedback.
 */
@Suppress("LongParameterList")
@Composable
fun BalanceCard(
    summary: MovimientoSummary,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.home_balance),
    secondaryAmountFormatter: (Money, TipoMovimiento) -> String = { amount, _ -> amount.format() },
    footerContent: (@Composable () -> Unit)? = null,
    balance: Money = summary.balance,
    secondaryLabels: BalanceCardLabels = BalanceCardLabels.default(),
    action: BalanceCardAction? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val shape = MaterialTheme.shapes.extraLarge
  Surface(
      modifier = modifier.then(balanceCardClick(action, shape, interactionSource, tapFeedback)),
      shape = shape,
      color =
          resolveTallybookSurfaceContainerColor(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              pressedOverlay = TallybookTheme.colors.interaction.pressedOverlay,
              isPressed = isPressed,
              hasTapFeedback = tapFeedback.isActive,
          ),
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
        Text(text = balance.format(), style = TallybookTheme.typography.headline)
      }
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        SummaryAmount(
            label = secondaryLabels.income,
            amountText = secondaryAmountFormatter(summary.ingresos, TipoMovimiento.INGRESO),
            tipo = TipoMovimiento.INGRESO,
            modifier = Modifier.fillMaxWidth(),
        )
        SummaryAmount(
            label = secondaryLabels.expenses,
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
private fun balanceCardClick(
    action: BalanceCardAction?,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier =
    if (action == null) Modifier
    else
        Modifier.clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = action.label,
                role = Role.Button,
                onClick = {
                  tapFeedback.trigger()
                  action.onClick()
                },
            )

@Immutable
data class BalanceCardLabels(val income: String, val expenses: String) {
  companion object {
    @Composable
    fun default(): BalanceCardLabels =
        BalanceCardLabels(
            income = stringResource(Res.string.home_income),
            expenses = stringResource(Res.string.home_expenses),
        )
  }
}

/** [label] is announced as what tapping the card does. */
@Immutable data class BalanceCardAction(val label: String, val onClick: () -> Unit)

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
