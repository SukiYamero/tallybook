package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.ui.category.categoryFamilies

/**
 * The tag blocks the picker shows: the custom categories as one block sorted by name, then one
 * block per predefined family (parent first, then its children) or, while [query] is not blank, a
 * single flat block of every match. Grouping runs before [excludedIds] are dropped so an excluded
 * parent never promotes its children into another family.
 */
internal fun categoryPickerGroups(
    catalog: List<Categoria>,
    excludedIds: Set<String>,
    query: String,
): List<List<Categoria>> {
  val (custom, predefined) =
      categoryFamilies(catalog).partition { it.first().origen == CategoriaOrigen.USUARIO }
  val customBlock = custom.flatten().sortedBy { CategoriaRules.nombreKey(it.nombre) }
  val groups =
      (listOf(customBlock) + predefined)
          .map { family -> family.filterNot { it.id in excludedIds } }
          .filter { it.isNotEmpty() }
  if (query.isBlank()) return groups
  val matches = searchCategorias(groups.flatten(), query)
  return if (matches.isEmpty()) emptyList() else listOf(matches)
}

/** The selected categories in selection order; ids missing from [catalog] are skipped. */
internal fun selectedCategorias(
    catalog: List<Categoria>,
    selectedIds: List<String>,
): List<Categoria> {
  val byId = catalog.associateBy(Categoria::id)
  return selectedIds.mapNotNull(byId::get)
}
