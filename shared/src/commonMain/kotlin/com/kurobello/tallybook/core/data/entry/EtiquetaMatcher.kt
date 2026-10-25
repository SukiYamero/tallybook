package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Etiqueta

/**
 * Finds existing tags named in a sentence. A name matches only as whole words in the same order,
 * compared with the shared search normalization, so "Casa" never matches "casamiento". Stop words
 * are kept on both sides because they are part of a tag's full name.
 */
internal class EtiquetaMatcher {

  /** Ids of the [etiquetas] whose full name appears in [text], in order of appearance. */
  fun match(text: String, etiquetas: List<Etiqueta>): List<String> {
    val words = searchWords(text)
    if (words.isEmpty()) return emptyList()
    return etiquetas
        .mapNotNull { etiqueta ->
          words.startsOf(nameWords(etiqueta.nombre)).firstOrNull()?.let { it to etiqueta.id }
        }
        .sortedBy { (position, _) -> position }
        .map { (_, id) -> id }
  }
}
