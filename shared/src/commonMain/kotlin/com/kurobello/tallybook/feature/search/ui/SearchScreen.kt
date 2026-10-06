@file:Suppress("LongParameterList", "LongMethod")

package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonColors
import com.kurobello.tallybook.core.ui.component.TallybookFilterTag
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowSkeleton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchActions
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchFailure
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterResolution
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSession
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.resolveSearchFilters
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.search_adjust_filters
import tallybook.shared.generated.resources.search_clear_query
import tallybook.shared.generated.resources.search_clear_text
import tallybook.shared.generated.resources.search_count
import tallybook.shared.generated.resources.search_date_range_value
import tallybook.shared.generated.resources.search_error
import tallybook.shared.generated.resources.search_expenses
import tallybook.shared.generated.resources.search_filters
import tallybook.shared.generated.resources.search_filters_active
import tallybook.shared.generated.resources.search_filters_collapsed
import tallybook.shared.generated.resources.search_filters_expanded
import tallybook.shared.generated.resources.search_filters_inactive
import tallybook.shared.generated.resources.search_income
import tallybook.shared.generated.resources.search_invalid_dates
import tallybook.shared.generated.resources.search_loading
import tallybook.shared.generated.resources.search_no_data_body
import tallybook.shared.generated.resources.search_no_data_title
import tallybook.shared.generated.resources.search_no_results_body
import tallybook.shared.generated.resources.search_no_results_query_body
import tallybook.shared.generated.resources.search_no_results_title
import tallybook.shared.generated.resources.search_placeholder
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.search_title
import tallybook.shared.generated.resources.unknown_category

private const val SEARCH_SKELETON_ROWS = 4

@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    actions: SearchActions,
    onAdd: () -> Unit,
    onOpenFilters: () -> Unit,
    bottomBarClearance: Dp,
    modifier: Modifier = Modifier,
    contentListState: LazyListState = rememberLazyListState(),
    fieldModifier: Modifier = Modifier,
    filterButtonModifier: Modifier = Modifier,
) {
  require(bottomBarClearance >= TallybookTheme.spacing.space0)
  val presentation by rememberSearchPresentation(uiState)
  val feedback = searchFeedback(uiState)
  val showFeedback = presentation.showLoading && presentation.state.request == feedback.request
  val skeleton = showFeedback && feedback.kind == SearchFeedbackKind.Skeleton
  val progress = showFeedback && feedback.kind == SearchFeedbackKind.Progress
  val pendingListState = remember { LazyListState() }
  val content = uiState.displayResults
  val listDescription = stringResource(Res.string.search_title)
  val loadingDescription = stringResource(Res.string.search_loading)
  TallybookScreenFrame(
      modifier = modifier,
      windowInsets =
          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
      bottomPaddingFloor = TallybookTheme.spacing.space0,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = listDescription },
        state = if (content != null) contentListState else pendingListState,
        contentPadding =
            PaddingValues(
                top = TallybookTheme.spacing.space20,
                bottom = bottomBarClearance + TallybookTheme.spacing.space16,
            ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      item(key = "search-header") {
        SearchHeader(uiState, progress, onOpenFilters, filterButtonModifier)
      }
      item(key = "search-field") {
        TallybookSearchField(
            uiState.rawQuery,
            actions::onQueryChanged,
            stringResource(Res.string.search_placeholder),
            stringResource(Res.string.search_clear_query),
            modifier = Modifier.fillMaxWidth(),
            inputModifier = fieldModifier,
            onClear = actions::onClearQuery,
        )
      }
      if (uiState.filters.hasActiveFilters)
          item(key = "search-applied-filters") { SearchAppliedChips(uiState, actions) }
      if (resolveSearchFilters(uiState.filters, uiState.today) is SearchFilterResolution.Invalid) {
        item(key = "search-invalid-dates") {
          SearchNotice(
              stringResource(Res.string.search_invalid_dates),
              onOpenFilters,
              correction = true,
              enabled = uiState.canOpenFilters,
          )
        }
      }
      if (
          uiState.results.failure is SearchFailure.Repository ||
              uiState.preferenceFailure != null ||
              uiState.categories.failure != null
      ) {
        item(key = "search-failure") {
          SearchNotice(stringResource(Res.string.search_error), actions::onRetry)
        }
      }
      if (skeleton) {
        item(key = "search-skeletons", contentType = "skeleton") {
          Column(
              modifier =
                  Modifier.semantics {
                    contentDescription = loadingDescription
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                  },
              verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
          ) {
            repeat(SEARCH_SKELETON_ROWS) { MovimientoRowSkeleton(Modifier.fillMaxWidth()) }
          }
        }
      }
      if (uiState.isCurrent && content?.count == 0) {
        item(key = "search-empty") {
          SearchEmpty(content, uiState.rawQuery, actions, onAdd, onOpenFilters)
        }
      }
      items(
          content?.rows.orEmpty(),
          key = { "search-row:${it.id}" },
          contentType = { "movement" },
      ) {
        SearchResultRow(it)
      }
    }
  }
}

@Composable
private fun SearchHeader(
    state: SearchUiState,
    progress: Boolean,
    onOpenFilters: () -> Unit,
    filterButtonModifier: Modifier,
) {
  val activeLabel =
      stringResource(
          if (state.filters.hasActiveFilters) Res.string.search_filters_active
          else Res.string.search_filters_inactive
      )
  val expandedLabel =
      stringResource(
          if (state.filterSession != SearchFilterSession.Hidden) Res.string.search_filters_expanded
          else Res.string.search_filters_collapsed
      )
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      TallybookText(
          stringResource(Res.string.search_title),
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.headline,
      )
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      ) {
        val count = state.currentCount
        when {
          count != null ->
              TallybookText(
                  pluralStringResource(Res.plurals.search_count, count, count),
                  modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                  style = TallybookTheme.typography.bodySmall,
              )
          state.work != null || state.results.isLoading ->
              TallybookText(
                  stringResource(Res.string.search_loading),
                  style = TallybookTheme.typography.bodySmall,
              )
          else ->
              TallybookSkeletonLine(
                  pluralStringResource(Res.plurals.search_count, 1, 1),
                  style = TallybookTheme.typography.bodySmall,
                  modifier = Modifier.alpha(0f),
              )
        }
        if (progress)
            TallybookProgressIndicator(
                stringResource(Res.string.search_loading),
                Modifier.size(TallybookTheme.dimensions.iconMedium),
            )
      }
    }
    TallybookIconButton(
        onOpenFilters,
        Lucide.ListFilter,
        stringResource(Res.string.search_filters),
        modifier =
            filterButtonModifier.semantics {
              selected = state.filters.hasActiveFilters
              stateDescription = "$activeLabel. $expandedLabel"
            },
        enabled = state.canOpenFilters && state.filterSession == SearchFilterSession.Hidden,
        style =
            if (state.filters.hasActiveFilters) TallybookIconButtonStyle.Brand
            else TallybookIconButtonStyle.Neutral,
    )
  }
}

@Composable
private fun SearchAppliedChips(state: SearchUiState, actions: SearchActions) {
  val unknownLabel = stringResource(Res.string.unknown_category)
  val categories =
      state.categories.values.orEmpty().map {
        it.copy(nombre = searchCategoryLabel(it, unknownLabel))
      }
  val selectedCategories =
      searchCategoryGroups(categories).flatten().filter { it.id in state.filters.categoryIds }
  val missing = state.filters.categoryIds - categories.mapTo(mutableSetOf()) { it.id }
  LazyRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    if (state.filters.preset != SearchDatePreset.All)
        item(key = "date") {
          val label =
              if (state.filters.preset == SearchDatePreset.Custom)
                  stringResource(
                      Res.string.search_date_range_value,
                      searchFullDate(state.filters.customStart),
                      searchFullDate(state.filters.customEnd),
                  )
              else searchPresetLabel(state.filters.preset)
          TallybookFilterTag(
              label,
              stringResource(Res.string.search_remove_filter, label),
              actions::onRemoveDateFilter,
          )
        }
    state.filters.tipo?.let { type ->
      item(key = "type") {
        val label =
            stringResource(
                if (type == TipoMovimiento.GASTO) Res.string.search_expenses
                else Res.string.search_income
            )
        TallybookFilterTag(
            label,
            stringResource(Res.string.search_remove_filter, label),
            actions::onRemoveTypeFilter,
        )
      }
    }
    items(selectedCategories, key = { "category:${it.id}" }) { category ->
      val tint = category.color.tintColors()
      TallybookFilterTag(
          category.nombre,
          stringResource(Res.string.search_remove_filter, category.nombre),
          { actions.onRemoveCategoryFilter(category.id) },
          colors =
              TallybookButtonColors(tint.container, tint.content, tint.container, tint.content),
      )
    }
    items(missing.sorted(), key = { "category:$it" }) { id ->
      val label = stringResource(Res.string.unknown_category)
      TallybookFilterTag(
          label,
          stringResource(Res.string.search_remove_filter, label),
          { actions.onRemoveCategoryFilter(id) },
      )
    }
  }
}

@Composable
private fun SearchNotice(
    message: String,
    onRetry: () -> Unit,
    correction: Boolean = false,
    enabled: Boolean = true,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookText(message)
    TallybookTextButton(onRetry, enabled = enabled) {
      TallybookText(
          stringResource(if (correction) Res.string.search_adjust_filters else Res.string.retry)
      )
    }
  }
}

@Composable
private fun SearchEmpty(
    content: SearchResultsUi,
    queryLabel: String,
    actions: SearchActions,
    onAdd: () -> Unit,
    onOpenFilters: () -> Unit,
) {
  val noData = !content.years.hasCurrencyData
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookText(
        stringResource(
            if (noData) Res.string.search_no_data_title else Res.string.search_no_results_title
        ),
        modifier = Modifier.semantics { heading() },
        style = TallybookTheme.typography.title,
    )
    val message =
        when {
          noData -> stringResource(Res.string.search_no_data_body)
          content.request.normalizedQuery.isNotEmpty() ->
              stringResource(Res.string.search_no_results_query_body, queryLabel)
          else -> stringResource(Res.string.search_no_results_body)
        }
    TallybookText(message)
    if (noData)
        TallybookButton(onAdd) { TallybookText(stringResource(Res.string.navigation_add_movement)) }
    else {
      if (content.request.normalizedQuery.isNotEmpty())
          TallybookTextButton(actions::onClearQuery) {
            TallybookText(stringResource(Res.string.search_clear_text))
          }
      TallybookButton(onOpenFilters) {
        TallybookText(stringResource(Res.string.search_adjust_filters))
      }
    }
  }
}
