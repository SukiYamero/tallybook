package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

internal sealed interface RuleOutcome {
  data object None : RuleOutcome

  /**
   * One agreeing save, a rule learned against another reading than this sentence's, or rules that
   * disagree across the sentence's words.
   */
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
private const val MIN_AGREEING_SAVES = 2

private data class WordField(val word: String, val campo: CampoCorreccion)

private sealed interface WordRule {
  val value: String

  data class Single(override val value: String) : WordRule

  data class OtherReading(override val value: String) : WordRule

  data class Rule(override val value: String) : WordRule
}

internal class CorrectionRules
private constructor(private val newestFirstByWordField: Map<WordField, List<CorreccionFrase>>) {
  /**
   * [leido] is the parser's own reading; a rule overrides it only if its records corrected that
   * reading.
   */
  fun outcome(words: List<String>, campo: CampoCorreccion, leido: String?): RuleOutcome {
    val perWord =
        words.distinct().mapNotNull { word ->
          wordRule(newestFirstByWordField[WordField(word, campo)].orEmpty(), leido)
        }
    val applied = perWord.valuesOf<WordRule.Rule>()
    val offered = perWord.valuesOf<WordRule.Single>()
    return when {
      perWord.isEmpty() -> RuleOutcome.None
      applied.size == 1 && (offered - applied).isEmpty() -> RuleOutcome.Applied(applied.single())
      else -> RuleOutcome.Offered(perWord.map { it.value }.toSet())
    }
  }

  /**
   * Agreement is counted in saves: every record of one save shares its `createdAt`, and a split
   * sentence writes one record per part, so a word repeated across parts would count twice.
   */
  private fun wordRule(newestFirst: List<CorreccionFrase>, leido: String?): WordRule? {
    val value = newestFirst.firstOrNull()?.guardado ?: return null
    val agreeing = newestFirst.filter { it.guardado == value }
    val correctedThatReading =
        leido == null || agreeing.filter { it.leido == leido }.saveCount() >= MIN_AGREEING_SAVES
    return when {
      agreeing.saveCount() < MIN_AGREEING_SAVES -> WordRule.Single(value)
      correctedThatReading -> WordRule.Rule(value)
      else -> WordRule.OtherReading(value)
    }
  }

  private fun List<CorreccionFrase>.saveCount(): Int = distinctBy { it.createdAt }.size

  private inline fun <reified R : WordRule> List<WordRule>.valuesOf(): Set<String> =
      filterIsInstance<R>().map { it.value }.toSet()

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
