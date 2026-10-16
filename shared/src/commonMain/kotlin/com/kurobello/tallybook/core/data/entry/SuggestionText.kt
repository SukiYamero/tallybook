package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.text.normalizeSearchText

/**
 * Normalized, stop-word-free, singularized words of [text], in order. Dictionary phrases and
 * history descriptions go through the same function, so both sides of a comparison agree on a form
 * even where the plural heuristic is imperfect.
 */
internal fun suggestionTokens(text: String): List<String> =
    normalizeSearchText(text)
        .split(NON_LETTERS)
        .filter { it.length >= MIN_TOKEN_LENGTH && it !in STOP_WORDS }
        .map(::reducePlural)

/** Order-insensitive identity of a subject, used for exact history matches. */
internal fun subjectKey(tokens: List<String>): String = tokens.sorted().joinToString(" ")

/**
 * Handles regular Spanish, Portuguese and English plurals. An "-es" suffix is removed whole only
 * after l, r or d preceded by a vowel (hoteles, doctores, ciudades) or in "-iones"; elsewhere only
 * the "s" goes, so singulars such as cine, mueble and restaurante keep their own form.
 */
internal fun reducePlural(token: String): String =
    when {
      token.length <= MIN_PLURAL_LENGTH -> token
      token.endsWith("ss") || token.endsWith("us") -> token
      token.endsWith("iones") -> token.dropLast(2)
      token.endsWith("es") && hasVowelLiquidStem(token) -> token.dropLast(2)
      token.endsWith("s") -> token.dropLast(1)
      else -> token
    }

private fun hasVowelLiquidStem(token: String): Boolean =
    token.length > MIN_ES_STEM_LENGTH &&
        token[token.length - ES_CONSONANT_OFFSET] in "lrd" &&
        token[token.length - ES_VOWEL_OFFSET] in "aeiou"

private val NON_LETTERS = Regex("[^\\p{L}]+")

private const val MIN_TOKEN_LENGTH = 2
private const val MIN_PLURAL_LENGTH = 3
private const val MIN_ES_STEM_LENGTH = 4
private const val ES_CONSONANT_OFFSET = 3
private const val ES_VOWEL_OFFSET = 4

private val STOP_WORDS: Set<String> =
    """
    de del en el la los las un una unos unas para por con al mi mis lo se su sus sin pa otro otra
    of the on for at and an to in my with from or
    no na nos nas do da dos das em com pra pro uma um umas uns meu minha
    """
        .trim()
        .split(Regex("\\s+"))
        .toSet()
