package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.account.CuentaBalance
import com.kurobello.tallybook.core.ui.account.CuentaBalanceRow
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookScreenHeader
import com.kurobello.tallybook.core.ui.component.TallybookSearchEmptyState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsActions
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsContent
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.accounts_archived
import tallybook.shared.generated.resources.accounts_archived_hide
import tallybook.shared.generated.resources.accounts_archived_show
import tallybook.shared.generated.resources.accounts_edit
import tallybook.shared.generated.resources.accounts_load_error
import tallybook.shared.generated.resources.accounts_new
import tallybook.shared.generated.resources.accounts_search
import tallybook.shared.generated.resources.accounts_search_empty
import tallybook.shared.generated.resources.accounts_title
import tallybook.shared.generated.resources.accounts_totals
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.search_clear_query

@Suppress("LongParameterList")
@Composable
internal fun AccountsScreen(
    uiState: AccountsUiState,
    actions: AccountsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    canTransfer: Boolean = false,
    trailingContent: LazyListScope.() -> Unit = {},
) {
  TallybookScreenFrame(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      AccountsTopBar(onBack = onBack, onCreate = actions::onCreate)
      TallybookSearchField(
          query = uiState.query,
          onQueryChange = actions::onSearchChanged,
          placeholder = stringResource(Res.string.accounts_search),
          clearContentDescription = stringResource(Res.string.search_clear_query),
          modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space32),
          onClear = { actions.onSearchChanged("") },
      )
      LazyColumn(
          // The outer padding keeps scrolled rows from clipping flush under the search field; the
          // content padding completes the resting gap.
          modifier =
              Modifier.weight(1f).fillMaxWidth().padding(top = TallybookTheme.spacing.space8),
          contentPadding =
              PaddingValues(
                  top = TallybookTheme.spacing.space8,
                  bottom = TallybookTheme.spacing.space16,
              ),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
      ) {
        accountsContent(uiState, actions)
        if (uiState.content is AccountsContent.Ready && uiState.query.isBlank()) trailingContent()
      }
    }
  }
  AccountSheetHost(uiState.sheet, actions, canTransfer)
}

private fun LazyListScope.accountsContent(uiState: AccountsUiState, actions: AccountsActions) {
  when (val content = uiState.content) {
    AccountsContent.Loading ->
        item(key = "loading") {
          TallybookLoadingState(
              contentDescription = stringResource(Res.string.loading),
              modifier =
                  Modifier.defaultMinSize(
                      minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                  ),
          )
        }
    AccountsContent.Failed -> item(key = "failed") { AccountsFailure(actions::onRetry) }
    is AccountsContent.Ready -> {
      item(key = "totals") { AccountsTotals(content.totals) }
      if (content.isSearchEmpty) {
        item(key = "search-empty") {
          TallybookSearchEmptyState(
              message = stringResource(Res.string.accounts_search_empty, content.query),
              modifier = Modifier.fillMaxWidth(),
          )
        }
      }
      content.groups.forEach { group ->
        item(key = "heading-${group.tipo}") {
          GroupHeading(stringResource(group.tipo.labelResource))
        }
        accountRows(group.rows, actions)
      }
      if (content.archived.isNotEmpty()) {
        item(key = "archived") {
          ArchivedHeading(uiState.archivedExpanded, uiState.query.isBlank(), actions)
        }
        if (uiState.showsArchived) accountRows(content.archived, actions)
      }
    }
  }
}

private fun LazyListScope.accountRows(rows: List<CuentaBalance>, actions: AccountsActions) {
  items(rows, key = { it.cuenta.id }) { row ->
    CuentaBalanceRow(
        balance = row,
        modifier = Modifier.fillMaxWidth(),
        onClick = { actions.onOpenCuenta(row.cuenta.id) },
        onClickLabel = stringResource(Res.string.accounts_edit),
    )
  }
}

@Composable
private fun AccountsTotals(totals: List<Money>) {
  if (totals.isEmpty()) return
  TallybookSurface(
      modifier = Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space8)
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      TallybookText(
          stringResource(Res.string.accounts_totals),
          modifier = Modifier.semantics { heading() },
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.balanceLabel,
      )
      totals.forEachIndexed { index, total ->
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          TallybookText(
              total.format(),
              modifier = Modifier.weight(1f, fill = false),
              style =
                  if (index == 0) TallybookTheme.typography.headline
                  else TallybookTheme.typography.balanceSecondaryAmount,
          )
          TallybookText(
              total.moneda.name,
              color = TallybookTheme.colors.foreground.tertiary,
              style = TallybookTheme.typography.caption,
          )
        }
      }
    }
  }
}

@Composable
private fun GroupHeading(text: String) {
  TallybookSectionHeading(text, modifier = Modifier.padding(top = TallybookTheme.spacing.space8))
}

@Composable
private fun ArchivedHeading(expanded: Boolean, togglable: Boolean, actions: AccountsActions) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookSectionHeading(
        stringResource(Res.string.accounts_archived),
        modifier = Modifier.weight(1f),
    )
    if (togglable) {
      TallybookTextButton(onClick = actions::onToggleArchived) {
        TallybookText(
            stringResource(
                if (expanded) Res.string.accounts_archived_hide
                else Res.string.accounts_archived_show
            )
        )
      }
    }
  }
}

@Composable
private fun AccountsTopBar(onBack: () -> Unit, onCreate: () -> Unit) {
  TallybookScreenHeader(
      title = stringResource(Res.string.accounts_title),
      modifier = Modifier.padding(top = TallybookTheme.spacing.space20),
      navigation = {
        TallybookIconButton(
            onClick = onBack,
            icon = Lucide.ChevronLeft,
            contentDescription = stringResource(Res.string.back),
            style = TallybookIconButtonStyle.Ghost,
        )
      },
      actions = {
        TallybookIconButton(
            onClick = onCreate,
            icon = Lucide.Plus,
            contentDescription = stringResource(Res.string.accounts_new),
            colors =
                TallybookIconButtonDefaults.colors(TallybookIconButtonStyle.Ghost)
                    .copy(content = TallybookTheme.colors.brand.primary),
            iconSize = TallybookTheme.dimensions.iconLarge,
        )
      },
  )
}

@Composable
private fun AccountsFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookText(
        stringResource(Res.string.accounts_load_error),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.body,
    )
    TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}
