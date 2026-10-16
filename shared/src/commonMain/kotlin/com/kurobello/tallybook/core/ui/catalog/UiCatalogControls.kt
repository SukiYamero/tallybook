@file:Suppress("MagicNumber", "TooManyFunctions")

package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UtensilsCrossed
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.component.TallybookAmountInput
import com.kurobello.tallybook.core.ui.component.TallybookButtonColors
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import com.kurobello.tallybook.core.ui.component.TallybookDateChipSize
import com.kurobello.tallybook.core.ui.component.TallybookDayPeriod
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookFilterTag
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookMonthPeriod
import com.kurobello.tallybook.core.ui.component.TallybookPeriod
import com.kurobello.tallybook.core.ui.component.TallybookPeriodOption
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerHeader
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerRow
import com.kurobello.tallybook.core.ui.component.TallybookPeriodScope
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentTone
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.component.TallybookWeekPeriod
import com.kurobello.tallybook.core.ui.component.TallybookYearMenu
import com.kurobello.tallybook.core.ui.component.focusedMonthForYear
import com.kurobello.tallybook.core.ui.component.selectableDayPeriods
import com.kurobello.tallybook.core.ui.component.selectableMonthPeriods
import com.kurobello.tallybook.core.ui.component.selectableWeekPeriods
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_weekday_names
import tallybook.shared.generated.resources.date_picker_weekdays
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.ui_catalog_amount_inputs
import tallybook.shared.generated.resources.ui_catalog_choice_pills
import tallybook.shared.generated.resources.ui_catalog_clear_search
import tallybook.shared.generated.resources.ui_catalog_date_pickers
import tallybook.shared.generated.resources.ui_catalog_disabled_icon_button
import tallybook.shared.generated.resources.ui_catalog_filter_tags
import tallybook.shared.generated.resources.ui_catalog_icon_button
import tallybook.shared.generated.resources.ui_catalog_icon_buttons
import tallybook.shared.generated.resources.ui_catalog_loading_states
import tallybook.shared.generated.resources.ui_catalog_loading_supporting
import tallybook.shared.generated.resources.ui_catalog_period_day
import tallybook.shared.generated.resources.ui_catalog_period_month
import tallybook.shared.generated.resources.ui_catalog_period_pickers
import tallybook.shared.generated.resources.ui_catalog_period_scope
import tallybook.shared.generated.resources.ui_catalog_period_summary
import tallybook.shared.generated.resources.ui_catalog_period_week
import tallybook.shared.generated.resources.ui_catalog_search_fields
import tallybook.shared.generated.resources.ui_catalog_search_query
import tallybook.shared.generated.resources.ui_catalog_segmented_controls
import tallybook.shared.generated.resources.ui_catalog_select_category
import tallybook.shared.generated.resources.ui_catalog_selection_fields
import tallybook.shared.generated.resources.ui_catalog_selection_supporting
import tallybook.shared.generated.resources.ui_catalog_state_default
import tallybook.shared.generated.resources.ui_catalog_state_disabled
import tallybook.shared.generated.resources.ui_catalog_state_emphasis
import tallybook.shared.generated.resources.ui_catalog_state_error
import tallybook.shared.generated.resources.ui_catalog_state_populated
import tallybook.shared.generated.resources.ui_catalog_state_selected
import tallybook.shared.generated.resources.ui_catalog_state_supporting
import tallybook.shared.generated.resources.ui_catalog_text_field_error
import tallybook.shared.generated.resources.ui_catalog_text_field_label
import tallybook.shared.generated.resources.ui_catalog_text_field_placeholder
import tallybook.shared.generated.resources.ui_catalog_text_field_supporting
import tallybook.shared.generated.resources.ui_catalog_text_field_value
import tallybook.shared.generated.resources.ui_catalog_text_fields
import tallybook.shared.generated.resources.ui_catalog_year_selector

internal const val CATALOG_LOADING_REGION_TAG = "catalog_loading_region"

internal fun LazyListScope.catalogControlMatrices() {
  catalogControlSection("icon-buttons", { stringResource(Res.string.ui_catalog_icon_buttons) }) {
    catalogIconButtonMatrix()
  }
  catalogControlSection("text-fields", { stringResource(Res.string.ui_catalog_text_fields) }) {
    CatalogTextFieldMatrix()
  }
  catalogControlSection("search-fields", { stringResource(Res.string.ui_catalog_search_fields) }) {
    CatalogSearchFieldMatrix()
  }
  catalogControlSection("amount-inputs", { stringResource(Res.string.ui_catalog_amount_inputs) }) {
    CatalogAmountInputMatrix()
  }
  catalogControlSection("date-pickers", { stringResource(Res.string.ui_catalog_date_pickers) }) {
    CatalogDatePickerMatrix()
  }
  catalogControlSection("choice-pills", { stringResource(Res.string.ui_catalog_choice_pills) }) {
    CatalogChoicePillMatrix()
  }
  catalogControlSection("filter-tags", { stringResource(Res.string.ui_catalog_filter_tags) }) {
    CatalogFilterTagMatrix()
  }
  catalogControlSection(
      "period-pickers",
      { stringResource(Res.string.ui_catalog_period_pickers) },
  ) {
    CatalogPeriodPickerMatrix()
  }
  catalogControlSection(
      "segmented-controls",
      { stringResource(Res.string.ui_catalog_segmented_controls) },
  ) {
    CatalogSegmentedControlMatrix()
  }
  catalogControlSection(
      "selection-fields",
      { stringResource(Res.string.ui_catalog_selection_fields) },
  ) {
    CatalogSelectionFieldMatrix()
  }
  catalogControlSection(
      "loading-states",
      { stringResource(Res.string.ui_catalog_loading_states) },
  ) {
    CatalogSurface {
      TallybookLoadingState(
          contentDescription = stringResource(Res.string.loading),
          modifier =
              Modifier.fillMaxWidth()
                  .heightIn(min = TallybookTheme.dimensions.containedLoadingMinHeight)
                  .testTag(CATALOG_LOADING_REGION_TAG),
          supportingText = stringResource(Res.string.ui_catalog_loading_supporting),
      )
    }
  }
}

internal fun LazyListScope.catalogControlSection(
    key: String,
    title: @Composable () -> String,
    content: @Composable () -> Unit,
) {
  item(key = key) {
    CatalogSection(
        title = title(),
        automationTag = catalogSectionAutomationTag(key),
    ) {
      content()
    }
  }
}

@Composable
private fun CatalogChoicePillMatrix() {
  val label = stringResource(Res.string.ui_catalog_period_day)
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookChoicePill(label, selected = false, onClick = {})
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_selected)) {
      TallybookChoicePill(label, selected = true, onClick = {})
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookChoicePill(label, selected = false, onClick = {}, enabled = false)
    }
  }
}

@Composable
private fun CatalogFilterTagMatrix() {
  val label = stringResource(Res.string.category_comida)
  val removal = stringResource(Res.string.search_remove_filter, label)
  val tint = CategoryTint.AMBER.tintColors()
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookFilterTag(label, removal, {})
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_emphasis)) {
      TallybookFilterTag(
          label,
          removal,
          {},
          colors =
              TallybookButtonColors(tint.container, tint.content, tint.container, tint.content),
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookFilterTag(label, removal, {}, enabled = false)
    }
  }
}

@Composable
private fun CatalogAmountInputMatrix() {
  var value by rememberSaveable { mutableStateOf("") }
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookAmountInput(
          value = value,
          onValueChange = { value = it },
          label = stringResource(Res.string.amount),
          currencySymbol = "$",
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_error)) {
      TallybookAmountInput(
          value = "",
          onValueChange = {},
          label = stringResource(Res.string.amount),
          currencySymbol = "$",
          message = TallybookFieldMessage.Error(stringResource(Res.string.amount_required)),
      )
    }
  }
}

@Composable
private fun CatalogDatePickerMatrix() {
  var value by rememberSaveable { mutableStateOf(LocalDate(2026, 9, 15)) }
  CatalogSurface {
    CatalogSample(label = "S") {
      TallybookDateChipPicker(
          value = value,
          onValueChange = { value = it },
          label = stringResource(Res.string.date),
          modifier = Modifier.testTag(CATALOG_DATE_PICKER_DEFAULT_TAG),
          maxDate = LocalDate(2026, 9, 15),
      )
    }
    CatalogSample(label = "M") {
      TallybookDateChipPicker(
          value = value,
          onValueChange = { value = it },
          label = stringResource(Res.string.date),
          size = TallybookDateChipSize.M,
          maxDate = LocalDate(2026, 9, 15),
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookDateChipPicker(
          value = value,
          onValueChange = {},
          label = stringResource(Res.string.date),
          enabled = false,
      )
    }
  }
}

@Composable
private fun CatalogPeriodPickerMatrix() {
  val today = remember { currentCatalogDate() }
  val day = stringResource(Res.string.ui_catalog_period_day)
  val week = stringResource(Res.string.ui_catalog_period_week)
  val month = stringResource(Res.string.ui_catalog_period_month)
  val summary = stringResource(Res.string.ui_catalog_period_summary)
  val scopeDescription = stringResource(Res.string.ui_catalog_period_scope)
  val yearDescription = stringResource(Res.string.ui_catalog_year_selector)
  val weekdayNames = stringArrayResource(Res.array.date_picker_weekdays)
  val weekdayFullNames = stringArrayResource(Res.array.date_picker_weekday_names)
  val monthNames = stringArrayResource(Res.array.date_picker_months)
  val copy =
      CatalogPeriodPickerCopy(
          scopes =
              remember(day, week, month) {
                listOf(
                    TallybookSegmentOption(TallybookPeriodScope.Day, day),
                    TallybookSegmentOption(TallybookPeriodScope.Week, week),
                    TallybookSegmentOption(TallybookPeriodScope.Month, month),
                )
              },
          scopeDescription = scopeDescription,
          yearDescription = yearDescription,
          week = week,
          summary = summary,
      )

  CatalogPeriodPickerSample(today, copy, weekdayNames, weekdayFullNames, monthNames)
}

@Composable
private fun CatalogPeriodPickerSample(
    today: LocalDate,
    copy: CatalogPeriodPickerCopy,
    weekdayNames: List<String>,
    weekdayFullNames: List<String>,
    monthNames: List<String>,
) {
  val years = remember(today) { (today.year downTo FIRST_SELECTABLE_YEAR).toList() }
  var scope by remember { mutableStateOf(TallybookPeriodScope.Day) }
  var focusedMonth by remember(today) { mutableStateOf(YearMonth(today.year, today.month)) }
  var selectedPeriod by
      remember(today) { mutableStateOf<TallybookPeriod>(TallybookDayPeriod(today)) }
  val periods = remember(scope, focusedMonth, today) { catalogPeriods(scope, focusedMonth, today) }
  val periodOptions =
      remember(periods, weekdayNames, monthNames) {
        periods.map { period -> catalogPeriodOption(period, weekdayNames, monthNames) }
      }
  val header = rememberCatalogPeriodHeader(selectedPeriod, weekdayFullNames, monthNames, copy)

  CatalogSurface {
    TallybookPeriodPickerHeader(
        title = header.title,
        supportingText = header.supportingText,
        yearControl = {
          TallybookYearMenu(
              years = years,
              selectedYear = focusedMonth.year,
              onSelect = { year ->
                val updatedMonth = focusedMonthForYear(focusedMonth, year, today)
                focusedMonth = updatedMonth
                selectedPeriod = catalogDefaultPeriod(scope, updatedMonth, today)
              },
              contentDescription = copy.yearDescription,
              modifier = Modifier.testTag(CATALOG_PERIOD_PICKER_YEAR_MENU_TAG),
          )
        },
    )
    TallybookSegmentedControl(
        options = copy.scopes,
        selected = scope,
        onSelect = { selectedScope ->
          scope = selectedScope
          selectedPeriod = catalogDefaultPeriod(selectedScope, focusedMonth, today)
        },
        modifier = Modifier.fillMaxWidth().testTag(CATALOG_PERIOD_PICKER_SCOPE_TAG),
        style = TallybookSegmentedControlStyle.Neutral,
    )
    TallybookPeriodPickerRow(
        options = periodOptions,
        selected = selectedPeriod,
        onSelect = { period ->
          selectedPeriod = period
          if (period is TallybookMonthPeriod) focusedMonth = period.month
        },
        contentDescription = copy.scopeDescription,
        modifier = Modifier.fillMaxWidth().testTag(CATALOG_PERIOD_PICKER_ROW_TAG),
    )
  }
}

private data class CatalogPeriodPickerCopy(
    val scopes: List<TallybookSegmentOption<TallybookPeriodScope>>,
    val scopeDescription: String,
    val yearDescription: String,
    val week: String,
    val summary: String,
)

@Composable
private fun rememberCatalogPeriodHeader(
    period: TallybookPeriod,
    weekdayNames: List<String>,
    monthNames: List<String>,
    copy: CatalogPeriodPickerCopy,
): CatalogPeriodHeader =
    remember(period, weekdayNames, monthNames, copy.week, copy.summary) {
      catalogPeriodHeader(period, weekdayNames, monthNames, copy.week, copy.summary)
    }

private fun catalogPeriods(
    scope: TallybookPeriodScope,
    focusedMonth: YearMonth,
    today: LocalDate,
): List<TallybookPeriod> =
    when (scope) {
      TallybookPeriodScope.Day -> selectableDayPeriods(focusedMonth, today)
      TallybookPeriodScope.Week -> selectableWeekPeriods(focusedMonth, today)
      TallybookPeriodScope.Month -> selectableMonthPeriods(focusedMonth.year, today)
    }

private fun catalogDefaultPeriod(
    scope: TallybookPeriodScope,
    focusedMonth: YearMonth,
    today: LocalDate,
): TallybookPeriod =
    when (scope) {
      TallybookPeriodScope.Month -> TallybookMonthPeriod(focusedMonth)
      else -> catalogPeriods(scope, focusedMonth, today).last()
    }

private fun catalogPeriodOption(
    period: TallybookPeriod,
    weekdayNames: List<String>,
    monthNames: List<String>,
): TallybookPeriodOption<TallybookPeriod> =
    when (period) {
      is TallybookDayPeriod ->
          TallybookPeriodOption(
              value = period,
              label = period.date.day.toString(),
              caption = weekdayNames[period.date.dayOfWeek.ordinal],
          )
      is TallybookWeekPeriod ->
          TallybookPeriodOption(value = period, label = "${period.start.day}–${period.end.day}")
      is TallybookMonthPeriod ->
          TallybookPeriodOption(
              value = period,
              label = monthNames[period.month.month.ordinal].take(MONTH_LABEL_LENGTH),
          )
    }

private fun catalogPeriodHeader(
    period: TallybookPeriod,
    weekdayNames: List<String>,
    monthNames: List<String>,
    weekLabel: String,
    summaryLabel: String,
): CatalogPeriodHeader =
    when (period) {
      is TallybookDayPeriod ->
          CatalogPeriodHeader(
              title = "${weekdayNames[period.date.dayOfWeek.ordinal]} ${period.date.day}",
              supportingText = "${monthNames[period.date.month.ordinal]} ${period.date.year}",
          )
      is TallybookWeekPeriod ->
          CatalogPeriodHeader(
              title =
                  "${period.start.day}–${period.end.day} ${monthNames[period.end.month.ordinal]}",
              supportingText =
                  "$weekLabel · ${monthNames[period.end.month.ordinal]} ${period.end.year}",
          )
      is TallybookMonthPeriod ->
          CatalogPeriodHeader(
              title = "${monthNames[period.month.month.ordinal]} ${period.month.year}",
              supportingText = summaryLabel,
          )
    }

private fun currentCatalogDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private data class CatalogPeriodHeader(val title: String, val supportingText: String?)

private val catalogIconButtonMatrix: @Composable () -> Unit = {
  CatalogSection(title = stringResource(Res.string.ui_catalog_icon_buttons)) {
    CatalogSurface {
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
          CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
            TallybookIconButton(
                onClick = {},
                icon = Lucide.X,
                contentDescription = stringResource(Res.string.ui_catalog_icon_button),
            )
          }
          CatalogSample(label = stringResource(Res.string.ui_catalog_state_emphasis)) {
            TallybookIconButton(
                onClick = {},
                icon = Lucide.X,
                contentDescription = stringResource(Res.string.ui_catalog_icon_button),
                style = TallybookIconButtonStyle.Brand,
            )
          }
        }
        CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
          TallybookIconButton(
              onClick = {},
              icon = Lucide.X,
              contentDescription = stringResource(Res.string.ui_catalog_disabled_icon_button),
              enabled = false,
          )
        }
      }
    }
  }
}

@Composable
private fun CatalogTextFieldMatrix() {
  val initialValue = stringResource(Res.string.ui_catalog_text_field_value)
  var defaultValue by rememberSaveable { mutableStateOf("") }
  var populatedValue by rememberSaveable { mutableStateOf(initialValue) }
  var supportingValue by rememberSaveable { mutableStateOf("") }
  var errorValue by rememberSaveable { mutableStateOf("") }
  val label = stringResource(Res.string.ui_catalog_text_field_label)
  val placeholder = stringResource(Res.string.ui_catalog_text_field_placeholder)
  val supportingMessage =
      TallybookFieldMessage.Supporting(stringResource(Res.string.ui_catalog_text_field_supporting))
  val error = TallybookFieldMessage.Error(stringResource(Res.string.ui_catalog_text_field_error))

  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookTextField(
          value = defaultValue,
          onValueChange = { defaultValue = it },
          modifier = Modifier.fillMaxWidth(),
          label = label,
          placeholder = placeholder,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_populated)) {
      TallybookTextField(
          value = populatedValue,
          onValueChange = { populatedValue = it },
          modifier = Modifier.fillMaxWidth(),
          label = label,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookTextField(
          value = initialValue,
          onValueChange = {},
          modifier = Modifier.fillMaxWidth(),
          label = label,
          enabled = false,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_supporting)) {
      TallybookTextField(
          value = supportingValue,
          onValueChange = { supportingValue = it },
          modifier = Modifier.fillMaxWidth(),
          label = label,
          placeholder = placeholder,
          message = supportingMessage,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_error)) {
      TallybookTextField(
          value = errorValue,
          onValueChange = { errorValue = it },
          modifier = Modifier.fillMaxWidth(),
          label = label,
          message = error,
      )
    }
  }
}

@Composable
private fun CatalogSearchFieldMatrix() {
  val initialQuery = stringResource(Res.string.ui_catalog_search_query)
  var defaultQuery by rememberSaveable { mutableStateOf("") }
  var populatedQuery by rememberSaveable { mutableStateOf(initialQuery) }
  val placeholder = stringResource(Res.string.home_search_placeholder)
  val clearDescription = stringResource(Res.string.ui_catalog_clear_search)

  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookSearchField(
          query = defaultQuery,
          onQueryChange = { defaultQuery = it },
          placeholder = placeholder,
          clearContentDescription = clearDescription,
          modifier = Modifier.fillMaxWidth(),
          onClear = { defaultQuery = "" },
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_populated)) {
      TallybookSearchField(
          query = populatedQuery,
          onQueryChange = { populatedQuery = it },
          placeholder = placeholder,
          clearContentDescription = clearDescription,
          modifier = Modifier.fillMaxWidth(),
          onClear = { populatedQuery = "" },
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookSearchField(
          query = stringResource(Res.string.ui_catalog_search_query),
          onQueryChange = {},
          placeholder = placeholder,
          clearContentDescription = clearDescription,
          modifier = Modifier.fillMaxWidth(),
          enabled = false,
          onClear = {},
      )
    }
  }
}

@Composable
private fun CatalogSegmentedControlMatrix() {
  val expense = stringResource(Res.string.expense)
  val income = stringResource(Res.string.income)
  val options =
      remember(expense, income) {
        listOf(
            TallybookSegmentOption(
                value = CatalogMovementType.Expense,
                label = expense,
                icon = Lucide.ArrowUpRight,
                tone = TallybookSegmentTone.Expense,
            ),
            TallybookSegmentOption(
                value = CatalogMovementType.Income,
                label = income,
                icon = Lucide.ArrowDownLeft,
                tone = TallybookSegmentTone.Income,
            ),
        )
      }
  var selected by rememberSaveable { mutableStateOf(CatalogMovementType.Expense) }

  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_selected)) {
      TallybookSegmentedControl(
          options = options,
          selected = selected,
          onSelect = { selected = it },
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookSegmentedControl(
          options = options,
          selected = CatalogMovementType.Expense,
          onSelect = {},
          modifier = Modifier.fillMaxWidth(),
          enabled = false,
      )
    }
  }
}

@Composable
private fun CatalogSelectionFieldMatrix() {
  val food = stringResource(Res.string.category_comida)
  var defaultValue by rememberSaveable { mutableStateOf<String?>(null) }
  var populatedValue by rememberSaveable { mutableStateOf<String?>(food) }
  val label = stringResource(Res.string.category)
  val placeholder = stringResource(Res.string.ui_catalog_select_category)

  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
      TallybookSelectionField(
          label = label,
          value = defaultValue,
          placeholder = placeholder,
          onClick = { defaultValue = defaultValue.toggled(food) },
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_populated)) {
      TallybookSelectionField(
          label = label,
          value = populatedValue,
          placeholder = placeholder,
          onClick = { populatedValue = populatedValue.toggled(food) },
          modifier = Modifier.fillMaxWidth(),
          leadingContent = { CatalogFoodIcon() },
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      TallybookSelectionField(
          label = label,
          value = food,
          placeholder = placeholder,
          onClick = {},
          modifier = Modifier.fillMaxWidth(),
          enabled = false,
      )
    }
    CatalogSelectionMessageStates(food = food, label = label, placeholder = placeholder)
  }
}

@Composable
private fun CatalogSelectionMessageStates(food: String, label: String, placeholder: String) {
  var supportingValue by rememberSaveable { mutableStateOf<String?>(null) }
  var errorValue by rememberSaveable { mutableStateOf<String?>(null) }

  CatalogSample(label = stringResource(Res.string.ui_catalog_state_supporting)) {
    TallybookSelectionField(
        label = label,
        value = supportingValue,
        placeholder = placeholder,
        onClick = { supportingValue = supportingValue.toggled(food) },
        modifier = Modifier.fillMaxWidth(),
        message =
            TallybookFieldMessage.Supporting(
                stringResource(Res.string.ui_catalog_selection_supporting)
            ),
    )
  }
  CatalogSample(label = stringResource(Res.string.ui_catalog_state_error)) {
    TallybookSelectionField(
        label = label,
        value = errorValue,
        placeholder = placeholder,
        onClick = { errorValue = errorValue.toggled(food) },
        modifier = Modifier.fillMaxWidth(),
        message = TallybookFieldMessage.Error(stringResource(Res.string.category_required)),
    )
  }
}

@Composable
private fun CatalogFoodIcon() {
  Image(
      imageVector = Lucide.UtensilsCrossed,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.fieldIconSize),
      colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
  )
}

@Composable
internal fun CatalogSample(label: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookText(text = label, style = TallybookTheme.typography.caption)
    content()
  }
}

@Composable
internal fun CatalogSurface(content: @Composable () -> Unit) {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      content()
    }
  }
}

private fun String?.toggled(value: String): String? = if (this == null) value else null

private enum class CatalogMovementType {
  Expense,
  Income,
}

private const val MONTH_LABEL_LENGTH = 3
