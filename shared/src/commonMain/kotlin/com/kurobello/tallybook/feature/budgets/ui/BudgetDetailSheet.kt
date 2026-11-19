package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.chart.TallybookCategoryTrendChart
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetDetailState
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budget_amount_label
import tallybook.shared.generated.resources.budget_delete
import tallybook.shared.generated.resources.budget_delete_error
import tallybook.shared.generated.resources.budget_delete_message
import tallybook.shared.generated.resources.budget_delete_title
import tallybook.shared.generated.resources.budget_edit_amount
import tallybook.shared.generated.resources.budget_history_compared
import tallybook.shared.generated.resources.budget_history_description
import tallybook.shared.generated.resources.budget_history_error
import tallybook.shared.generated.resources.budget_history_heading
import tallybook.shared.generated.resources.budget_history_month_item
import tallybook.shared.generated.resources.budget_history_within
import tallybook.shared.generated.resources.budget_view_movimientos
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.date_picker_months_short
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

private const val ITEM_SEPARATOR = ", "

internal data class BudgetHistoryTally(val within: Int, val closed: Int)

/**
 * The last month is still running, so only the months before it can be within the monto yet; months
 * the user never recorded say nothing about the budget.
 */
internal fun List<BudgetMonth>.tally(monto: Money): BudgetHistoryTally {
  val closed = dropLast(1).filter { it.recorded }
  return BudgetHistoryTally(within = closed.count { it.spent <= monto }, closed = closed.size)
}

@Composable
internal fun BudgetDetailContent(
    detail: BudgetDetailState,
    categoria: Categoria?,
    primaryMoneda: Moneda?,
    actions: BudgetsActions,
) {
  val status = detail.status
  val name = budgetName(status, primaryMoneda)
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .padding(bottom = TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
  ) {
    BudgetDetailHeader(name, categoria, actions::onRequestDelete)
    BudgetAmount(status, actions::onEditAmount)
    BudgetHistory(detail, name, categoria) { actions.onOpen(status.presupuesto.id) }
    if (detail.deleteFailed) {
      TallybookText(
          stringResource(Res.string.budget_delete_error),
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          color = TallybookTheme.colors.status.danger.base,
      )
    }
    TallybookButton(onClick = actions::onViewMovements, modifier = Modifier.fillMaxWidth()) {
      TallybookText(stringResource(Res.string.budget_view_movimientos))
    }
  }
}

@Composable
private fun BudgetDetailHeader(name: String, categoria: Categoria?, onDelete: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (categoria != null) CategoryAvatar(categoria)
    TallybookSheetTitle(name, modifier = Modifier.weight(1f))
    TallybookIconButton(
        onClick = onDelete,
        icon = Lucide.Trash2,
        contentDescription = stringResource(Res.string.budget_delete),
        colors = TallybookIconButtonDefaults.destructiveColors(),
    )
  }
}

@Composable
private fun BudgetAmount(status: BudgetStatus, onEditAmount: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.budget_amount_label))
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          status.presupuesto.monto.format(),
          modifier = Modifier.weight(1f),
          style = TallybookTheme.typography.title,
      )
      TallybookTextButton(onClick = onEditAmount) {
        TallybookText(stringResource(Res.string.budget_edit_amount))
      }
    }
    SpentOfMonto(status)
    BudgetProgress(status)
  }
}

/**
 * Past months are compared with today's monto, since a budget keeps no earlier amounts; the caption
 * says so. A failed read offers [onRetry] instead of waiting forever.
 */
@Composable
private fun BudgetHistory(
    detail: BudgetDetailState,
    name: String,
    categoria: Categoria?,
    onRetry: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.budget_history_heading))
    val history = detail.history
    when {
      history != null -> BudgetHistoryChart(history, detail.status, name, categoria)
      detail.historyFailed -> BudgetHistoryFailure(onRetry)
      else ->
          TallybookText(
              stringResource(Res.string.loading),
              modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
              color = TallybookTheme.colors.foreground.secondary,
          )
    }
  }
}

@Composable
private fun BudgetHistoryChart(
    history: List<BudgetMonth>,
    status: BudgetStatus,
    name: String,
    categoria: Categoria?,
) {
  val monto = status.presupuesto.monto
  val shortMonths = stringArrayResource(Res.array.date_picker_months_short)
  val labels = history.map { shortMonths[it.month.month.ordinal] }
  val items = history.mapIndexed { index, month ->
    stringResource(Res.string.budget_history_month_item, labels[index], month.spent.format())
  }
  TallybookCategoryTrendChart(
      values = history.map { it.spent.minorUnits.toDouble() },
      labels = labels,
      average = monto.minorUnits.toDouble(),
      partialLast = true,
      color = categoria?.color?.tintColors()?.content ?: TallybookTheme.colors.foreground.tertiary,
      contentDescription =
          stringResource(
              Res.string.budget_history_description,
              name,
              items.joinToString(ITEM_SEPARATOR),
              monto.format(),
          ),
      modifier = Modifier.fillMaxWidth(),
  )
  TallybookText(
      stringResource(Res.string.budget_history_compared, monto.format()),
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.caption,
  )
  val tally = history.tally(monto)
  if (tally.closed > 0) {
    TallybookText(
        stringResource(Res.string.budget_history_within, tally.within, tally.closed),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun BudgetHistoryFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookText(
        stringResource(Res.string.budget_history_error),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
    TallybookTextButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
internal fun BudgetDeleteDialog(visible: Boolean, actions: BudgetsActions) {
  TallybookConfirmationDialog(
      visible = visible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.budget_delete_title),
              message = stringResource(Res.string.budget_delete_message),
              confirmLabel = stringResource(Res.string.budget_delete),
              dismissLabel = stringResource(Res.string.cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
}
