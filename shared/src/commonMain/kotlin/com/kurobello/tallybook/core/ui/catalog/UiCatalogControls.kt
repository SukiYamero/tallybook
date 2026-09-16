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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UtensilsCrossed
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.ui_catalog_clear_search
import tallybook.shared.generated.resources.ui_catalog_disabled_icon_button
import tallybook.shared.generated.resources.ui_catalog_icon_button
import tallybook.shared.generated.resources.ui_catalog_icon_buttons
import tallybook.shared.generated.resources.ui_catalog_loading_states
import tallybook.shared.generated.resources.ui_catalog_loading_supporting
import tallybook.shared.generated.resources.ui_catalog_search_fields
import tallybook.shared.generated.resources.ui_catalog_search_query
import tallybook.shared.generated.resources.ui_catalog_segmented_controls
import tallybook.shared.generated.resources.ui_catalog_select_category
import tallybook.shared.generated.resources.ui_catalog_selection_fields
import tallybook.shared.generated.resources.ui_catalog_selection_supporting
import tallybook.shared.generated.resources.ui_catalog_state_default
import tallybook.shared.generated.resources.ui_catalog_state_disabled
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

internal const val CATALOG_LOADING_REGION_TAG = "catalog_loading_region"

internal fun LazyListScope.catalogControlMatrices() {
  item(key = "icon-buttons") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_icon_buttons)) {
      CatalogSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
          CatalogSample(label = stringResource(Res.string.ui_catalog_state_default)) {
            TallybookIconButton(
                onClick = {},
                icon = Lucide.X,
                contentDescription = stringResource(Res.string.ui_catalog_icon_button),
            )
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
  item(key = "text-fields") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_text_fields)) {
      CatalogTextFieldMatrix()
    }
  }
  item(key = "search-fields") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_search_fields)) {
      CatalogSearchFieldMatrix()
    }
  }
  item(key = "segmented-controls") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_segmented_controls)) {
      CatalogSegmentedControlMatrix()
    }
  }
  item(key = "selection-fields") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_selection_fields)) {
      CatalogSelectionFieldMatrix()
    }
  }
  item(key = "loading-states") {
    CatalogSection(title = stringResource(Res.string.ui_catalog_loading_states)) {
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
            TallybookSegmentOption(CatalogMovementType.Expense, expense),
            TallybookSegmentOption(CatalogMovementType.Income, income),
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
private fun CatalogSample(label: String, content: @Composable () -> Unit) {
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
