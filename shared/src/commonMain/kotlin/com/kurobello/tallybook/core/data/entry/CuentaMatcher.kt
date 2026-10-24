package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Cuenta

/** An account named in a sentence over the raw characters `[start, endExclusive)`. */
internal data class CuentaMatch(val cuentaId: String, val start: Int, val endExclusive: Int)

/**
 * Finds accounts named in a sentence with the same whole-word rule as [EtiquetaMatcher]. Where two
 * names overlap ("Banco" inside "Banco Pichincha") the earliest, then the longest, owns the words;
 * accounts sharing one name in different currencies all match the same span.
 */
internal class CuentaMatcher {

  /** Live non-archived accounts whose full name appears as whole words, earliest first. */
  fun match(text: String, cuentas: List<Cuenta>): List<CuentaMatch> {
    val words = searchWords(text)
    if (words.isEmpty()) return emptyList()
    val occurrences =
        cuentas
            .filterNot(Cuenta::archivada)
            .flatMap { cuenta ->
              val name = nameWords(cuenta.nombre)
              words.startsOf(name).map { start -> WordSpan(cuenta.id, start, start + name.size) }
            }
            .sortedWith(compareBy<WordSpan> { it.start }.thenByDescending { it.endExclusive })
    val accepted = mutableListOf<WordSpan>()
    occurrences.forEach { span -> if (accepted.none { it.conflictsWith(span) }) accepted += span }
    return accepted.distinctBy(WordSpan::cuentaId).map {
      CuentaMatch(it.cuentaId, words[it.start].start, words[it.endExclusive - 1].endExclusive)
    }
  }
}

private data class WordSpan(val cuentaId: String, val start: Int, val endExclusive: Int) {
  fun conflictsWith(other: WordSpan): Boolean {
    val isSameSpan = start == other.start && endExclusive == other.endExclusive
    return !isSameSpan && start < other.endExclusive && other.start < endExclusive
  }
}
