package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.no_categories_found
import tallybook.shared.generated.resources.search_categories

@Suppress("LongParameterList")
@Composable
fun CategoryPickerSheet(
    visible: Boolean,
    categorias: List<Categoria>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val tree = remember(categorias) { buildCategoryTree(categorias) }
  var query by rememberSaveable { mutableStateOf("") }
  var parentId by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(visible) {
    if (visible) {
      query = ""
      parentId = null
    }
  }

  val parent = tree.roots.firstOrNull { it.id == parentId }
  val isSearching = query.isNotBlank()
  val shownCategories =
      when {
        isSearching -> searchCategorias(categorias, query)
        parent != null -> listOf(parent) + tree.childrenByParent[parent.id].orEmpty()
        else -> tree.roots
      }
  val paneTitle = stringResource(Res.string.category)
  val contentState =
      CategoryPickerContentState(
          query = query,
          parent = parent,
          shownCategories = shownCategories,
          selectedId = selectedId,
          paneTitle = paneTitle,
      )

  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = paneTitle,
      onDismiss = onDismiss,
      modifier = modifier,
  ) {
    CategoryPickerContent(
        state = contentState,
        onQueryChange = { value ->
          query = value
          if (value.isNotBlank()) parentId = null
        },
        onBack = { parentId = null },
        onCategoryClick = { categoria ->
          val hasChildren =
              !isSearching &&
                  parent == null &&
                  tree.childrenByParent[categoria.id].orEmpty().isNotEmpty()
          if (hasChildren) {
            parentId = categoria.id
          } else {
            onSelect(categoria.id)
            onDismiss()
          }
        },
    )
  }
}

private data class CategoryPickerContentState(
    val query: String,
    val parent: Categoria?,
    val shownCategories: List<Categoria>,
    val selectedId: String?,
    val paneTitle: String,
) {
  val isSearching: Boolean
    get() = query.isNotBlank()
}

@Composable
private fun CategoryPickerContent(
    state: CategoryPickerContentState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onCategoryClick: (Categoria) -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    CategoryPickerHeader(
        paneTitle = state.paneTitle,
        parent = state.parent.takeUnless { state.isSearching },
        onBack = onBack,
    )
    CategoryPickerSearchField(query = state.query, onQueryChange = onQueryChange)
    CategoryPickerResults(state = state, onCategoryClick = onCategoryClick)
  }
}

@Composable
private fun CategoryPickerHeader(
    paneTitle: String,
    parent: Categoria?,
    onBack: () -> Unit,
) {
  if (parent != null) CategoryPickerBackButton(onClick = onBack)
  Text(
      text = parent?.nombre ?: paneTitle,
      modifier = Modifier.semantics { heading() },
      style = MaterialTheme.typography.titleLarge,
  )
}

@Composable
private fun CategoryPickerSearchField(query: String, onQueryChange: (String) -> Unit) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      label = { Text(stringResource(Res.string.search_categories)) },
  )
}

@Composable
private fun CategoryPickerResults(
    state: CategoryPickerContentState,
    onCategoryClick: (Categoria) -> Unit,
) {
  if (state.shownCategories.isEmpty() && state.isSearching) {
    Text(
        text = stringResource(Res.string.no_categories_found),
        modifier = Modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
  } else {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
  TextButton(onClick = onClick) { Text(stringResource(Res.string.back)) }
}
