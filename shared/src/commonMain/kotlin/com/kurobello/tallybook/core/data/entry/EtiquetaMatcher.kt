package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.text.normalizeSearchText

/**
 * Finds existing tags named in a sentence. A name matches only as whole words in the same order,
 * compared with the shared search normalization, so "Casa" never matches "casamiento". Stop words
 * are kept on both sides because they are part of a tag's full name.
 */
internal class EtiquetaMatcher {

  /** Ids of the [etiquetas] whose full name appears in [text], in order of appearance. */
  fun match(text: String, etiquetas: List<Etiqueta>): List<String> {
    val words = words(text)
    if (words.isEmpty()) return emptyList()
    return etiquetas
        .mapNotNull { etiqueta ->
          words.indexOfSequence(words(etiqueta.nombre))?.let { position -> position to etiqueta.id }
        }
        .sortedBy { (position, _) -> position }
        .map { (_, id) -> id }
  }

  private fun words(value: String): List<String> =
      normalizeSearchText(value).split(WORD_SEPARATORS).filter(String::isNotEmpty)

  private fun List<String>.indexOfSequence(sequence: List<String>): Int? {
    if (sequence.isEmpty() || sequence.size > size) return null
    return (0..size - sequence.size).firstOrNull { start ->
      sequence.indices.all { offset -> this[start + offset] == sequence[offset] }
    }
  }
}

private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
