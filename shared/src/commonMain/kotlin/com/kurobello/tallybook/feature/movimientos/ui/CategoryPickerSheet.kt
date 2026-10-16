package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.category.categoryGroups
import com.kurobello.tallybook.core.ui.category.toImageVector
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookFilterTag
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_picker_create
import tallybook.shared.generated.resources.category_picker_selected_label
import tallybook.shared.generated.resources.category_picker_title
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.no_categories_found
import tallybook.shared.generated.resources.search_categories

internal const val CATEGORY_PICKER_SELECTED_TAG = "category_picker_selected"
internal const val CATEGORY_PICKER_OFFER_CREATE_TAG = "category_picker_offer_create"

/**
 * One level of tags grouped by family, so no tap ever navigates. Tags toggle through
 * [CategoriaSelection][com.kurobello.tallybook.core.model.CategoriaSelection], so a parent and its
 * children never coexist and [max] is respected, and every change is reported live through
 * [onSelectionChange]. The selection is also listed under the search field as removable tags.
 * "Done" closes the sheet. With [creation], a search without results offers creating the typed name
 * in a pane of this same sheet; Back from that pane returns to the results with the query kept.
 */
@Composable
@Suppress("LongParameterList")
internal fun CategoryPickerSheet(
    visible: Boolean,
    categorias: List<Categoria>,
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    max: Int = MAX_CATEGORIAS_POR_MOVIMIENTO,
    excludedIds: Set<String> = emptySet(),
    onHidden: () -> Unit = {},
    onShown: () -> Unit = {},
    paneTitle: String? = null,
    onBack: (() -> Unit)? = null,
    creation: CategoryPickerCreation? = null,
) {
  CategoryPickerHost(
      selection = CategoryPickerSelection(selectedIds, max, onSelectionChange),
      options =
          CategoryPickerOptions(
              visible = visible,
              categorias = categorias,
              excludedIds = excludedIds,
              creation = creation,
          ),
      sheet =
          CategoryPickerSheetHooks(
              onDismiss = onDismiss,
              onHidden = onHidden,
              onShown = onShown,
              onBack = onBack,
              modifier = modifier,
          ),
      paneTitle = paneTitle,
  )
}

private data class CategoryPickerOptions(
    val visible: Boolean,
    val categorias: List<Categoria>,
    val excludedIds: Set<String>,
    val creation: CategoryPickerCreation?,
)

private data class CategoryPickerSheetHooks(
    val onDismiss: () -> Unit,
    val onHidden: () -> Unit,
    val onShown: () -> Unit,
    val onBack: (() -> Unit)?,
    val modifier: Modifier,
)

@Composable
@Suppress("LongMethod")
private fun CategoryPickerHost(
    selection: CategoryPickerSelection,
    options: CategoryPickerOptions,
    sheet: CategoryPickerSheetHooks,
    paneTitle: String?,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var shownContentHeightPx by remember { mutableIntStateOf(0) }
  var searchMinimumHeightPx by remember { mutableIntStateOf(0) }

  LaunchedEffect(options.visible) {
    if (options.visible) {
      query = ""
      shownContentHeightPx = 0
      searchMinimumHeightPx = 0
    }
  }

  val isSearching = query.isNotBlank()
  val groups =
      remember(options.categorias, options.excludedIds, query) {
        categoryGroups(options.categorias, options.excludedIds, query)
      }
  val selected =
      remember(options.categorias, selection.selectedIds) {
        selectedCategorias(options.categorias, selection.selectedIds)
      }
  val resolvedPaneTitle = paneTitle ?: stringResource(Res.string.category_picker_title)
  val focusManager = LocalFocusManager.current
  val creation = options.creation
  val draft = creation?.state?.draft
  val closeCreation: () -> Unit = {
    focusManager.clearFocus(force = true)
    creation?.actions?.onClose()
  }
  val contentState =
      CategoryPickerContentState(
          query = query,
          groups = groups,
          selected = selected,
          selection = selection,
          catalog = options.categorias,
          paneTitle = resolvedPaneTitle,
          searchMinimumHeight = with(LocalDensity.current) { searchMinimumHeightPx.toDp() },
      )
  val callbacks =
      CategoryPickerContentCallbacks(
          onQueryChange = { value ->
            if (value.isNotBlank() && query.isBlank()) {
              searchMinimumHeightPx = shownContentHeightPx
            }
            query = value
            if (value.isBlank()) searchMinimumHeightPx = 0
          },
          onCategoryClick = { categoria -> selection.choose(categoria.id, options.categorias) },
          onDone = sheet.onDismiss,
          onShownContentHeightChanged = { height ->
            if (!isSearching && shownContentHeightPx != height) {
              shownContentHeightPx = height
            }
          },
          onCreate =
              creation?.let { offered ->
                { nombre ->
                  focusManager.clearFocus(force = true)
                  offered.actions.onStart(nombre)
                }
              },
      )

  TallybookModalBottomSheet(
      visible = options.visible,
      paneTitle =
          if (draft == null) resolvedPaneTitle else stringResource(Res.string.category_new_title),
      callbacks = sheet.sheetCallbacks(creation, closeCreation),
      layout = ModalBottomSheetLayout(modifier = sheet.modifier),
  ) {
    if (creation != null && draft != null) {
      CategoryPickerCreationPane(
          draft = draft,
          saveFailed = creation.state.saveFailed,
          actions = creation.actions,
          onBack = closeCreation,
      )
    } else {
      CategoryPickerContent(state = contentState, callbacks = callbacks)
    }
  }
}

private fun CategoryPickerSheetHooks.sheetCallbacks(
    creation: CategoryPickerCreation?,
    closeCreation: () -> Unit,
): ModalBottomSheetCallbacks =
    ModalBottomSheetCallbacks(
        onDismiss = onDismiss,
        // Hiding ends the creation pane however the sheet closed, so a reopen starts on the
        // results, and the pane stays in place while the sheet animates out.
        onHidden = {
          creation?.actions?.onClose()
          onHidden()
        },
        onShown = onShown,
        onBack =
            if (creation == null) onBack
            else {
              { if (creation.state.draft != null) closeCreation() else (onBack ?: onDismiss)() }
            },
    )

private data class CategoryPickerContentState(
    val query: String,
    val groups: List<List<Categoria>>,
    val selected: List<Categoria>,
    val selection: CategoryPickerSelection,
    val catalog: List<Categoria>,
    val paneTitle: String,
    val searchMinimumHeight: Dp,
) {
  val isSearching: Boolean
    get() = query.isNotBlank()

  // A tag whose tap would change nothing (at the limit without a replacement, or the last selected
  // category) is disabled.
  fun isEnabled(categoria: Categoria): Boolean =
      selection.toggled(categoria.id, catalog) != selection.selectedIds
}

private data class CategoryPickerContentCallbacks(
    val onQueryChange: (String) -> Unit,
    val onCategoryClick: (Categoria) -> Unit,
    val onDone: () -> Unit,
    val onShownContentHeightChanged: (Int) -> Unit,
    val onCreate: ((String) -> Unit)?,
)

@Composable
private fun CategoryPickerContent(
    state: CategoryPickerContentState,
    callbacks: CategoryPickerContentCallbacks,
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = if (state.isSearching) state.searchMinimumHeight else Dp.Unspecified)
              .onSizeChanged { size ->
                if (!state.isSearching) callbacks.onShownContentHeightChanged(size.height)
              }
              .testTag("category_picker_content")
              .padding(horizontal = TallybookTheme.spacing.space22),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(text = state.paneTitle)
    CategoryPickerSearchField(query = state.query, onQueryChange = callbacks.onQueryChange)
    if (state.selected.isNotEmpty()) {
      CategoryPickerSelected(state = state, onRemove = callbacks.onCategoryClick)
    }
    CategoryPickerResults(
        state = state,
        onCategoryClick = callbacks.onCategoryClick,
        onCreate = callbacks.onCreate,
    )
    CategoryPickerFooter(selection = state.selection, onDone = callbacks.onDone)
  }
}

@Composable
private fun CategoryPickerSearchField(query: String, onQueryChange: (String) -> Unit) {
  TallybookSearchField(
      query = query,
      onQueryChange = onQueryChange,
      placeholder = stringResource(Res.string.search_categories),
      clearContentDescription = stringResource(Res.string.clear_search),
      modifier = Modifier.fillMaxWidth(),
      onClear = { onQueryChange("") },
  )
}

@Composable
private fun CategoryPickerSelected(
    state: CategoryPickerContentState,
    onRemove: (Categoria) -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().testTag(CATEGORY_PICKER_SELECTED_TAG),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
  ) {
    TallybookSectionHeading(stringResource(Res.string.category_picker_selected_label))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      state.selected.forEach { categoria ->
        key(categoria.id) {
          TallybookFilterTag(
              label = categoria.nombre,
              removeContentDescription =
                  stringResource(Res.string.category_tag_remove, categoria.nombre),
              onRemove = { onRemove(categoria) },
              enabled = state.isEnabled(categoria),
              leadingIcon = categoria.icono.toImageVector(),
          )
        }
      }
    }
  }
}

@Composable
private fun CategoryPickerResults(
    state: CategoryPickerContentState,
    onCategoryClick: (Categoria) -> Unit,
    onCreate: ((String) -> Unit)?,
) {
  if (state.groups.isEmpty() && state.isSearching) {
    CategoryPickerNoResults(query = state.query, onCreate = onCreate)
  } else {
    LazyColumn(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight)
                .testTag("category_picker_results"),
        contentPadding = PaddingValues(bottom = TallybookTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      items(state.groups, key = { it.first().id }) { group ->
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
            verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        ) {
          group.forEach { categoria ->
            key(categoria.id) {
              CategoryChip(
                  categoria = categoria,
                  selected = categoria.id in state.selection.selectedIds,
                  onClick = { onCategoryClick(categoria) },
                  enabled = state.isEnabled(categoria),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryPickerNoResults(query: String, onCreate: ((String) -> Unit)?) {
  val nombre = query.trim()
  Column(
      modifier = Modifier.padding(vertical = TallybookTheme.spacing.space24),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookText(
        text = stringResource(Res.string.no_categories_found),
        style = TallybookTheme.typography.body,
    )
    if (onCreate != null && nombre.isNotEmpty()) {
      TallybookTextButton(
          onClick = { onCreate(nombre) },
          modifier = Modifier.testTag(CATEGORY_PICKER_OFFER_CREATE_TAG),
      ) {
        TallybookText(stringResource(Res.string.category_picker_create, nombre))
      }
    }
  }
}
