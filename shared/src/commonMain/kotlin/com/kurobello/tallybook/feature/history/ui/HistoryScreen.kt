package com.kurobello.tallybook.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import com.kurobello.tallybook.feature.history.viewmodel.cachedValue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.history_empty_body
import tallybook.shared.generated.resources.history_empty_title
import tallybook.shared.generated.resources.history_movement_count
import tallybook.shared.generated.resources.history_unavailable
import tallybook.shared.generated.resources.history_year_error
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.retry

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    actions: HistoryActions,
    onAdd: () -> Unit,
    bottomBarClearance: Dp,
    modifier: Modifier = Modifier,
) {
  require(bottomBarClearance >= TallybookTheme.spacing.space0)
  val contentListState = rememberLazyListState()
  // A short loading list must not clamp restored row position before fresh rows arrive.
  val pendingListState = remember { LazyListState() }
  val listState =
      if (uiState.period.cachedValue == null && !uiState.isEmpty) pendingListState
      else contentListState
  val listDescription = stringResource(Res.string.navigation_history)
  TallybookScreenFrame(
      modifier = modifier,
      windowInsets =
          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
      bottomPaddingFloor = TallybookTheme.spacing.space0,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = listDescription },
        state = listState,
        contentPadding =
            PaddingValues(
                top = TallybookTheme.spacing.space20,
                bottom = bottomBarClearance + TallybookTheme.spacing.space16,
            ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      historyContent(uiState, actions, onAdd)
    }
  }
}

@Composable
internal fun HistoryRow(row: HistoryMovimientoRowUi, modifier: Modifier = Modifier) {
  MovimientoRowContent(
      row.movimiento,
      row.categoria,
      MovimientoRowText(row.title, historyDateSubtitle(row.fecha), row.categoryLabel),
      modifier = modifier.fillMaxWidth(),
  )
}

private fun LazyListScope.historyContent(
    uiState: HistoryUiState,
    actions: HistoryActions,
    onAdd: () -> Unit,
) {
  val content = uiState.period.cachedValue
  item(key = "history-context") { HistoryPeriodHeader(uiState, actions) }
  item(key = "history-count") {
    TallybookText(
        content?.let {
          pluralStringResource(
              Res.plurals.history_movement_count,
              it.movementCount,
              it.movementCount,
          )
        } ?: stringResource(Res.string.history_unavailable),
        style = TallybookTheme.typography.bodySmall,
    )
  }
  item(key = "history-scope") { HistoryScopeControl(uiState, actions) }
  item(key = "history-periods") { HistoryPeriodRow(uiState, actions) }
  if (uiState.years.failure != null)
      item(key = "history-years-error") {
        HistoryReadNotice(stringResource(Res.string.history_year_error), actions::onRetry)
      }
  if (uiState.preferenceFailure != null)
      item(key = "history-preferences-error") {
        HistoryReadNotice(stringResource(Res.string.movimiento_preferences_error), actions::onRetry)
      }
  item(key = "history-period-summary") { HistoryPeriodSummary(uiState.period, actions::onRetry) }
  if (uiState.isEmpty) item(key = "history-empty") { HistoryEmptyPeriod(onAdd) }
  content?.groups?.forEach { group ->
    item(key = group.key, contentType = "date") {
      TallybookText(
          historyFullDate(group.fecha),
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.label,
      )
    }
    items(group.rows, key = { it.key }, contentType = { "movimiento" }) { row -> HistoryRow(row) }
  }
}

@Composable
private fun HistoryReadNotice(message: String, onRetry: () -> Unit) {
  Column {
    TallybookText(message, style = TallybookTheme.typography.bodySmall)
    TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun HistoryEmptyPeriod(onAdd: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookText(
        stringResource(Res.string.history_empty_title),
        modifier = Modifier.semantics { heading() },
        style = TallybookTheme.typography.title,
    )
    TallybookText(stringResource(Res.string.history_empty_body))
    TallybookButton(onClick = onAdd) {
      TallybookText(stringResource(Res.string.navigation_add_movement))
    }
  }
}
