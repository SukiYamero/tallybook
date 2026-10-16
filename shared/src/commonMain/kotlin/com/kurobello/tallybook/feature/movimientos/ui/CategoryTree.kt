package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria

data class CategoryTree(
    val roots: List<Categoria>,
    val childrenByParent: Map<String, List<Categoria>>,
)

internal fun buildCategoryTree(categorias: List<Categoria>): CategoryTree {
  val categoryIds = categorias.mapTo(mutableSetOf()) { it.id }
  val roots = mutableListOf<Categoria>()
  val childrenByParent = mutableMapOf<String, MutableList<Categoria>>()

  categorias.forEach { categoria ->
    val parentId = categoria.padreId
    if (parentId == null || parentId !in categoryIds) {
      roots += categoria
    } else {
      childrenByParent.getOrPut(parentId) { mutableListOf() } += categoria
    }
  }

  return CategoryTree(roots = roots, childrenByParent = childrenByParent)
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
    isSearching: Boolean,
    parent: Categoria?,
): Boolean = !isSearching && parent == null && childrenByParent[categoria.id].orEmpty().isNotEmpty()
