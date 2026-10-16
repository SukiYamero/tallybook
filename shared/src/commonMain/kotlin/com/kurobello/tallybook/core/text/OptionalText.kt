package com.kurobello.tallybook.core.text

private val whitespaceRuns = Regex("\\s+")

/** Trims and collapses whitespace runs to one space; blank text becomes null. */
internal fun String.normalizeOptionalText(): String? =
    trim().replace(whitespaceRuns, " ").ifEmpty { null }
