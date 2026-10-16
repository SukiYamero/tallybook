package com.kurobello.tallybook.core.ui.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.PickerSearchState
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.pickerSearchHeight
import com.kurobello.tallybook.core.ui.component.rememberPickerSearchState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.search_tags
import tallybook.shared.generated.resources.tag_picker_create
import tallybook.shared.generated.resources.tag_picker_done
import tallybook.shared.generated.resources.tag_picker_limit_reached
import tallybook.shared.generated.resources.tag_picker_title
import tallybook.shared.generated.resources.tags_empty
import tallybook.shared.generated.resources.tags_search_empty

internal const val ETIQUETA_PICKER_SELECTED_TAG = "etiqueta_picker_selected"
internal const val ETIQUETA_PICKER_OFFER_CREATE_TAG = "etiqueta_picker_offer_create"

/**
 * Multi-select tag picker: a search field, the selected tags as removable tags under it, every
 * matching tag as a toggle and a "Done (n)" action that closes. Each toggle reports the whole new
 * selection through [onSelectionChange]; additions stop at [max] and a hint explains why. A search
 * without results offers creating the trimmed query through [onCreate] in one tap, also at [max];
 * the caller creates it and selects it only while the limit allows.
 */
@Composable
@Suppress("LongParameterList")
internal fun EtiquetaPickerSheet(
    visible: Boolean,
    etiquetas: List<Etiqueta>,
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    max: Int = MAX_ETIQUETAS_POR_MOVIMIENTO,
    onHidden: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
  val search = rememberPickerSearchState(visible)
  val results = remember(etiquetas, search.query) { searchEtiquetas(etiquetas, search.query) }
  val selected =
      remember(etiquetas, selectedIds) {
        val byId = etiquetas.associateBy(Etiqueta::id)
        selectedIds.mapNotNull(byId::get)
      }
  val title = stringResource(Res.string.tag_picker_title)
  val state =
      EtiquetaPickerContentState(
          search = search,
          catalogIsEmpty = etiquetas.isEmpty(),
          results = results,
          selected = selected,
          selectedIds = selectedIds,
          max = max,
          title = title,
      )
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(onDismiss = onDismiss, onHidden = onHidden, onBack = onBack),
      layout = ModalBottomSheetLayout(modifier = modifier),
  ) {
    EtiquetaPickerContent(
        state = state,
        onToggle = { id -> onSelectionChange(toggledEtiquetaIds(selectedIds, id, max)) },
        onCreate = onCreate,
        onDone = onDismiss,
    )
  }
}

private data class EtiquetaPickerContentState(
    val search: PickerSearchState,
    val catalogIsEmpty: Boolean,
    val results: List<Etiqueta>,
    val selected: List<Etiqueta>,
    val selectedIds: List<String>,
    val max: Int,
    val title: String,
) {
  val isFull: Boolean
    get() = selectedIds.size >= max
}

@Composable
private fun EtiquetaPickerContent(
    state: EtiquetaPickerContentState,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDone: () -> Unit,
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .pickerSearchHeight(state.search)
              .padding(horizontal = TallybookTheme.spacing.space22),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(text = state.title)
    TallybookSearchField(
        query = state.search.query,
        onQueryChange = state.search::onQueryChange,
        placeholder = stringResource(Res.string.search_tags),
        clearContentDescription = stringResource(Res.string.clear_search),
        modifier = Modifier.fillMaxWidth(),
        onClear = { state.search.onQueryChange("") },
    )
    if (state.selected.isNotEmpty()) {
      EtiquetaPickerSelected(state.selected) { onToggle(it.id) }
    }
    EtiquetaPickerResults(state = state, onToggle = onToggle, onCreate = onCreate)
    EtiquetaPickerFooter(state = state, onDone = onDone)
  }
}

@Composable
private fun EtiquetaPickerSelected(selected: List<Etiqueta>, onRemove: (Etiqueta) -> Unit) {
  FlowRow(
      modifier = Modifier.fillMaxWidth().testTag(ETIQUETA_PICKER_SELECTED_TAG),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    selected.forEach { etiqueta ->
      key(etiqueta.id) { EtiquetaTag(etiqueta = etiqueta, onRemove = { onRemove(etiqueta) }) }
    }
  }
}

@Composable
private fun EtiquetaPickerResults(
    state: EtiquetaPickerContentState,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
  when {
    state.results.isEmpty() && state.search.isSearching ->
        EtiquetaPickerNoResults(query = state.search.query, onCreate = onCreate)
    state.catalogIsEmpty -> EtiquetaPickerMessage(stringResource(Res.string.tags_empty))
    else ->
        FlowRow(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(max = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight)
                    .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
            verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        ) {
          state.results.forEach { etiqueta ->
            key(etiqueta.id) {
              val isSelected = etiqueta.id in state.selectedIds
              EtiquetaChip(
                  etiqueta = etiqueta,
                  selected = isSelected,
                  onClick = { onToggle(etiqueta.id) },
                  enabled = isSelected || !state.isFull,
              )
            }
          }
        }
  }
}

@Composable
private fun EtiquetaPickerNoResults(query: String, onCreate: (String) -> Unit) {
  val nombre = query.trim()
  Column(
      modifier = Modifier.padding(vertical = TallybookTheme.spacing.space24),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookText(
        text = stringResource(Res.string.tags_search_empty, nombre),
        style = TallybookTheme.typography.body,
    )
    if (nombre.isNotEmpty()) {
      TallybookTextButton(
          onClick = { onCreate(nombre) },
          modifier = Modifier.testTag(ETIQUETA_PICKER_OFFER_CREATE_TAG),
      ) {
        TallybookText(stringResource(Res.string.tag_picker_create, nombre))
      }
    }
  }
}

@Composable
private fun EtiquetaPickerMessage(text: String) {
  TallybookText(
      text = text,
      modifier = Modifier.padding(vertical = TallybookTheme.spacing.space24),
      color = TallybookTheme.colors.foreground.secondary,
      style = TallybookTheme.typography.body,
  )
}

@Composable
private fun EtiquetaPickerFooter(state: EtiquetaPickerContentState, onDone: () -> Unit) {
  Column(
      modifier = Modifier.padding(bottom = TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    if (state.isFull) {
      TallybookText(
          text = stringResource(Res.string.tag_picker_limit_reached, state.max),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
    TallybookButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
      TallybookText(stringResource(Res.string.tag_picker_done, state.selectedIds.size))
    }
  }
}

private val previewPickerEtiquetas =
    listOf("Empresa", "Viaje Chile", "Casa", "Regalos").map { nombre ->
      Etiqueta("tag_$nombre", nombre, previewEtiquetaCreatedAt)
    }

@Composable
private fun EtiquetaPickerPreviewContent() {
  val search = rememberPickerSearchState(visible = true)
  EtiquetaPickerContent(
      state =
          EtiquetaPickerContentState(
              search = search,
              catalogIsEmpty = false,
              results = previewPickerEtiquetas,
              selected = previewPickerEtiquetas.take(2),
              selectedIds = previewPickerEtiquetas.take(2).map(Etiqueta::id),
              max = 2,
              title = stringResource(Res.string.tag_picker_title),
          ),
      onToggle = {},
      onCreate = {},
      onDone = {},
  )
}

@Preview
@Composable
internal fun EtiquetaPickerSheetLightPreview() {
  TallybookTheme(darkTheme = false) { EtiquetaPickerPreviewContent() }
}

@Preview
@Composable
internal fun EtiquetaPickerSheetDarkPreview() {
  TallybookTheme(darkTheme = true) { EtiquetaPickerPreviewContent() }
}
