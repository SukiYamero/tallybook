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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.date.CivilDayHeader
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowSkeleton
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import com.kurobello.tallybook.core.ui.movimiento.rowDescription
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
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

private const val MOVEMENT_SKELETON_COUNT = 3

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    actions: HistoryActions,
    navigation: HistoryNavigationCallbacks,
    bottomBarClearance: Dp,
    modifier: Modifier = Modifier,
) {
  require(bottomBarClearance >= TallybookTheme.spacing.space0)
  val presentation by rememberHistoryPresentation(uiState)
  val renderedState = presentation.state
  val contentListState = rememberLazyListState()
  // A short loading list must not clamp restored row position before fresh rows arrive.
  val pendingListState = remember { LazyListState() }
  val listState =
      if (renderedState.period.cachedValue == null && !renderedState.isEmpty) pendingListState
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
      historyContent(
          renderedState,
          actions,
          navigation.onAdd,
          presentation.showLoading,
          navigation.onOpenMovimiento,
      )
    }
  }
}

@Composable
internal fun HistoryRow(
    row: HistoryMovimientoRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
  MovimientoRowContent(
      row.movimiento,
      row.categoria,
      MovimientoRowText(row.title, row.movimiento.rowDescription, row.categoryLabel),
      modifier = modifier.fillMaxWidth(),
      onClick = onClick,
      pending = CATEGORIA_OTROS_ID in row.movimiento.categoriaIds,
  )
}

private fun LazyListScope.historyContent(
    uiState: HistoryUiState,
    actions: HistoryActions,
    onAdd: () -> Unit,
    showLoading: Boolean,
    onOpenMovimiento: ((String) -> Unit)?,
) {
  val content = uiState.period.cachedValue
  item(key = "history-context") { HistoryPeriodHeader(uiState, actions) }
  item(key = "history-count") {
    if (content != null) {
      TallybookText(
          pluralStringResource(
              Res.plurals.history_movement_count,
              content.movementCount,
              content.movementCount,
          ),
          style = TallybookTheme.typography.bodySmall,
      )
    } else if (uiState.period is RetainedLoadState.Loading) {
      TallybookSkeletonLine(
          sampleText = pluralStringResource(Res.plurals.history_movement_count, 1, 1),
          style = TallybookTheme.typography.bodySmall,
          modifier = Modifier.alpha(if (showLoading) 1f else 0f),
      )
    } else {
      TallybookText(
          stringResource(Res.string.history_unavailable),
          style = TallybookTheme.typography.bodySmall,
      )
    }
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
  item(key = "history-period-summary") {
    HistoryPeriodSummary(uiState.period, actions::onRetry, showLoading)
  }
  if (showLoading && uiState.period is RetainedLoadState.Loading && content == null) {
    items(MOVEMENT_SKELETON_COUNT, key = { "history-skeleton-$it" }, contentType = { "skeleton" }) {
      MovimientoRowSkeleton(Modifier.fillMaxWidth())
    }
  }
  if (uiState.isEmpty) item(key = "history-empty") { HistoryEmptyPeriod(onAdd) }
  content?.groups?.forEach { group ->
    item(key = group.key, contentType = "date") { CivilDayHeader(group.fecha) }
    items(group.rows, key = { it.key }, contentType = { "movimiento" }) { row ->
      HistoryRow(row, onClick = onOpenMovimiento?.let { open -> { open(row.movimiento.id) } })
    }
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
