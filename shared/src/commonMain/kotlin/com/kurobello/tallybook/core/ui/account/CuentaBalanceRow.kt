package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.neutralCategoryTintColors
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.date.fullCivilDate
import com.kurobello.tallybook.core.ui.movimiento.movementRowAmountStyle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_credit_in_favor
import tallybook.shared.generated.resources.account_cycle_range
import tallybook.shared.generated.resources.account_cycle_spending
import tallybook.shared.generated.resources.account_debt_estimated

/**
 * One account with its balance. [credit] turns it into a credit-card row: the estimated debt, or a
 * positive balance as credit in the user's favour, plus this cycle's spending. [showsMoneda] adds
 * the currency code for accounts outside the primary currency.
 */
@Immutable
internal data class CuentaBalance(
    val cuenta: Cuenta,
    val saldo: Money,
    val credit: CreditEstimate? = null,
    val showsMoneda: Boolean = false,
)

@Composable
internal fun CuentaBalanceRow(
    balance: CuentaBalance,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
  val lines = balanceLines(balance)
  val description = cuentaBalanceDescription(balance, lines)
  val interactions = remember { MutableInteractionSource() }
  val isPressed by interactions.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          modifier
              .then(cuentaBalanceClick(onClick, onClickLabel, interactions, tapFeedback))
              .semantics(mergeDescendants = true) { contentDescription = description },
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
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
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = TallybookTheme.spacing.space14,
                    vertical = TallybookTheme.spacing.space12,
                ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CategoryAvatar(balance.cuenta.tipo.icon, neutralCategoryTintColors())
      Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
      ) {
        CuentaName(balance)
        lines.forEach { BalanceLine(it) }
      }
    }
  }
}

@Composable
private fun cuentaBalanceDescription(balance: CuentaBalance, lines: List<BalanceLine>): String {
  val cycleLabel =
      balance.credit?.cycle?.let { cycle ->
        stringResource(
            Res.string.account_cycle_range,
            fullCivilDate(cycle.start),
            fullCivilDate(cycle.end),
        )
      }
  return listOfNotNull(
          balance.cuenta.nombre,
          stringResource(balance.cuenta.tipo.labelResource),
          balance.cuenta.moneda.name.takeIf { balance.showsMoneda },
      )
      .plus(lines.flatMap { line -> listOfNotNull(line.label, line.amount) })
      .plus(listOfNotNull(cycleLabel))
      .joinToString(", ")
}

private fun cuentaBalanceClick(
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    interactions: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier =
    if (onClick == null) Modifier
    else
        Modifier.clickable(
            interactionSource = interactions,
            indication = null,
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = {
              tapFeedback.trigger()
              onClick()
            },
        )

@Composable
private fun CuentaName(balance: CuentaBalance) {
  Row(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        balance.cuenta.nombre,
        modifier = Modifier.weight(1f, fill = false),
        style = TallybookTheme.typography.cardTitle,
        maxLines = NAME_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
    if (balance.showsMoneda) {
      TallybookText(
          balance.cuenta.moneda.name,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}

/** [label] is null for a plain balance, whose amount needs no caption. */
private data class BalanceLine(val label: String?, val amount: String, val emphasized: Boolean)

@Composable
private fun balanceLines(balance: CuentaBalance): List<BalanceLine> {
  val credit = balance.credit ?: return listOf(BalanceLine(null, balance.saldo.format(), true))
  val headline =
      if (balance.saldo.minorUnits > 0) {
        BalanceLine(
            stringResource(Res.string.account_credit_in_favor),
            balance.saldo.format(),
            true,
        )
      } else {
        BalanceLine(
            stringResource(Res.string.account_debt_estimated),
            credit.deuda.format(),
            true,
        )
      }
  return listOf(
      headline,
      BalanceLine(
          stringResource(Res.string.account_cycle_spending),
          credit.cycleSpending.format(),
          false,
      ),
  )
}

@Composable
private fun BalanceLine(line: BalanceLine) {
  FlowRow(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    line.label?.let {
      TallybookText(
          it,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
    TallybookText(
        line.amount,
        color =
            if (line.emphasized) TallybookTheme.colors.foreground.primary
            else TallybookTheme.colors.foreground.secondary,
        style =
            if (line.emphasized) movementRowAmountStyle() else TallybookTheme.typography.caption,
    )
  }
}

private const val NAME_MAX_LINES = 2
