package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.category.CategoryTag
import com.kurobello.tallybook.core.ui.category.categoryGroups
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.PickerSearchState
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSearchEmptyState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.pickerSearchHeight
import com.kurobello.tallybook.core.ui.component.rememberPickerSearchState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_picker_create
import tallybook.shared.generated.resources.category_picker_current
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.no_categories_found
import tallybook.shared.generated.resources.search_categories

internal const val CATEGORY_PICKER_OFFER_CREATE_TAG = "category_picker_offer_create"
internal const val CATEGORY_PICKER_CURRENT_TAG = "category_picker_current"

/**
 * One level of categories grouped by family, so no tap ever navigates. Tapping a category reports
 * it through [onSelect] (unless it is [selectedId] already) and closes the sheet. A selected
 * category other than Otros stays pinned above the results, which open scrolled to its family. With
 * [creation], a search without results offers creating the typed name in a pane of this same sheet;
 * Back from that pane returns to the results with the query kept. The caller's quick-create effect
 * selects the created category, which closes the sheet like a tap.
 */
@Composable
@Suppress("LongParameterList")
internal fun CategoryPickerSheet(
    visible: Boolean,
    categorias: List<Categoria>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    excludedIds: Set<String> = emptySet(),
    onHidden: () -> Unit = {},
    onShown: () -> Unit = {},
    paneTitle: String? = null,
    onBack: (() -> Unit)? = null,
    creation: CategoryPickerCreation? = null,
) {
  CategoryPickerHost(
      selection = CategoryPickerSelection(selectedId, onSelect),
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

/** The selection when quick-create started: a different one later is the created category. */
private data class PendingCreation(val selectedIdAtStart: String?)

@Composable
@Suppress("LongMethod")
private fun CategoryPickerHost(
    selection: CategoryPickerSelection,
    options: CategoryPickerOptions,
    sheet: CategoryPickerSheetHooks,
    paneTitle: String?,
) {
  val search = rememberPickerSearchState(options.visible)
  var pendingCreation by remember { mutableStateOf<PendingCreation?>(null) }
  LaunchedEffect(options.visible) { if (options.visible) pendingCreation = null }
  val currentOnDismiss by rememberUpdatedState(sheet.onDismiss)
  LaunchedEffect(selection.selectedId, pendingCreation) {
    val pending = pendingCreation ?: return@LaunchedEffect
    if (selection.selectedId != pending.selectedIdAtStart) {
      pendingCreation = null
      currentOnDismiss()
    }
  }

  val groups =
      remember(options.categorias, options.excludedIds, search.query) {
        categoryGroups(options.categorias, options.excludedIds, search.query)
      }
  val current =
      remember(options.categorias, selection.selectedId) {
        options.categorias.firstOrNull {
          it.id == selection.selectedId && it.id != CATEGORIA_OTROS_ID
        }
      }
  val listState = rememberLazyListState()
  LaunchedEffect(options.visible) {
    if (!options.visible) return@LaunchedEffect
    // Opening resets the search, so the family index comes from the unfiltered groups.
    val families = categoryGroups(options.categorias, options.excludedIds, query = "")
    val family = families.indexOfFirst { group -> group.any { it.id == selection.selectedId } }
    listState.scrollToItem(family.coerceAtLeast(0))
  }
  val resolvedPaneTitle = paneTitle ?: stringResource(Res.string.category_picker_single_title)
  val focusManager = LocalFocusManager.current
  val creation = options.creation
  val draft = creation?.state?.draft
  val closeCreation: () -> Unit = {
    focusManager.clearFocus(force = true)
    pendingCreation = null
    creation?.actions?.onClose()
  }
  val contentState =
      CategoryPickerContentState(
          search = search,
          groups = groups,
          selectedId = selection.selectedId,
          current = current,
          listState = listState,
          paneTitle = resolvedPaneTitle,
      )
  val callbacks =
      CategoryPickerContentCallbacks(
          onCategoryClick = { categoria ->
            selection.choose(categoria.id)
            sheet.onDismiss()
          },
          onCreate =
              creation?.let { offered ->
                { nombre ->
                  focusManager.clearFocus(force = true)
                  pendingCreation = PendingCreation(selection.selectedId)
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
    val search: PickerSearchState,
    val groups: List<List<Categoria>>,
    val selectedId: String?,
    val current: Categoria?,
    val listState: LazyListState,
    val paneTitle: String,
)

private data class CategoryPickerContentCallbacks(
    val onCategoryClick: (Categoria) -> Unit,
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
              .pickerSearchHeight(state.search)
              .testTag("category_picker_content")
              .padding(horizontal = TallybookTheme.spacing.space22)
              .padding(bottom = TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(text = state.paneTitle)
    CategoryPickerSearchField(
        query = state.search.query,
        onQueryChange = state.search::onQueryChange,
    )
    state.current?.let { current -> CategoryPickerCurrent(categoria = current) }
    CategoryPickerResults(
        state = state,
        onCategoryClick = callbacks.onCategoryClick,
        onCreate = callbacks.onCreate,
    )
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
private fun CategoryPickerCurrent(categoria: Categoria) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(CATEGORY_PICKER_CURRENT_TAG),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        text = stringResource(Res.string.category_picker_current),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
    CategoryTag(categoria = categoria)
  }
}

@Composable
private fun CategoryPickerResults(
    state: CategoryPickerContentState,
    onCategoryClick: (Categoria) -> Unit,
    onCreate: ((String) -> Unit)?,
) {
  if (state.groups.isEmpty() && state.search.isSearching) {
    CategoryPickerNoResults(query = state.search.query, onCreate = onCreate)
  } else {
    LazyColumn(
        state = state.listState,
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = TallybookTheme.dimensions.pickerResultsHeight)
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
                  selected = categoria.id == state.selectedId,
                  onClick = { onCategoryClick(categoria) },
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
  TallybookSearchEmptyState(
      message = stringResource(Res.string.no_categories_found),
      modifier = Modifier.fillMaxWidth(),
      action =
          if (onCreate == null || nombre.isEmpty()) null
          else {
            {
              TallybookTextButton(
                  onClick = { onCreate(nombre) },
                  modifier = Modifier.testTag(CATEGORY_PICKER_OFFER_CREATE_TAG),
              ) {
                TallybookText(stringResource(Res.string.category_picker_create, nombre))
              }
            }
          },
  )
}
