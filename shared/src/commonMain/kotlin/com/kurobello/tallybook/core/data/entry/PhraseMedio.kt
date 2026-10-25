package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.MedioPago

internal sealed interface MedioWord {
  data class Explicit(val medio: MedioPago) : MedioWord

  /** "con tarjeta": a card without saying which kind. */
  data object AnyCard : MedioWord

  data object NotMedio : MedioWord
}

/** [medio] is decided only when the words agree; otherwise [candidates] holds what to offer. */
internal data class PhraseMedio(val medio: MedioPago?, val candidates: Set<MedioPago>)

private val cardMedios: Set<MedioPago> = setOf(MedioPago.DEBITO, MedioPago.CREDITO)

/** One method mention: overlapping phrases ("con tarjeta" + "tarjeta de crédito") merged. */
private data class MedioMention(val start: Int, val endExclusive: Int, val word: MedioWord)

private data class MedioHit(val start: Int, val endExclusive: Int, val word: MedioWord) {
  val length: Int
    get() = endExclusive - start
}

// On a length tie the phrase naming the method wins over the generic card that overlaps it.
private val hitPriority: Comparator<MedioHit> =
    compareBy<MedioHit> { it.length }.thenBy { it.word is MedioWord.Explicit }

/**
 * Finds payment-method words and consumes them so they leave the subject. Negated mentions are
 * consumed too but decide nothing; a card that is the object being paid stays in the subject.
 */
internal fun consumePhraseMedio(cursor: PhraseCursor): PhraseMedio {
  val mentions = mergedMentions(cursor)
  val words = mutableListOf<MedioWord>()
  mentions.forEach { mention ->
    when {
      isNegatedAt(cursor, mention.start) -> cursor.consume(mention.start - 1, mention.endExclusive)
      mention.word == MedioWord.NotMedio || isPaidCard(cursor, mention) -> Unit
      else -> {
        words += mention.word
        cursor.consume(mention.start, mention.endExclusive)
      }
    }
  }
  consumeNegatedCardNouns(cursor)
  return decide(words)
}

private fun mergedMentions(cursor: PhraseCursor): List<MedioMention> {
  val hits =
      cursor.tokens.indices.mapNotNull { index ->
        medioWords.matchAt(cursor, index)?.let { MedioHit(index, index + it.length, it.value) }
      }
  val groups = mutableListOf<MutableList<MedioHit>>()
  hits.forEach { hit ->
    val last = groups.lastOrNull()
    if (last != null && hit.start < last.maxOf(MedioHit::endExclusive)) last += hit
    else groups += mutableListOf(hit)
  }
  return groups.map { group ->
    MedioMention(
        start = group.first().start,
        endExclusive = group.maxOf(MedioHit::endExclusive),
        word = group.maxWith(hitPriority).word,
    )
  }
}

private fun isNegatedAt(cursor: PhraseCursor, start: Int): Boolean =
    rawWordAt(cursor, start - 1) in negationWords

private fun isPaidCard(cursor: PhraseCursor, mention: MedioMention): Boolean {
  if (rawWordAt(cursor, mention.start) !in cardLeadWords) return false
  val previous = rawWordAt(cursor, mention.start - 1)
  val isDeterminedObject =
      previous in cardDeterminers && rawWordAt(cursor, mention.start - 2) !in methodPrepositions
  return previous in cardObjectMarkers ||
      isDeterminedObject ||
      rawWordAt(cursor, mention.endExclusive) in cardObjectFollowers
}

private fun consumeNegatedCardNouns(cursor: PhraseCursor) {
  for (index in 0 until cursor.tokens.size - 1) {
    if (cursor.wordAt(index) in negationWords && cursor.wordAt(index + 1) in cardNouns) {
      cursor.consume(index, index + 2)
    }
  }
}

// Context checks read consumed tokens too: "Pagué la tarjeta" has its verb consumed already.
private fun rawWordAt(cursor: PhraseCursor, index: Int): String? =
    cursor.tokens.getOrNull(index)?.takeIf { it.kind == PhraseTokenKind.WORD }?.norm

private fun decide(words: List<MedioWord>): PhraseMedio {
  val explicit = words.filterIsInstance<MedioWord.Explicit>().map(MedioWord.Explicit::medio)
  val namesAnyCard = MedioWord.AnyCard in words
  val impliedCards =
      if (namesAnyCard && explicit.none(cardMedios::contains)) cardMedios else emptySet()
  val offered = explicit.toSet() + impliedCards
  val medio = offered.singleOrNull()
  return PhraseMedio(medio, if (medio == null) offered else emptySet())
}
