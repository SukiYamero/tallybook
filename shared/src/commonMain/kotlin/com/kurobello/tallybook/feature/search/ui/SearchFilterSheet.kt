@file:Suppress("LongParameterList")

package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.date.fullCivilDate
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchActions
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterResolution
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSession
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSessionId
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilters
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.resolveSearchFilters
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_all_history
import tallybook.shared.generated.resources.search_all_types
import tallybook.shared.generated.resources.search_apply
import tallybook.shared.generated.resources.search_clear_filters
import tallybook.shared.generated.resources.search_custom
import tallybook.shared.generated.resources.search_date_range
import tallybook.shared.generated.resources.search_end_date
import tallybook.shared.generated.resources.search_expenses
import tallybook.shared.generated.resources.search_filters
import tallybook.shared.generated.resources.search_income
import tallybook.shared.generated.resources.search_invalid_dates
import tallybook.shared.generated.resources.search_last_30_days
import tallybook.shared.generated.resources.search_last_7_days
import tallybook.shared.generated.resources.search_start_date
import tallybook.shared.generated.resources.search_this_month
import tallybook.shared.generated.resources.search_this_year
import tallybook.shared.generated.resources.search_type
import tallybook.shared.generated.resources.search_type_transfers

@Composable
internal fun SearchFilterSheet(
    uiState: SearchUiState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
  val sessionId =
      when (val session = uiState.filterSession) {
        SearchFilterSession.Hidden -> return
        is SearchFilterSession.Editing -> session.sessionId
        is SearchFilterSession.Closing -> session.sessionId
      }
  key(sessionId) {
    TallybookModalBottomSheet(
        visible = uiState.filterSession is SearchFilterSession.Editing,
        paneTitle = stringResource(Res.string.search_filters),
        callbacks =
            ModalBottomSheetCallbacks(
                onDismiss = { actions.onCancelFilters(sessionId) },
                onHidden = { actions.onFiltersHidden(sessionId) },
            ),
        layout =
            ModalBottomSheetLayout(
                modifier = modifier.fillMaxHeight(SEARCH_FILTER_SHEET_HEIGHT_FRACTION),
                fillAvailableHeight = true,
            ),
    ) {
      SearchFilterSheetContent(uiState, actions)
    }
  }
}

@Composable
internal fun SearchFilterSheetContent(
    uiState: SearchUiState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
  val session = uiState.filterSession
  val id: SearchFilterSessionId
  val filters: SearchFilters
  when (session) {
    SearchFilterSession.Hidden -> return
    is SearchFilterSession.Editing -> {
      id = session.sessionId
      filters = session.draft
    }
    is SearchFilterSession.Closing -> {
      id = session.sessionId
      filters = session.renderSnapshot
    }
  }
  val enabled = session is SearchFilterSession.Editing
  val valid = resolveSearchFilters(filters, uiState.today) is SearchFilterResolution.Valid
  Column(modifier.fillMaxSize().imePadding().padding(horizontal = TallybookTheme.spacing.space20)) {
    Column(
        modifier =
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
    ) {
      TallybookSheetTitle(stringResource(Res.string.search_filters))
      SearchDateFilters(filters, uiState, enabled) { actions.onFilterDraftChanged(id, it) }
      SearchTypeFilters(filters.tipo, enabled) {
        actions.onFilterDraftChanged(id, filters.copy(tipo = it))
      }
      SearchPaymentFilters(uiState, filters, enabled) { actions.onFilterDraftChanged(id, it) }
      SearchCategoryFilters(
          uiState.categories.values.orEmpty(),
          filters.categoryIds,
          enabled,
          { actions.onFilterDraftChanged(id, filters.toggleCategory(it)) },
          Modifier.fillMaxWidth(),
      )
      val etiquetas = uiState.etiquetas.values.orEmpty()
      if (etiquetas.isNotEmpty()) {
        SearchTagFilters(
            etiquetas,
            filters.etiquetaIds,
            enabled,
            { actions.onFilterDraftChanged(id, filters.toggleEtiqueta(it)) },
            Modifier.fillMaxWidth(),
        )
      }
    }
    SearchFilterFooter(
        enabled,
        valid,
        { actions.onClearFilters(id) },
        { actions.onApplyFilters(id) },
    )
  }
}

/** Medio and Account, the dimensions that place a movimiento's money. */
@Composable
private fun SearchPaymentFilters(
    uiState: SearchUiState,
    filters: SearchFilters,
    enabled: Boolean,
    onChange: (SearchFilters) -> Unit,
) {
  SearchMedioFilters(
      filters.medios,
      enabled,
      { onChange(filters.toggleMedio(it)) },
      Modifier.fillMaxWidth(),
  )
  val moneda = uiState.moneda ?: return
  SearchAccountFilters(
      uiState.cuentas.values.orEmpty(),
      moneda,
      uiState.primary ?: moneda,
      filters.cuentaIds,
      enabled,
      { onChange(filters.toggleCuenta(it)) },
      Modifier.fillMaxWidth(),
  )
}

// Wrapping pills rather than a segmented control: four labels, one of them "Between accounts",
// do not fit one row of a phone at larger font scales.
@Composable
private fun SearchTypeFilters(
    selectedType: SearchTipo?,
    enabled: Boolean,
    onSelect: (SearchTipo?) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.search_type))
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      (listOf(null) + SearchTipo.entries).forEach { tipo ->
        TallybookChoicePill(
            searchTipoLabel(tipo),
            selectedType == tipo,
            { onSelect(tipo) },
            enabled = enabled,
        )
      }
    }
  }
}

private val searchTipoResources: Map<SearchTipo, StringResource> =
    mapOf(
        SearchTipo.GASTO to Res.string.search_expenses,
        SearchTipo.INGRESO to Res.string.search_income,
        SearchTipo.TRANSFERENCIA to Res.string.search_type_transfers,
    )

@Composable
internal fun searchTipoLabel(tipo: SearchTipo?): String =
    stringResource(tipo?.let(searchTipoResources::getValue) ?: Res.string.search_all_types)

@Composable
private fun SearchFilterFooter(
    enabled: Boolean,
    valid: Boolean,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = TallybookTheme.spacing.space12),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookTextButton(onClear, enabled = enabled) {
      TallybookText(stringResource(Res.string.search_clear_filters))
    }
    TallybookButton(
        onApply,
        Modifier.weight(1f),
        enabled = enabled && valid,
    ) {
      TallybookText(stringResource(Res.string.search_apply))
    }
  }
}

@Composable
private fun SearchDateFilters(
    filters: SearchFilters,
    state: SearchUiState,
    enabled: Boolean,
    onChange: (SearchFilters) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.search_date_range))
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      SearchDatePreset.entries.forEach { preset ->
        TallybookChoicePill(
            searchPresetLabel(preset),
            filters.preset == preset,
            { onChange(filters.copy(preset = preset)) },
            enabled = enabled,
        )
      }
    }
    if (filters.preset == SearchDatePreset.Custom) {
      Column(
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      ) {
        val startDate = fullCivilDate(filters.customStart)
        val endDate = fullCivilDate(filters.customEnd)
        val minimumYear = state.availableYears.minOrNull() ?: state.today.year
        SearchCustomDateRow(stringResource(Res.string.search_start_date)) {
          TallybookDateChipPicker(
              filters.customStart,
              { onChange(filters.copy(customStart = it)) },
              "${stringResource(Res.string.search_start_date)}: $startDate",
              enabled = enabled,
              maxDate = minOf(filters.customEnd, state.today),
              minYear = minimumYear,
              valueLabel = startDate,
              calendarDaySize = TallybookTheme.dimensions.minimumTouchTarget,
          )
        }
        SearchCustomDateRow(stringResource(Res.string.search_end_date)) {
          TallybookDateChipPicker(
              filters.customEnd,
              { onChange(filters.copy(customEnd = it)) },
              "${stringResource(Res.string.search_end_date)}: $endDate",
              enabled = enabled,
              maxDate = state.today,
              minYear = minimumYear,
              valueLabel = endDate,
              calendarDaySize = TallybookTheme.dimensions.minimumTouchTarget,
              minDate = filters.customStart,
          )
        }
      }
    }
    if (resolveSearchFilters(filters, state.today) is SearchFilterResolution.Invalid) {
      TallybookText(
          stringResource(Res.string.search_invalid_dates),
          color = TallybookTheme.colors.status.danger.base,
      )
    }
  }
}

@Composable
private fun SearchCustomDateRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        label,
        modifier = Modifier.widthIn(min = TallybookTheme.spacing.space48),
        style = TallybookTheme.typography.label,
        color = TallybookTheme.colors.foreground.secondary,
    )
    content()
  }
}

private val searchPresetResources: Map<SearchDatePreset, StringResource> =
    mapOf(
        SearchDatePreset.All to Res.string.search_all_history,
        SearchDatePreset.Last7Days to Res.string.search_last_7_days,
        SearchDatePreset.Last30Days to Res.string.search_last_30_days,
        SearchDatePreset.ThisMonth to Res.string.search_this_month,
        SearchDatePreset.ThisYear to Res.string.search_this_year,
        SearchDatePreset.Custom to Res.string.search_custom,
    )

@Composable
internal fun searchPresetLabel(preset: SearchDatePreset): String =
    stringResource(searchPresetResources.getValue(preset))

private const val SEARCH_FILTER_SHEET_HEIGHT_FRACTION = 0.72f
