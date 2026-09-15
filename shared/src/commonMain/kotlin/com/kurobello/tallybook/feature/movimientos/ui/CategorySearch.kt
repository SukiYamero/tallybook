package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria

internal fun searchCategorias(categorias: List<Categoria>, query: String): List<Categoria> {
  val normalizedQuery = query.normalizedForCategorySearch()
  if (normalizedQuery.isEmpty()) return emptyList()

  return categorias.filter { categoria ->
    categoria.nombre.normalizedForCategorySearch().contains(normalizedQuery)
  }
}

private fun String.normalizedForCategorySearch(): String =
    trim()
        .lowercase()
        .replace("á", "a")
        .replace("à", "a")
        .replace("â", "a")
        .replace("ã", "a")
        .replace("ä", "a")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("ë", "e")
        .replace("í", "i")
        .replace("ï", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("õ", "o")
        .replace("ö", "o")
        .replace("ú", "u")
        .replace("ü", "u")
        .replace("ç", "c")
