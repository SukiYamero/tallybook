package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.text.isAsciiDigit
import com.kurobello.tallybook.core.text.normalizeSearchText

internal fun tokenizePhrase(text: String): List<PhraseToken> {
  val tokens = mutableListOf<PhraseToken>()
  var index = 0
  while (index < text.length) {
    val character = text[index]
    val token =
        when {
          character.isWhitespace() -> null
          character.isAsciiDigit() -> scanNumber(text, index)
          character.isWordCharacter() -> scanWord(text, index)
          character == '$' -> rawToken(PhraseTokenKind.SYMBOL, text, index, index + 1)
          else -> rawToken(PhraseTokenKind.PUNCT, text, index, index + 1)
        }
    token?.let(tokens::add)
    index = token?.end ?: (index + 1)
  }
  return tokens
}

private fun scanNumber(text: String, start: Int): PhraseToken {
  val integerEnd = scanDigits(text, start)
  val slashDateEnd = scanSlashDate(text, integerEnd)
  if (slashDateEnd != null) return rawToken(PhraseTokenKind.SLASH_DATE, text, start, slashDateEnd)
  var end = integerEnd
  // Space groups only continue a number that is itself grouped from a 1–3 digit lead, so
  // "matrícula 2025 500 mil" keeps the year apart from the amount.
  var spaceGroupsAllowed = integerEnd - start <= GROUP_DIGITS
  while (true) {
    val isSpaceGroup = spaceGroupsAllowed && text.isSpaceGroupAt(end)
    if (!isSpaceGroup && !text.isSeparatorBeforeDigit(end)) break
    spaceGroupsAllowed = isSpaceGroup
    end = scanDigits(text, end + 1)
  }
  val raw = text.substring(start, end)
  return PhraseToken(PhraseTokenKind.NUMBER, start, end, raw, raw.filterNot { it in spaceGroups })
}

private fun String.isSeparatorBeforeDigit(index: Int): Boolean =
    getOrNull(index) in numberSeparators && getOrNull(index + 1)?.isAsciiDigit() == true

// Dictation writes "50 000": a space starts a thousands group only before exactly three digits,
// so "cine 2 12" stays two numbers.
private fun String.isSpaceGroupAt(index: Int): Boolean =
    getOrNull(index) in spaceGroups &&
        (1..GROUP_DIGITS).all { getOrNull(index + it)?.isAsciiDigit() == true } &&
        getOrNull(index + GROUP_DIGITS + 1)?.isAsciiDigit() != true

private fun scanSlashDate(text: String, afterFirst: Int): Int? {
  var end = afterFirst
  var parts = 1
  while (
      parts < MAX_DATE_PARTS &&
          text.getOrNull(end) == '/' &&
          text.getOrNull(end + 1)?.isAsciiDigit() == true
  ) {
    end = scanDigits(text, end + 1)
    parts++
  }
  return end.takeIf { parts > 1 }
}

private fun scanDigits(text: String, start: Int): Int {
  var end = start
  while (text.getOrNull(end)?.isAsciiDigit() == true) end++
  return end
}

private fun scanWord(text: String, start: Int): PhraseToken {
  var end = start
  while (end < text.length && isWordPart(text, end)) end++
  val word = text.substring(start, end)
  // "R$" and "S/" are single currency symbols made of a letter plus punctuation.
  val suffix = currencyLetterSuffixes[word.lowercase()]
  return if (suffix != null && text.getOrNull(end) == suffix) {
    rawToken(PhraseTokenKind.SYMBOL, text, start, end + 1)
  } else {
    PhraseToken(PhraseTokenKind.WORD, start, end, word, normalizeSearchText(word))
  }
}

private fun isWordPart(text: String, index: Int): Boolean {
  val character = text[index]
  val isInnerApostrophe =
      character in apostrophes &&
          text.getOrNull(index - 1)?.isWordCharacter() == true &&
          text.getOrNull(index + 1)?.isWordCharacter() == true
  return character.isWordCharacter() || isInnerApostrophe
}

private fun rawToken(kind: PhraseTokenKind, text: String, start: Int, end: Int): PhraseToken {
  val raw = text.substring(start, end)
  return PhraseToken(kind, start, end, raw, raw)
}

private fun Char.isWordCharacter(): Boolean =
    isLetter() || category == CharCategory.NON_SPACING_MARK

private const val MAX_DATE_PARTS = 3
private const val GROUP_DIGITS = 3
private val numberSeparators = setOf('.', ',')
private val spaceGroups = setOf(' ', ' ', ' ')
private val apostrophes = setOf('\'', '’')
private val currencyLetterSuffixes = mapOf("r" to '$', "s" to '/')
