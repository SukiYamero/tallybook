package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PiggyBank
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.component.LocalTallybookContentColor
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookScreenHeader
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsContent
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsUiState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.budgets_add
import tallybook.shared.generated.resources.budgets_add_description
import tallybook.shared.generated.resources.budgets_empty_body
import tallybook.shared.generated.resources.budgets_empty_title
import tallybook.shared.generated.resources.budgets_load_error
import tallybook.shared.generated.resources.budgets_period
import tallybook.shared.generated.resources.budgets_summary
import tallybook.shared.generated.resources.budgets_title
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

@Composable
internal fun BudgetsScreen(
    uiState: BudgetsUiState,
    actions: BudgetsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
  TallybookScreenFrame(modifier = modifier) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
    ) {
      item(key = "header") {
        BudgetsTopBar(
            onBack,
            Modifier.padding(
                top = TallybookTheme.spacing.space20,
                bottom = TallybookTheme.spacing.space12,
            ),
        )
      }
      budgetsContent(uiState, actions)
    }
  }
  BudgetSheetHost(uiState, actions)
}

private fun LazyListScope.budgetsContent(uiState: BudgetsUiState, actions: BudgetsActions) {
  val content = uiState.content.cachedValue
  when {
    content != null -> {
      if (uiState.content is RetainedLoadState.Failed) {
        item(key = "failed") { BudgetsFailure(actions::onRetry) }
      }
      budgetsList(content, uiState, actions)
    }
    uiState.content is RetainedLoadState.Failed ->
        item(key = "failed") { BudgetsFailure(actions::onRetry) }
    else ->
        item(key = "loading") {
          TallybookLoadingState(
              contentDescription = stringResource(Res.string.loading),
              modifier =
                  Modifier.defaultMinSize(
                      minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                  ),
          )
        }
  }
}

private fun LazyListScope.budgetsList(
    content: BudgetsContent,
    uiState: BudgetsUiState,
    actions: BudgetsActions,
) {
  item(key = "period") { BudgetsPeriod(content) }
  if (content.statuses.isEmpty()) {
    item(key = "empty") { BudgetsEmpty() }
  } else {
    item(key = "summary") { BudgetsSummaryCard(content.fineCount, content.statuses.size) }
    val categoriasById: Map<String, Categoria> = uiState.categorias.associateBy(Categoria::id)
    items(content.statuses, key = { it.presupuesto.id }) { status ->
      BudgetRow(
          status = status,
          categoria = categoriasById[status.presupuesto.categoriaId],
          primaryMoneda = uiState.primaryMoneda,
          onOpen = actions::onOpen,
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
  item(key = "add") { BudgetsAddButton(actions::onCreate) }
}

@Composable
private fun BudgetsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
  TallybookScreenHeader(
      title = stringResource(Res.string.budgets_title),
      modifier = modifier,
      navigation = {
        TallybookIconButton(
            onClick = onBack,
            icon = Lucide.ChevronLeft,
            contentDescription = stringResource(Res.string.back),
            style = TallybookIconButtonStyle.Ghost,
        )
      },
  )
}

@Composable
private fun BudgetsPeriod(content: BudgetsContent) {
  val month = stringArrayResource(Res.array.date_picker_months)[content.month.month.ordinal]
  TallybookText(
      pluralStringResource(Res.plurals.budgets_period, content.daysLeft, month, content.daysLeft),
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.bodySmall,
  )
}

@Composable
private fun BudgetsSummaryCard(fineCount: Int, total: Int) {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style = TallybookSurfaceDefaults.cardStyle(),
  ) {
    TallybookText(
        stringResource(Res.string.budgets_summary, fineCount, total),
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        style = TallybookTheme.typography.bodyEmphasis,
    )
  }
}

@Composable
private fun BudgetsAddButton(onCreate: () -> Unit) {
  val description = stringResource(Res.string.budgets_add_description)
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space8),
      horizontalArrangement = Arrangement.End,
  ) {
    TallybookButton(
        onClick = onCreate,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
      Image(
          imageVector = Lucide.Plus,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(LocalTallybookContentColor.current),
      )
      TallybookText(stringResource(Res.string.budgets_add))
    }
  }
}

@Composable
private fun BudgetsFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookText(
        stringResource(Res.string.budgets_load_error),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.body,
    )
    TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun BudgetsEmpty() {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  shape = RoundedCornerShape(TallybookTheme.radii.radius26),
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      ),
              ),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = TallybookTheme.spacing.space24,
                    vertical = TallybookTheme.spacing.space48,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
          modifier =
              Modifier.size(TallybookTheme.dimensions.emptyStateIconContainer)
                  .background(TallybookTheme.colors.surface.raised, CircleShape),
          contentAlignment = Alignment.Center,
      ) {
        Image(
            imageVector = Lucide.PiggyBank,
            contentDescription = null,
            modifier = Modifier.size(TallybookTheme.dimensions.emptyStateIcon),
            colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
        )
      }
      TallybookText(
          text = stringResource(Res.string.budgets_empty_title),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space14).semantics { heading() },
          style = TallybookTheme.typography.title.copy(textAlign = TextAlign.Center),
      )
      TallybookText(
          text = stringResource(Res.string.budgets_empty_body),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space6),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
      )
    }
  }
}
