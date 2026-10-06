package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_category_label
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.unknown_category

@Composable
internal fun SearchCategoryFilters(
    categories: List<Categoria>,
    selectedIds: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val unknownLabel = stringResource(Res.string.unknown_category)
  val groups =
      remember(categories, unknownLabel) {
        searchCategoryGroups(
            categories.map { it.copy(nombre = searchCategoryLabel(it, unknownLabel)) }
        )
      }
  val missingIds =
      remember(categories, selectedIds) { selectedIds - categories.mapTo(mutableSetOf()) { it.id } }
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    SearchFilterHeading(stringResource(Res.string.search_category_label))
    groups.forEach { group ->
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      ) {
        group.forEach { category ->
          CategoryChip(
              category,
              category.id in selectedIds,
              { onToggle(category.id) },
              enabled = enabled,
          )
        }
      }
    }
    missingIds.sorted().forEach { id ->
      val label = stringResource(Res.string.unknown_category)
      val removal = stringResource(Res.string.search_remove_filter, label)
      TallybookTextButton(
          { onToggle(id) },
          enabled = enabled,
          modifier =
              Modifier.semantics {
                contentDescription = removal
                selected = true
              },
      ) {
        TallybookText(label)
      }
    }
  }
}

@Composable
internal fun SearchFilterHeading(text: String) {
  TallybookText(
      text,
      modifier = Modifier.semantics { heading() },
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.label,
  )
}

internal fun searchCategoryGroups(categories: List<Categoria>): List<List<Categoria>> {
  val ids = categories.mapTo(mutableSetOf()) { it.id }
  val children = categories.groupBy { it.padreId }
  val visited = mutableSetOf<String>()
  fun descendants(category: Categoria): List<Categoria> =
      if (!visited.add(category.id)) emptyList()
      else listOf(category) + children[category.id].orEmpty().flatMap(::descendants)
  val roots = categories.filter { it.padreId == null || it.padreId !in ids }
  return (roots + categories).mapNotNull { descendants(it).takeIf(List<Categoria>::isNotEmpty) }
}
