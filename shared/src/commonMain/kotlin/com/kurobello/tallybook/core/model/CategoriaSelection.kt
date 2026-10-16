package com.kurobello.tallybook.core.model

/** Pure rules for the set of categories attached to one movimiento while it is being edited. */
object CategoriaSelection {

  /**
   * Appends [id], first removing its parent or its children (they never coexist with it). Ignores
   * the id when it is already present or when appending would exceed [limit]; a replacement never
   * grows the list, so it is accepted at the limit.
   */
  fun add(
      current: List<String>,
      id: String,
      catalog: List<Categoria>,
      limit: Int = MAX_CATEGORIAS_POR_MOVIMIENTO,
  ): List<String> {
    if (id in current) return current
    val parentId = catalog.firstOrNull { it.id == id }?.padreId
    val childIds = catalog.filter { it.padreId == id }.mapTo(mutableSetOf(), Categoria::id)
    val remaining = current.filterNot { it == parentId || it in childIds }
    val appended = normalized(remaining + id)
    return if (remaining.size >= limit || id !in appended) current else appended
  }

  /** Removes [id] unless it is the only category left, so a selection never becomes empty. */
  fun remove(current: List<String>, id: String): List<String> =
      if (id in current && current.size > 1) current - id else current

  /**
   * Moves [current] toward [target] by removing first and adding after, so a swap at [limit]
   * succeeds. Every rule above still applies to a selection reported as a whole list: removals may
   * only empty the selection when an addition follows. Ids outside [catalog] and the Otros fallback
   * are never added.
   */
  fun reconcile(
      current: List<String>,
      target: List<String>,
      catalog: List<Categoria>,
      limit: Int = MAX_CATEGORIAS_POR_MOVIMIENTO,
  ): List<String> {
    val known = catalog.mapTo(mutableSetOf(), Categoria::id) - CATEGORIA_OTROS_ID
    val additions = target.filter { it in known && it !in current }
    val removed =
        current
            .filterNot { it in target }
            .fold(current) { selection, id ->
              if (additions.isEmpty()) remove(selection, id) else selection - id
            }
    return additions.fold(removed) { selection, id -> add(selection, id, catalog, limit) }
  }

  /** Drops the Otros fallback as soon as a real category is present and removes duplicates. */
  fun normalized(current: List<String>): List<String> {
    val distinct = current.distinct()
    return distinct.filter { it != CATEGORIA_OTROS_ID }.ifEmpty { distinct }
  }
}
