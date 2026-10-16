package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.text.matchesNormalizedSearchText
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.movimiento.rowDescription
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.core.ui.movimiento.rowTitulo
import com.kurobello.tallybook.feature.search.ui.SearchResultRowUi
import com.kurobello.tallybook.feature.search.ui.searchCategoryLabel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SearchCandidate(val row: SearchResultRowUi, val normalizedFields: List<String>)

internal suspend fun buildSearchCandidates(
    movimientos: List<Movimiento>,
    categorias: List<Categoria>,
    unknownCategoryLabel: String,
): List<SearchCandidate> {
  val context = currentCoroutineContext()
  val categoriesById = categorias.associateBy { it.id }
  return movimientos.map { movimiento ->
    context.ensureActive()
    val categoria = categoriesById[movimiento.categoriaId]
    val categoryLabel = searchCategoryLabel(categoria, unknownCategoryLabel)
    SearchCandidate(
        SearchResultRowUi(movimiento, categoria, movimiento.rowTitle(categoryLabel), categoryLabel),
        listOfNotNull(movimiento.rowTitulo, movimiento.rowDescription, categoryLabel)
            .map(::normalizeSearchText),
    )
  }
}

internal suspend fun matchSearchCandidates(
    candidates: List<SearchCandidate>,
    normalizedQuery: String,
): List<SearchResultRowUi> {
  val context = currentCoroutineContext()
  return buildList {
    for (candidate in candidates) {
      context.ensureActive()
      if (matchesNormalizedSearchText(normalizedQuery, candidate.normalizedFields))
          add(candidate.row)
    }
  }
}
