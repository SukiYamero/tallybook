package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.Categoria

/**
 * Groups [categories] into families: each root (or orphan whose parent is missing) followed by its
 * descendants, in the catalog's order. Every category appears exactly once, so an imported cycle
 * stays reachable instead of vanishing.
 */
internal fun categoryFamilies(categories: List<Categoria>): List<List<Categoria>> {
  val ids = categories.mapTo(mutableSetOf()) { it.id }
  val children = categories.groupBy { it.padreId }
  val visited = mutableSetOf<String>()
  fun descendants(category: Categoria): List<Categoria> =
      if (!visited.add(category.id)) emptyList()
      else listOf(category) + children[category.id].orEmpty().flatMap(::descendants)
  val roots = categories.filter { it.padreId == null || it.padreId !in ids }
  return (roots + categories).mapNotNull { descendants(it).takeIf(List<Categoria>::isNotEmpty) }
}
