package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.text.normalizeSearchText

internal fun searchCategorias(categorias: List<Categoria>, query: String): List<Categoria> {
  val normalizedQuery = normalizeSearchText(query)
  if (normalizedQuery.isEmpty()) return emptyList()

  return categorias.filter { categoria ->
    normalizeSearchText(categoria.nombre).contains(normalizedQuery)
  }
}
