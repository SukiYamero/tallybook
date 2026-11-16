@file:Suppress("TooManyFunctions")

package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategorySingleField
import com.kurobello.tallybook.core.ui.category.CategorySuggestionTag
import com.kurobello.tallybook.core.ui.category.CategoryTag
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookModalDialog
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeSize
import com.kurobello.tallybook.core.ui.movimiento.MovimientoTypeBadge
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.CategoryPickerSheet
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoDateBadge
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoEntrySamples
import com.kurobello.tallybook.feature.movimientos.ui.VoiceDictationPlaceholder
import com.kurobello.tallybook.feature.movimientos.ui.rememberMovimientoEntrySamples
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_cine
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_mascotas
import tallybook.shared.generated.resources.category_salud
import tallybook.shared.generated.resources.category_supermercado
import tallybook.shared.generated.resources.category_transporte
import tallybook.shared.generated.resources.close
import tallybook.shared.generated.resources.ui_catalog_category_field
import tallybook.shared.generated.resources.ui_catalog_category_picker
import tallybook.shared.generated.resources.ui_catalog_category_tags
import tallybook.shared.generated.resources.ui_catalog_dialog_content
import tallybook.shared.generated.resources.ui_catalog_modal_dialog
import tallybook.shared.generated.resources.ui_catalog_movimiento_badges
import tallybook.shared.generated.resources.ui_catalog_open_dialog
import tallybook.shared.generated.resources.ui_catalog_open_picker
import tallybook.shared.generated.resources.ui_catalog_size_large
import tallybook.shared.generated.resources.ui_catalog_size_regular
import tallybook.shared.generated.resources.ui_catalog_state_pending_category
import tallybook.shared.generated.resources.ui_catalog_state_removable
import tallybook.shared.generated.resources.ui_catalog_state_selected
import tallybook.shared.generated.resources.ui_catalog_state_suggested
import tallybook.shared.generated.resources.ui_catalog_state_with_suggestions
import tallybook.shared.generated.resources.ui_catalog_voice_placeholder

private const val BADGE_SAMPLE_MONTH = 1
private const val BADGE_SAMPLE_DAY = 3

internal fun LazyListScope.catalogTagComponents() {
  catalogControlSection("category-tags", { stringResource(Res.string.ui_catalog_category_tags) }) {
    CatalogCategoryTagMatrix()
  }
  catalogControlSection(
      "category-single-field",
      { stringResource(Res.string.ui_catalog_category_field) },
  ) {
    CatalogCategorySingleFieldMatrix()
  }
  catalogEtiquetaComponents()
  catalogControlSection(
      "movimiento-badges",
      { stringResource(Res.string.ui_catalog_movimiento_badges) },
  ) {
    CatalogBadgeMatrix()
  }
  catalogControlSection("modal-dialog", { stringResource(Res.string.ui_catalog_modal_dialog) }) {
    CatalogModalDialogSample()
  }
  catalogControlSection(
      "voice-placeholder",
      { stringResource(Res.string.ui_catalog_voice_placeholder) },
  ) {
    CatalogSurface { VoiceDictationPlaceholder() }
  }
}

@Composable
private fun CatalogCategoryTagMatrix() {
  val samples = rememberMovimientoEntrySamples()
  CatalogSurface {
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_selected)) {
      CategoryTag(samples.cinema)
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_removable)) {
      CategoryTag(samples.cinema, onRemove = {})
      CategoryTag(samples.cinema, enabled = false, onRemove = {})
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_pending_category)) {
      CategoryTag(samples.other, pending = true)
    }
    CatalogTagRow(stringResource(Res.string.ui_catalog_state_suggested)) {
      CategorySuggestionTag(samples.cinema, onClick = {})
      CategorySuggestionTag(samples.cinema, onClick = {}, enabled = false)
    }
  }
}

@Composable
internal fun CatalogTagRow(label: String, content: @Composable () -> Unit) {
  CatalogSample(label = label) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      content()
    }
  }
}

@Composable
private fun CatalogCategorySingleFieldMatrix() {
  val categories = rememberCatalogCategories()
  val byId = remember(categories) { categories.associateBy(Categoria::id) }
  val other = rememberMovimientoEntrySamples().other
  var selectedId by remember { mutableStateOf<String?>(ID_FOOD) }
  var pickerVisible by remember { mutableStateOf(false) }
  val suggestions = remember(byId) { listOfNotNull(byId[ID_TRANSPORT], byId[ID_HEALTH]) }
  val selected = selectedId?.let(byId::get)
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_with_suggestions)) {
      CategorySingleField(
          categoria = selected ?: other,
          deselectable = true,
          onChoose = { pickerVisible = true },
          onSuggestion = { selectedId = it.id },
          onDeselect = { selectedId = null },
          pending = selected == null,
          suggestions = suggestions,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_pending_category)) {
      CategorySingleField(
          categoria = other,
          deselectable = false,
          onChoose = {},
          onSuggestion = {},
          pending = true,
          suggestions = suggestions,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_category_picker)) {
      TallybookButton(onClick = { pickerVisible = true }) {
        TallybookText(stringResource(Res.string.ui_catalog_open_picker))
      }
    }
  }
  CategoryPickerSheet(
      visible = pickerVisible,
      categorias = categories,
      selectedId = selectedId,
      onSelect = { selectedId = it },
      onDismiss = { pickerVisible = false },
      excludedIds = setOf(CATEGORIA_OTROS_ID),
  )
}

@Composable
private fun CatalogBadgeMatrix() {
  val today = MovimientoEntrySamples.today
  CatalogSurface {
    MovimientoBadgeSize.entries.forEach { size ->
      CatalogSample(label = stringResource(size.catalogLabel())) {
        Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
          TipoMovimiento.entries.forEach { MovimientoTypeBadge(it, size = size) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
          MovimientoDateBadge(today, today, size = size)
          MovimientoDateBadge(MovimientoEntrySamples.yesterday, today, size = size)
          MovimientoDateBadge(
              LocalDate(today.year, BADGE_SAMPLE_MONTH, BADGE_SAMPLE_DAY),
              today,
              size = size,
          )
        }
      }
    }
  }
}

private val sizeLabels =
    mapOf(
        MovimientoBadgeSize.Regular to Res.string.ui_catalog_size_regular,
        MovimientoBadgeSize.Large to Res.string.ui_catalog_size_large,
    )

private fun MovimientoBadgeSize.catalogLabel() = sizeLabels.getValue(this)

@Composable
private fun CatalogModalDialogSample() {
  var visible by remember { mutableStateOf(false) }
  val title = stringResource(Res.string.ui_catalog_modal_dialog)
  CatalogSurface {
    TallybookButton(onClick = { visible = true }) {
      TallybookText(stringResource(Res.string.ui_catalog_open_dialog))
    }
  }
  TallybookModalDialog(
      visible = visible,
      paneTitle = title,
      onDismissRequest = { visible = false },
  ) {
    TallybookSheetTitle(title)
    TallybookText(stringResource(Res.string.ui_catalog_dialog_content))
    TallybookButton(onClick = { visible = false }, modifier = Modifier.fillMaxWidth()) {
      TallybookText(stringResource(Res.string.close))
    }
  }
}

private const val ID_FOOD = "cat_comida"
private const val ID_TRANSPORT = "cat_transporte"
private const val ID_CINEMA = "cat_cine"
private const val ID_HEALTH = "cat_salud"

@Composable
private fun rememberCatalogCategories(): List<Categoria> {
  val food = stringResource(Res.string.category_comida)
  val groceries = stringResource(Res.string.category_supermercado)
  val transport = stringResource(Res.string.category_transporte)
  val cinema = stringResource(Res.string.category_cine)
  val health = stringResource(Res.string.category_salud)
  val home = stringResource(Res.string.category_hogar)
  val pets = stringResource(Res.string.category_mascotas)
  return remember(food, groceries, transport, cinema, health, home, pets) {
    listOf(
        Categoria(ID_FOOD, food, null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        Categoria(
            "cat_supermercado",
            groceries,
            ID_FOOD,
            CategoryIcon.SHOPPING_CART,
            CategoryTint.AMBER,
        ),
        Categoria(ID_TRANSPORT, transport, null, CategoryIcon.CAR, CategoryTint.BLUE),
        Categoria(ID_CINEMA, cinema, null, CategoryIcon.FILM, CategoryTint.PURPLE),
        Categoria(ID_HEALTH, health, null, CategoryIcon.HEART_PULSE, CategoryTint.RED),
        Categoria("cat_hogar", home, null, CategoryIcon.HOUSE, CategoryTint.EMERALD),
        Categoria("cat_mascotas", pets, null, CategoryIcon.PAW, CategoryTint.INDIGO),
    )
  }
}
