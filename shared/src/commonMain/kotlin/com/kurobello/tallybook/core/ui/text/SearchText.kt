package com.kurobello.tallybook.core.ui.text

/** Supported-locale accent folding preserves ñ; it is not universal transliteration. */
internal fun normalizeSearchText(value: String): String {
  val text = value.trim().lowercase()
  return buildString(text.length) {
    var index = 0
    while (index < text.length) {
      val character = text[index]
      val next = text.getOrNull(index + 1)
      if (character == 'n' && next == '\u0303') {
        append('ñ')
        index += 2
      } else {
        append(composedAccentFold[character] ?: character)
        index += if (next != null && decomposedAccents[character]?.contains(next) == true) 2 else 1
      }
    }
  }
}

/** For occasional raw fields; snapshot consumers should use [matchesNormalizedSearchText]. */
internal fun matchesSearchText(query: String, fields: List<String>): Boolean {
  val normalizedQuery = normalizeSearchText(query)
  return normalizedQuery.isEmpty() ||
      fields.any { normalizeSearchText(it).contains(normalizedQuery) }
}

/** Inputs must already be normalized so repeated queries do not rebuild snapshot fields. */
internal fun matchesNormalizedSearchText(
    normalizedQuery: String,
    normalizedFields: List<String>,
): Boolean = normalizedQuery.isEmpty() || normalizedFields.any { it.contains(normalizedQuery) }

private val composedAccentFold: Map<Char, Char> =
    mapOf(
        'á' to 'a',
        'à' to 'a',
        'â' to 'a',
        'ã' to 'a',
        'ä' to 'a',
        'é' to 'e',
        'ê' to 'e',
        'ë' to 'e',
        'í' to 'i',
        'ï' to 'i',
        'ó' to 'o',
        'ô' to 'o',
        'õ' to 'o',
        'ö' to 'o',
        'ú' to 'u',
        'ü' to 'u',
        'ç' to 'c',
    )

private val decomposedAccents: Map<Char, String> =
    mapOf(
        'a' to "\u0301\u0300\u0302\u0303\u0308",
        'e' to "\u0301\u0302\u0308",
        'i' to "\u0301\u0308",
        'o' to "\u0301\u0302\u0303\u0308",
        'u' to "\u0301\u0308",
        'c' to "\u0327",
    )
