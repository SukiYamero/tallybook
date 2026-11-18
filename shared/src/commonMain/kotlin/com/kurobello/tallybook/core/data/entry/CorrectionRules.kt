package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

internal sealed interface RuleOutcome {
  data object None : RuleOutcome

  /** One agreeing record, or rules that disagree across the sentence's words. */
  data class Offered(val values: Set<String>) : RuleOutcome

  data class Applied(val value: String) : RuleOutcome
}

internal val LEARNED_CAMPOS =
    setOf(
        CampoCorreccion.TIPO,
        CampoCorreccion.CATEGORIA,
        CampoCorreccion.MEDIO,
        CampoCorreccion.CUENTA,
        CampoCorreccion.FRECUENCIA,
    )

internal val RULE_WINDOW = 180.days

private const val MIN_WORD_LENGTH = 3
private const val MIN_AGREEING_RECORDS = 2

private data class WordField(val word: String, val campo: CampoCorreccion)

internal class CorrectionRules
private constructor(private val newestFirstByWordField: Map<WordField, List<CorreccionFrase>>) {
  /**
   * [leido] is the parser's own reading; a rule overrides it only if its records corrected that
   * reading.
   */
  fun outcome(words: List<String>, campo: CampoCorreccion, leido: String?): RuleOutcome {
    val perWord =
        words.distinct().map { word ->
          wordOutcome(newestFirstByWordField[WordField(word, campo)].orEmpty(), leido)
        }
    val applied = perWord.filterIsInstance<RuleOutcome.Applied>().map { it.value }.toSet()
    val offered = perWord.filterIsInstance<RuleOutcome.Offered>().flatMap { it.values }.toSet()
    return when {
      applied.isEmpty() && offered.isEmpty() -> RuleOutcome.None
      applied.size == 1 && offered.isEmpty() -> RuleOutcome.Applied(applied.single())
      else -> RuleOutcome.Offered(applied + offered)
    }
  }

  private fun wordOutcome(newestFirst: List<CorreccionFrase>, leido: String?): RuleOutcome {
    val value = newestFirst.firstOrNull()?.guardado ?: return RuleOutcome.None
    val agreeing = newestFirst.filter { it.guardado == value }
    val correctedThatReading =
        leido == null || agreeing.count { it.leido == leido } >= MIN_AGREEING_RECORDS
    return when {
      agreeing.size < MIN_AGREEING_RECORDS -> RuleOutcome.Offered(setOf(value))
      correctedThatReading -> RuleOutcome.Applied(value)
      else -> RuleOutcome.None
    }
  }

  companion object {
    val EMPTY = CorrectionRules(emptyMap())

    fun from(records: List<CorreccionFrase>, now: Instant): CorrectionRules {
      val windowStart = now - RULE_WINDOW
      val index = HashMap<WordField, MutableList<CorreccionFrase>>()
      records
          .filter { it.campo in LEARNED_CAMPOS && it.createdAt >= windowStart }
          .sortedWith(
              compareByDescending<CorreccionFrase> { it.createdAt }.thenByDescending { it.id }
          )
          .forEach { record ->
            record.palabras
                .filter { it.length >= MIN_WORD_LENGTH }
                .distinct()
                .forEach { word ->
                  index.getOrPut(WordField(word, record.campo)) { mutableListOf() }.add(record)
                }
          }
      return CorrectionRules(index)
    }
  }
}
