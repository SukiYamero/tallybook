package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules

/** The custom categories sorted by name, and each predefined family in catalog order. */
internal data class CategoryBlocks(
    val custom: List<Categoria>,
    val families: List<List<Categoria>>,
) {
  val groups: List<List<Categoria>>
    get() = (listOf(custom) + families).filter { it.isNotEmpty() }
}

/**
 * Splits [catalog] into [CategoryBlocks]. Grouping runs before [excludedIds] are dropped so an
 * excluded parent never promotes its children into another family.
 */
internal fun categoryBlocks(
    catalog: List<Categoria>,
    excludedIds: Set<String> = emptySet(),
): CategoryBlocks {
  val (custom, predefined) =
      categoryFamilies(catalog).partition { it.first().origen == CategoriaOrigen.USUARIO }
  fun List<Categoria>.visible() = filterNot { it.id in excludedIds }
  return CategoryBlocks(
      custom = custom.flatten().sortedBy { CategoriaRules.nombreKey(it.nombre) }.visible(),
      families = predefined.map { it.visible() }.filter { it.isNotEmpty() },
  )
}

/**
 * The tag blocks a category list shows: [CategoryBlocks.groups] or, while [query] is not blank, a
 * single flat block of every match in that same order.
 */
internal fun categoryGroups(
    catalog: List<Categoria>,
    excludedIds: Set<String>,
    query: String,
): List<List<Categoria>> {
  val groups = categoryBlocks(catalog, excludedIds).groups
  if (query.isBlank()) return groups
  val matches = searchCategorias(groups.flatten(), query)
  return if (matches.isEmpty()) emptyList() else listOf(matches)
}
