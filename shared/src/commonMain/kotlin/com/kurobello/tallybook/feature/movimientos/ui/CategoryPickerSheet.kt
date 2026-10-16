package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaSelection
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.no_categories_found
import tallybook.shared.generated.resources.search_categories

@Composable
@Suppress("LongParameterList")
fun CategoryPickerSheet(
    visible: Boolean,
    categorias: List<Categoria>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    allowSubcategories: Boolean = true,
    excludedIds: Set<String> = emptySet(),
    onHidden: () -> Unit = {},
    onShown: () -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
    paneTitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
  CategoryPickerHost(
      mode = CategoryPickerMode.Single(selectedId, onSelect),
      options =
          CategoryPickerOptions(
              visible = visible,
              categorias = categorias,
              allowSubcategories = allowSubcategories,
              excludedIds = excludedIds,
          ),
      sheet =
          CategoryPickerSheetHooks(
              onDismiss = onDismiss,
              onHidden = onHidden,
              onShown = onShown,
              onBack = onBack,
              modifier = modifier,
          ),
      headerContent = headerContent,
      paneTitle = paneTitle,
  )
}

/**
 * Multi-select flavor: rows toggle through [CategoriaSelection], so a parent and its children never
 * coexist and [max] is respected, and every change is reported live through [onSelectionChange].
 * Drilling into a parent never selects it; its own row inside the group does. "Done" closes the
 * sheet.
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
    max: Int = CategoriaSelection.DEFAULT_LIMIT,
    excludedIds: Set<String> = emptySet(),
    onHidden: () -> Unit = {},
    onShown: () -> Unit = {},
    paneTitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
  CategoryPickerHost(
      mode = CategoryPickerMode.Multiple(selectedIds, max, onSelectionChange),
      options =
          CategoryPickerOptions(
              visible = visible,
              categorias = categorias,
              allowSubcategories = true,
              excludedIds = excludedIds,
          ),
      sheet =
          CategoryPickerSheetHooks(
              onDismiss = onDismiss,
              onHidden = onHidden,
              onShown = onShown,
              onBack = onBack,
              modifier = modifier,
          ),
      headerContent = null,
      paneTitle = paneTitle,
  )
}

private data class CategoryPickerOptions(
    val visible: Boolean,
    val categorias: List<Categoria>,
    val allowSubcategories: Boolean,
    val excludedIds: Set<String>,
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
    mode: CategoryPickerMode,
    options: CategoryPickerOptions,
    sheet: CategoryPickerSheetHooks,
    headerContent: (@Composable () -> Unit)?,
    paneTitle: String?,
) {
  val categorias = options.categorias
  val tree =
      remember(categorias, options.excludedIds) {
        buildCategoryTree(categorias).without(options.excludedIds)
      }
  var query by rememberSaveable { mutableStateOf("") }
  var parentId by rememberSaveable { mutableStateOf<String?>(null) }
  var shownContentHeightPx by remember { mutableIntStateOf(0) }
  var searchMinimumHeightPx by remember { mutableIntStateOf(0) }

  LaunchedEffect(options.visible) {
    if (options.visible) {
      query = ""
      parentId = null
      shownContentHeightPx = 0
      searchMinimumHeightPx = 0
    }
  }

  val parent = tree.roots.firstOrNull { it.id == parentId }
  val isSearching = query.isNotBlank()
  val shownCategories =
      tree.shownCategories(
          catalog = categorias,
          options = options,
          parent = parent,
          query = query,
      )
  val opensChildren: (Categoria) -> Boolean = { categoria ->
    tree.shouldOpenChildren(categoria, options.allowSubcategories, isSearching, parent)
  }
  val resolvedPaneTitle = paneTitle ?: stringResource(mode.defaultPaneTitle)
  val contentState =
      CategoryPickerContentState(
          query = query,
          parent = parent,
          shownCategories = shownCategories,
          mode = mode,
          catalog = categorias,
          opensChildren = opensChildren,
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
            if (value.isNotBlank()) {
              parentId = null
            } else {
              searchMinimumHeightPx = 0
            }
          },
          onBack = { parentId = null },
          onCategoryClick = { categoria ->
            if (opensChildren(categoria)) {
              parentId = categoria.id
            } else {
              mode.choose(categoria.id, categorias, sheet.onDismiss)
            }
          },
          onDone = sheet.onDismiss,
          onShownContentHeightChanged = { height ->
            if (!isSearching && shownContentHeightPx != height) {
              shownContentHeightPx = height
            }
          },
      )

  TallybookModalBottomSheet(
      visible = options.visible,
      paneTitle = resolvedPaneTitle,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = sheet.onDismiss,
              onHidden = sheet.onHidden,
              onShown = sheet.onShown,
              onBack = sheet.onBack,
          ),
      layout = ModalBottomSheetLayout(modifier = sheet.modifier),
  ) {
    CategoryPickerContent(
        state = contentState,
        callbacks = callbacks,
        headerContent = headerContent,
    )
  }
}

private fun CategoryTree.shownCategories(
    catalog: List<Categoria>,
    options: CategoryPickerOptions,
    parent: Categoria?,
    query: String,
): List<Categoria> {
  val searchable =
      if (options.allowSubcategories) catalog.filterNot { it.id in options.excludedIds } else roots
  return when {
    query.isNotBlank() -> searchCategorias(searchable, query)
    parent != null -> listOf(parent) + childrenByParent[parent.id].orEmpty()
    else -> roots
  }
}

private data class CategoryPickerContentState(
    val query: String,
    val parent: Categoria?,
    val shownCategories: List<Categoria>,
    val mode: CategoryPickerMode,
    val catalog: List<Categoria>,
    val opensChildren: (Categoria) -> Boolean,
    val paneTitle: String,
    val searchMinimumHeight: Dp,
) {
  val isSearching: Boolean
    get() = query.isNotBlank()

  // Only a row that would change nothing (at the limit, not a replacement) is disabled: rows that
  // drill into children and rows already selected stay enabled.
  fun isEnabled(categoria: Categoria): Boolean =
      when (mode) {
        is CategoryPickerMode.Single -> true
        is CategoryPickerMode.Multiple ->
            opensChildren(categoria) ||
                categoria.id in mode.selectedIds ||
                mode.toggled(categoria.id, catalog) != mode.selectedIds
      }
}

private data class CategoryPickerContentCallbacks(
    val onQueryChange: (String) -> Unit,
    val onBack: () -> Unit,
    val onCategoryClick: (Categoria) -> Unit,
    val onDone: () -> Unit,
    val onShownContentHeightChanged: (Int) -> Unit,
)

@Composable
private fun CategoryPickerContent(
    state: CategoryPickerContentState,
    callbacks: CategoryPickerContentCallbacks,
    headerContent: (@Composable () -> Unit)?,
) {
  val shouldPreserveHeight = state.isSearching
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(
                  min =
                      if (shouldPreserveHeight) {
                        state.searchMinimumHeight
                      } else {
                        Dp.Unspecified
                      }
              )
              .onSizeChanged { size ->
                if (!state.isSearching) callbacks.onShownContentHeightChanged(size.height)
              }
              .testTag("category_picker_content")
              .padding(horizontal = TallybookTheme.spacing.space22),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    if (headerContent != null) headerContent()
    else
        CategoryPickerHeader(
            paneTitle = state.paneTitle,
            parent = state.parent.takeUnless { state.isSearching },
            onBack = callbacks.onBack,
        )
    CategoryPickerSearchField(query = state.query, onQueryChange = callbacks.onQueryChange)
    CategoryPickerResults(
        state = state,
        onCategoryClick = callbacks.onCategoryClick,
    )
    val mode = state.mode
    if (mode is CategoryPickerMode.Multiple) {
      CategoryPickerFooter(mode = mode, onDone = callbacks.onDone)
    }
  }
}

@Composable
private fun CategoryPickerHeader(
    paneTitle: String,
    parent: Categoria?,
    onBack: () -> Unit,
) {
  if (parent != null) CategoryPickerBackButton(onClick = onBack)
  TallybookSheetTitle(
      text = parent?.nombre ?: paneTitle,
  )
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
private fun CategoryPickerResults(
    state: CategoryPickerContentState,
    onCategoryClick: (Categoria) -> Unit,
) {
  if (state.shownCategories.isEmpty() && state.isSearching) {
    TallybookText(
        text = stringResource(Res.string.no_categories_found),
        modifier = Modifier.padding(vertical = TallybookTheme.spacing.space24),
        style = TallybookTheme.typography.body,
    )
  } else {
    val isMultiple = state.mode is CategoryPickerMode.Multiple
    LazyColumn(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight)
                .testTag("category_picker_results"),
        contentPadding =
            PaddingValues(
                bottom =
                    if (isMultiple) TallybookTheme.spacing.space4
                    else TallybookTheme.spacing.space24
            ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      items(state.shownCategories, key = Categoria::id) { categoria ->
        CategoryChip(
            categoria = categoria,
            selected = categoria.id in state.mode.selectedIds,
            onClick = { onCategoryClick(categoria) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEnabled(categoria),
            trailingCheck = isMultiple,
        )
      }
    }
  }
}

@Composable
private fun CategoryPickerBackButton(onClick: () -> Unit) {
  TallybookTextButton(onClick = onClick) { TallybookText(stringResource(Res.string.back)) }
}
