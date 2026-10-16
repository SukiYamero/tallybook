package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria

/** The selected categories in selection order; ids missing from [catalog] are skipped. */
internal fun selectedCategorias(
    catalog: List<Categoria>,
    selectedIds: List<String>,
): List<Categoria> {
  val byId = catalog.associateBy(Categoria::id)
  return selectedIds.mapNotNull(byId::get)
}
