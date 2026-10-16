package com.kurobello.tallybook.core.ui.tag

import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.text.normalizeSearchText

/** Removes a selected [id], or appends it while fewer than [max] tags are selected. */
internal fun toggledEtiquetaIds(selectedIds: List<String>, id: String, max: Int): List<String> =
    when {
      id in selectedIds -> selectedIds - id
      selectedIds.size >= max -> selectedIds
      else -> selectedIds + id
    }

/** Tags whose name contains [query] after the shared search normalization; blank keeps them all. */
internal fun searchEtiquetas(etiquetas: List<Etiqueta>, query: String): List<Etiqueta> {
  val normalizedQuery = normalizeSearchText(query)
  if (normalizedQuery.isEmpty()) return etiquetas
  return etiquetas.filter { normalizeSearchText(it.nombre).contains(normalizedQuery) }
}
