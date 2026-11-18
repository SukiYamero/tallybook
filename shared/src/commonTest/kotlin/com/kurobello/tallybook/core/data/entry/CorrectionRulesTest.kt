package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.OrigenFrase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CorrectionRulesTest {
  private val now = Instant.parse("2026-11-17T12:00:00Z")

  private var nextId = 0

  private fun record(
      words: List<String>,
      saved: String?,
      daysAgo: Int = 1,
      campo: CampoCorreccion = CampoCorreccion.MEDIO,
      read: String? = null,
  ) =
      CorreccionFrase(
          id = "r${nextId++}",
          origen = OrigenFrase.ESCRITO,
          palabras = words,
          campo = campo,
          leido = read,
          propuesto = null,
          guardado = saved,
          createdAt = now - daysAgo.days,
      )

  private fun rules(vararg records: CorreccionFrase) = CorrectionRules.from(records.toList(), now)

  private fun CorrectionRules.medio(words: List<String>, read: String? = null) =
      outcome(words, CampoCorreccion.MEDIO, read)

  @Test
  fun twoAgreeingCorrectionsApply() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 3),
            record(listOf("netflix"), "Crédito", daysAgo = 2),
        )

    assertEquals(RuleOutcome.Applied("Crédito"), rules.medio(listOf("pague", "netflix")))
  }

  @Test
  fun oneCorrectionIsOnlyOffered() {
    val rules = rules(record(listOf("netflix"), "Crédito"))

    assertEquals(RuleOutcome.Offered(setOf("Crédito")), rules.medio(listOf("netflix")))
  }

  @Test
  fun aNewerContradictionSuspendsTheRule() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 5),
            record(listOf("netflix"), "Crédito", daysAgo = 4),
            record(listOf("netflix"), "Débito", daysAgo = 1),
        )

    assertEquals(RuleOutcome.Offered(setOf("Débito")), rules.medio(listOf("netflix")))
  }

  @Test
  fun recordsOlderThan180DaysAreIgnored() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 181),
            record(listOf("netflix"), "Crédito", daysAgo = 200),
        )

    assertEquals(RuleOutcome.None, rules.medio(listOf("netflix")))
  }

  @Test
  fun shortWordsNeverFormRules() {
    val rules =
        rules(
            record(listOf("la"), "Crédito", daysAgo = 2),
            record(listOf("la"), "Crédito", daysAgo = 1),
        )

    assertEquals(RuleOutcome.None, rules.medio(listOf("la")))
  }

  @Test
  fun disagreeingWordsOfOneSentenceOfferBoth() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 2),
            record(listOf("netflix"), "Crédito", daysAgo = 1),
            record(listOf("uber"), "Débito", daysAgo = 2),
            record(listOf("uber"), "Débito", daysAgo = 1),
        )

    assertEquals(
        RuleOutcome.Offered(setOf("Crédito", "Débito")),
        rules.medio(listOf("netflix", "uber")),
    )
  }

  @Test
  fun anExplicitReadingIsOverriddenOnlyByCorrectionsOfThatReadingAndOfferedOtherwise() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", read = "Débito", daysAgo = 2),
            record(listOf("netflix"), "Crédito", read = "Débito", daysAgo = 1),
        )

    assertEquals(RuleOutcome.Applied("Crédito"), rules.medio(listOf("netflix"), read = "Débito"))
    assertEquals(
        RuleOutcome.Offered(setOf("Crédito")),
        rules.medio(listOf("netflix"), read = "Efectivo"),
    )
    assertEquals(RuleOutcome.Applied("Crédito"), rules.medio(listOf("netflix"), read = null))
  }

  @Test
  fun anOfferOfTheSameValueDoesNotSuspendARule() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 2),
            record(listOf("netflix"), "Crédito", daysAgo = 1),
            record(listOf("spotify"), "Crédito"),
        )

    assertEquals(RuleOutcome.Applied("Crédito"), rules.medio(listOf("netflix", "spotify")))
  }

  @Test
  fun anOfferOfAnotherValueTurnsARuleIntoAnOffer() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 2),
            record(listOf("netflix"), "Crédito", daysAgo = 1),
            record(listOf("uber"), "Débito"),
        )

    assertEquals(
        RuleOutcome.Offered(setOf("Crédito", "Débito")),
        rules.medio(listOf("netflix", "uber")),
    )
  }

  @Test
  fun aRecordExactly180DaysOldCounts() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 180),
            record(listOf("netflix"), "Crédito", daysAgo = 179),
        )

    assertEquals(RuleOutcome.Applied("Crédito"), rules.medio(listOf("netflix")))
  }

  @Test
  fun aNullLatestValueGivesNone() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", daysAgo = 3),
            record(listOf("netflix"), "Crédito", daysAgo = 2),
            record(listOf("netflix"), null, daysAgo = 1),
        )

    assertEquals(RuleOutcome.None, rules.medio(listOf("netflix")))
  }

  @Test
  fun recordsOfOneSaveCountAsOneCorrection() {
    val rules =
        rules(
            record(listOf("uber"), "Crédito", daysAgo = 1),
            record(listOf("uber"), "Crédito", daysAgo = 1),
        )

    assertEquals(RuleOutcome.Offered(setOf("Crédito")), rules.medio(listOf("uber")))
  }

  @Test
  fun recordsOfOneSaveCountOnceForTheReadingTheyCorrected() {
    val rules =
        rules(
            record(listOf("uber"), "Crédito", read = "Débito", daysAgo = 2),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 1),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 1),
        )

    assertEquals(
        RuleOutcome.Offered(setOf("Crédito")),
        rules.medio(listOf("uber"), read = "Efectivo"),
    )
  }

  @Test
  fun aRuleLearnedAgainstAnotherReadingDoesNotBlockAnAppliedWord() {
    val rules =
        rules(
            record(listOf("netflix"), "Débito", read = "Crédito", daysAgo = 2),
            record(listOf("netflix"), "Débito", read = "Crédito", daysAgo = 1),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 2),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 1),
        )

    assertEquals(
        RuleOutcome.Applied("Crédito"),
        rules.medio(listOf("netflix", "uber"), read = "Efectivo"),
    )
  }

  @Test
  fun aDuplicatedWordIsNotCountedTwice() {
    val rules = rules(record(listOf("netflix", "netflix"), "Crédito"))

    assertEquals(RuleOutcome.Offered(setOf("Crédito")), rules.medio(listOf("netflix", "netflix")))
  }

  @Test
  fun aGatedOutWordDoesNotBlockAnotherAppliedWord() {
    val rules =
        rules(
            record(listOf("netflix"), "Crédito", read = "Débito", daysAgo = 2),
            record(listOf("netflix"), "Crédito", read = "Débito", daysAgo = 1),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 2),
            record(listOf("uber"), "Crédito", read = "Efectivo", daysAgo = 1),
        )

    assertEquals(
        RuleOutcome.Applied("Crédito"),
        rules.medio(listOf("netflix", "uber"), read = "Efectivo"),
    )
  }

  @Test
  fun unlearnedCamposNeverFormRules() {
    val rules =
        rules(
            record(listOf("netflix"), "10", daysAgo = 2, campo = CampoCorreccion.MONTO),
            record(listOf("netflix"), "10", daysAgo = 1, campo = CampoCorreccion.MONTO),
        )

    assertEquals(RuleOutcome.None, rules.outcome(listOf("netflix"), CampoCorreccion.MONTO, null))
  }
}
