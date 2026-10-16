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
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.clear_search
import tallybook.shared.generated.resources.no_categories_found
import tallybook.shared.generated.resources.search_categories

@Suppress("LongMethod", "LongParameterList")
@Composable
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
  val tree =
      remember(categorias, excludedIds) { buildCategoryTree(categorias).without(excludedIds) }
  var query by rememberSaveable { mutableStateOf("") }
  var parentId by rememberSaveable { mutableStateOf<String?>(null) }
  var shownContentHeightPx by remember { mutableIntStateOf(0) }
  var searchMinimumHeightPx by remember { mutableIntStateOf(0) }

  LaunchedEffect(visible) {
    if (visible) {
      query = ""
      parentId = null
      shownContentHeightPx = 0
      searchMinimumHeightPx = 0
    }
  }

  val parent = tree.roots.firstOrNull { it.id == parentId }
  val isSearching = query.isNotBlank()
  val searchableCategories =
      if (allowSubcategories) categorias.filterNot { it.id in excludedIds } else tree.roots
  val shownCategories =
      when {
        isSearching -> searchCategorias(searchableCategories, query)
        parent != null -> listOf(parent) + tree.childrenByParent[parent.id].orEmpty()
        else -> tree.roots
      }
  val resolvedPaneTitle = paneTitle ?: stringResource(Res.string.category)
  val contentState =
      CategoryPickerContentState(
          query = query,
          parent = parent,
          shownCategories = shownCategories,
          selectedId = selectedId,
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
            if (tree.shouldOpenChildren(categoria, allowSubcategories, isSearching, parent)) {
              parentId = categoria.id
            } else {
              onSelect(categoria.id)
              onDismiss()
            }
          },
          onShownContentHeightChanged = { height ->
            if (!isSearching && shownContentHeightPx != height) {
              shownContentHeightPx = height
            }
          },
      )

  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = resolvedPaneTitle,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = onDismiss,
              onHidden = onHidden,
              onShown = onShown,
              onBack = onBack,
          ),
      layout = ModalBottomSheetLayout(modifier = modifier),
  ) {
    CategoryPickerContent(
        state = contentState,
        callbacks = callbacks,
        headerContent = headerContent,
    )
  }
}

// Filters after building so an excluded parent never promotes its children to roots.
internal fun CategoryTree.without(excludedIds: Set<String>): CategoryTree =
    if (excludedIds.isEmpty()) {
      this
    } else {
      CategoryTree(
          roots = roots.filterNot { it.id in excludedIds },
          childrenByParent =
              childrenByParent.mapValues { (_, children) ->
                children.filterNot { it.id in excludedIds }
              },
      )
    }

internal fun CategoryTree.shouldOpenChildren(
    categoria: Categoria,
    allowSubcategories: Boolean,
    isSearching: Boolean,
    parent: Categoria?,
): Boolean =
    allowSubcategories &&
        !isSearching &&
        parent == null &&
        childrenByParent[categoria.id].orEmpty().isNotEmpty()

private data class CategoryPickerContentState(
    val query: String,
    val parent: Categoria?,
    val shownCategories: List<Categoria>,
    val selectedId: String?,
    val paneTitle: String,
    val searchMinimumHeight: Dp,
) {
  val isSearching: Boolean
    get() = query.isNotBlank()
}

private data class CategoryPickerContentCallbacks(
    val onQueryChange: (String) -> Unit,
    val onBack: () -> Unit,
    val onCategoryClick: (Categoria) -> Unit,
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
    LazyColumn(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight)
                .testTag("category_picker_results"),
        contentPadding = PaddingValues(bottom = TallybookTheme.spacing.space24),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      items(state.shownCategories, key = Categoria::id) { categoria ->
        CategoryChip(
            categoria = categoria,
            selected = categoria.id == state.selectedId,
            onClick = { onCategoryClick(categoria) },
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun CategoryPickerBackButton(onClick: () -> Unit) {
  TallybookTextButton(onClick = onClick) { TallybookText(stringResource(Res.string.back)) }
}
