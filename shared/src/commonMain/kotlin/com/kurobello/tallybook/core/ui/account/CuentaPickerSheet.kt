package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.PickerSearchState
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSearchEmptyState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.pickerSearchHeight
import com.kurobello.tallybook.core.ui.component.rememberPickerSearchState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.entry_cuenta_picker_title
import tallybook.shared.generated.resources.entry_cuenta_search
import tallybook.shared.generated.resources.entry_cuenta_search_empty

/** Fewer cuentas than this fit the sheet at a glance, so searching them would only add a step. */
internal const val CUENTA_PICKER_SEARCH_THRESHOLD = 8

/** Cuentas whose name contains [query] under the shared search normalization. */
internal fun List<Cuenta>.matching(query: String): List<Cuenta> {
  val needle = normalizeSearchText(query.trim())
  return if (needle.isEmpty()) this else filter { needle in normalizeSearchText(it.nombre) }
}

/**
 * Single choice among [cuentas], which the caller has already limited to live, active cuentas
 * compatible with the medio. A tap reports the cuenta and closes the sheet.
 */
@Composable
@Suppress("LongParameterList")
internal fun CuentaPickerSheet(
    visible: Boolean,
    cuentas: List<Cuenta>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    primaryMoneda: Moneda? = null,
) {
  val title = stringResource(Res.string.entry_cuenta_picker_title)
  val search = rememberPickerSearchState(visible)
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = title,
      callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss, onBack = onDismiss),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .pickerSearchHeight(search)
                .padding(horizontal = TallybookTheme.spacing.space22)
                .padding(bottom = TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      TallybookSheetTitle(title)
      if (cuentas.size >= CUENTA_PICKER_SEARCH_THRESHOLD) CuentaSearchField(search)
      CuentaPickerResults(
          cuentas = cuentas.matching(search.query),
          query = search.query,
          selectedId = selectedId,
          primaryMoneda = primaryMoneda,
          onClick = { id ->
            onSelect(id)
            onDismiss()
          },
      )
    }
  }
}

@Composable
private fun CuentaSearchField(search: PickerSearchState) {
  TallybookSearchField(
      query = search.query,
      onQueryChange = search::onQueryChange,
      placeholder = stringResource(Res.string.entry_cuenta_search),
      clearContentDescription = stringResource(Res.string.clear_search),
      modifier = Modifier.fillMaxWidth(),
      onClear = { search.onQueryChange("") },
  )
}

@Composable
private fun CuentaPickerResults(
    cuentas: List<Cuenta>,
    query: String,
    selectedId: String?,
    primaryMoneda: Moneda?,
    onClick: (String) -> Unit,
) {
  if (cuentas.isEmpty()) {
    TallybookSearchEmptyState(
        message = stringResource(Res.string.entry_cuenta_search_empty, query.trim()),
        modifier = Modifier.fillMaxWidth(),
    )
    return
  }
  LazyColumn(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(max = TallybookTheme.dimensions.pickerResultsHeight)
              .selectableGroup()
  ) {
    items(cuentas, key = Cuenta::id) { cuenta ->
      CuentaPickerRow(
          cuenta = cuenta,
          name = cuentaDisplayName(cuenta, primaryMoneda),
          selected = cuenta.id == selectedId,
          onClick = { onClick(cuenta.id) },
      )
    }
  }
}

@Composable
private fun CuentaPickerRow(cuenta: Cuenta, name: String, selected: Boolean, onClick: () -> Unit) {
  val colors = TallybookTheme.colors
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  role = Role.RadioButton,
                  onClick = onClick,
              )
              .padding(vertical = TallybookTheme.spacing.space8),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    CuentaTypeIcon(
        tipo = cuenta.tipo,
        modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
        tint = cuenta.color.tintColors().content,
    )
    TallybookText(
        text = name,
        modifier = Modifier.weight(1f),
        color = colors.foreground.primary,
        style =
            if (selected) TallybookTheme.typography.bodyEmphasis
            else TallybookTheme.typography.body,
    )
    if (selected) {
      Image(
          imageVector = Lucide.Check,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(colors.brand.secondary),
      )
    }
  }
}
