package com.kurobello.tallybook.core.data.entry

/**
 * Words are comma separated and may wrap across lines; each phrase is normalized by
 * [CategoryKeywordDictionary], so accents, case and regular plurals need not be repeated.
 */
internal fun keywords(
    categoriaId: String,
    strong: String = "",
    weak: String = "",
): List<CategoryKeywordEntry> =
    splitKeywords(strong).map { CategoryKeywordEntry(it, categoriaId, KeywordWeight.STRONG) } +
        splitKeywords(weak).map { CategoryKeywordEntry(it, categoriaId, KeywordWeight.WEAK) }

private fun splitKeywords(block: String): List<String> =
    block.replace(WHITESPACE, " ").split(',').map { it.trim() }.filter { it.isNotEmpty() }

private val WHITESPACE = Regex("\\s+")
