package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.text.matchesNormalizedSearchText
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.movimiento.rowDescription
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.core.ui.movimiento.rowTitulo
import com.kurobello.tallybook.feature.search.ui.SearchActividadRowUi
import com.kurobello.tallybook.feature.search.ui.SearchResultRowUi
import com.kurobello.tallybook.feature.search.ui.SearchTransferenciaRowUi
import com.kurobello.tallybook.feature.search.ui.searchCategoryLabel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SearchCandidate(
    val row: SearchActividadRowUi,
    val normalizedFields: List<String>,
)

/** A transfer is found by its description and either account's name. */
internal suspend fun buildSearchCandidates(
    actividades: List<Actividad>,
    categorias: List<Categoria>,
    etiquetas: List<Etiqueta>,
    unknownCategoryLabel: String,
    cuentas: List<Cuenta> = emptyList(),
): List<SearchCandidate> {
  val context = currentCoroutineContext()
  val categoriesById = categorias.associateBy { it.id }
  val etiquetaNamesById = etiquetas.associate { it.id to it.nombre }
  val cuentasById = cuentas.associateBy { it.id }
  return actividades.map { actividad ->
    context.ensureActive()
    when (actividad) {
      is Actividad.DeMovimiento -> {
        val movimiento = actividad.movimiento
        val categoria = categoriesById[movimiento.categoriaId]
        val categoryLabel = searchCategoryLabel(categoria, unknownCategoryLabel)
        SearchCandidate(
            SearchResultRowUi(
                movimiento,
                categoria,
                movimiento.rowTitle(categoryLabel),
                categoryLabel,
            ),
            (listOfNotNull(movimiento.rowTitulo, movimiento.rowDescription, categoryLabel) +
                    movimiento.etiquetaIds.mapNotNull(etiquetaNamesById::get))
                .map(::normalizeSearchText),
        )
      }
      is Actividad.DeTransferencia -> {
        val transferencia = actividad.transferencia
        val origen = cuentasById[transferencia.cuentaOrigenId]
        val destino = cuentasById[transferencia.cuentaDestinoId]
        SearchCandidate(
            SearchTransferenciaRowUi(actividad, origen, destino),
            listOfNotNull(transferencia.descripcion, origen?.nombre, destino?.nombre)
                .map(::normalizeSearchText),
        )
      }
    }
  }
}

internal suspend fun matchSearchCandidates(
    candidates: List<SearchCandidate>,
    normalizedQuery: String,
): List<SearchActividadRowUi> {
  val context = currentCoroutineContext()
  return buildList {
    for (candidate in candidates) {
      context.ensureActive()
      if (matchesNormalizedSearchText(normalizedQuery, candidate.normalizedFields))
          add(candidate.row)
    }
  }
}
