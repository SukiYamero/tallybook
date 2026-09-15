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
