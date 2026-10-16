package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.tag.EtiquetaAddTag
import com.kurobello.tallybook.core.ui.tag.EtiquetaChip
import com.kurobello.tallybook.core.ui.tag.EtiquetaPickerSheet
import com.kurobello.tallybook.core.ui.tag.EtiquetaSuggestionTag
import com.kurobello.tallybook.core.ui.tag.EtiquetaTag
import com.kurobello.tallybook.core.ui.tag.EtiquetaTagField
import com.kurobello.tallybook.feature.movimientos.ui.rememberMovimientoEntrySamples
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tag_add
import tallybook.shared.generated.resources.ui_catalog_open_picker
import tallybook.shared.generated.resources.ui_catalog_state_disabled
import tallybook.shared.generated.resources.ui_catalog_state_empty
import tallybook.shared.generated.resources.ui_catalog_state_removable
import tallybook.shared.generated.resources.ui_catalog_state_selected
import tallybook.shared.generated.resources.ui_catalog_state_suggested
import tallybook.shared.generated.resources.ui_catalog_state_with_suggestions
import tallybook.shared.generated.resources.ui_catalog_tag_chips
import tallybook.shared.generated.resources.ui_catalog_tag_field
import tallybook.shared.generated.resources.ui_catalog_tag_picker
import tallybook.shared.generated.resources.ui_catalog_tags

private const val CHIP_SAMPLE_COUNT = 12L

internal fun LazyListScope.catalogEtiquetaComponents() {
  catalogControlSection("etiqueta-tags", { stringResource(Res.string.ui_catalog_tags) }) {
    CatalogEtiquetaTagMatrix()
  }
  catalogControlSection("etiqueta-tag-field", { stringResource(Res.string.ui_catalog_tag_field) }) {
    CatalogEtiquetaTagFieldMatrix()
  }
}

@Composable
private fun CatalogEtiquetaTagMatrix() {
  val etiquetas = rememberMovimientoEntrySamples().etiquetas
  val business = etiquetas[0]
  val trip = etiquetas[1]
  CatalogSurface {
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_selected)) { EtiquetaTag(business) }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_removable)) {
      EtiquetaTag(business, onRemove = {})
      EtiquetaTag(business, enabled = false, onRemove = {})
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_suggested)) {
      EtiquetaSuggestionTag(trip, onClick = {})
      EtiquetaSuggestionTag(trip, onClick = {}, enabled = false)
    }
    CatalogTagRow(stringResource(Res.string.tag_add)) {
      EtiquetaAddTag(onClick = {})
      EtiquetaAddTag(onClick = {}, enabled = false)
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_tag_chips)) {
      EtiquetaChip(business, selected = true, onClick = {})
      EtiquetaChip(trip, selected = false, onClick = {})
      EtiquetaChip(trip, selected = false, onClick = {}, count = CHIP_SAMPLE_COUNT)
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_disabled)) {
      EtiquetaChip(trip, selected = false, onClick = {}, enabled = false)
    }
  }
}

@Composable
private fun CatalogEtiquetaTagFieldMatrix() {
  val catalog = rememberMovimientoEntrySamples().etiquetas
  var selectedIds by remember { mutableStateOf(listOf(catalog[0].id)) }
  var pickerVisible by remember { mutableStateOf(false) }
  val selected = selectedIds.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_empty)) {
      EtiquetaTagField(
          etiquetas = emptyList(),
          max = MAX_ETIQUETAS_POR_MOVIMIENTO,
          onRemove = {},
          onAdd = {},
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_with_suggestions)) {
      EtiquetaTagField(
          etiquetas = selected,
          max = MAX_ETIQUETAS_POR_MOVIMIENTO,
          onRemove = { removed -> selectedIds = selectedIds - removed.id },
          onAdd = { pickerVisible = true },
          suggestions = catalog.drop(1).take(2),
          onSuggestion = { added -> selectedIds = selectedIds + added.id },
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_tag_picker)) {
      TallybookButton(onClick = { pickerVisible = true }) {
        TallybookText(stringResource(Res.string.ui_catalog_open_picker))
      }
    }
  }
  EtiquetaPickerSheet(
      visible = pickerVisible,
      etiquetas = catalog,
      selectedIds = selectedIds,
      onSelectionChange = { selectedIds = it },
      onCreate = {},
      onDismiss = { pickerVisible = false },
  )
}
