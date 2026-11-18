package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.entry.CategoryHistoryEntry
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CategorySuggestionIndex
import com.kurobello.tallybook.core.data.entry.CorrectionRules
import com.kurobello.tallybook.core.data.entry.CuentaMatcher
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.OrigenFrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate

class MovimientoEntryEngineRulesTest {

  private val now = Instant.parse("2026-10-15T00:00:00Z")

  private val suggester = CategorySuggester(CategoryKeywordDictionary.builtIn)

  private val engine =
      MovimientoEntryEngine(
          parser = MovimientoPhraseParser(),
          suggester = suggester,
          etiquetaMatcher = EtiquetaMatcher(),
          cuentaMatcher = CuentaMatcher(),
          localeProvider =
              object : DeviceLocaleProvider {
                override fun currentLanguageTag(): String = "es-CO"
              },
          computationDispatcher = Dispatchers.Unconfined,
      )

  private val context = PhraseContext(Moneda.COP, AppLocale.ES_AR, LocalDate(2026, 10, 15))

  private val candidates =
      SuggestionCandidates(
          availableIds = CategorySeedCatalog.categories.map { it.id }.toSet(),
          excludedIds = emptySet(),
      )

  private var recordCount = 0

  private fun record(
      words: List<String>,
      campo: CampoCorreccion,
      guardado: String,
      leido: String? = null,
  ): CorreccionFrase {
    recordCount++
    return CorreccionFrase(
        id = "r$recordCount",
        origen = OrigenFrase.ESCRITO,
        palabras = words,
        campo = campo,
        leido = leido,
        propuesto = leido,
        guardado = guardado,
        createdAt = now - (100 - recordCount).days,
    )
  }

  private fun twice(
      words: List<String>,
      campo: CampoCorreccion,
      guardado: String,
      leido: String? = null,
  ) = List(2) { record(words, campo, guardado, leido) }

  private fun rulesOf(vararg records: List<CorreccionFrase>) =
      CorrectionRules.from(records.flatMap { it }, now)

  private fun interpret(
      text: String,
      rules: CorrectionRules = CorrectionRules.EMPTY,
      cuentas: List<Cuenta> = listOf(COP_CASH, NEQUI, NU),
      index: CategorySuggestionIndex = engine.dictionaryIndex(now),
  ): PhraseInterpretation =
      engine.interpret(
          text,
          context,
          index,
          candidates,
          SentenceCatalogs(
              emptyList(),
              SentenceCuentas(cuentas, Moneda.COP) { _, _ -> Moneda.COP },
              rules,
          ),
      )

  @Test
  fun aLearnedMedioFillsAnUndecidedSentence() {
    val rules = rulesOf(twice(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "CREDITO"))

    val sentence = interpret("almuerzo 20 mil con la visa", rules)

    assertNull(interpret("almuerzo 20 mil con la visa").sentence.medio.medio)
    assertEquals(MedioPago.CREDITO, sentence.sentence.medio.medio)
    assertTrue(sentence.sentence.medio.candidates.isEmpty())
  }

  @Test
  fun anOfferedMedioJoinsTheCandidates() {
    val rules =
        rulesOf(listOf(record(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "CREDITO")))

    val sentence = interpret("almuerzo 20 mil con la visa", rules)

    assertNull(sentence.sentence.medio.medio)
    assertTrue(MedioPago.CREDITO in sentence.sentence.medio.candidates)
    assertEquals(setOf(MedioPago.CREDITO), sentence.offered.medios)
  }

  @Test
  fun anExplicitMedioWinsOverAHabit() {
    val rules = rulesOf(twice(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "CREDITO"))

    val sentence = interpret("almuerzo 20 mil con la visa con débito", rules)

    assertEquals(MedioPago.DEBITO, sentence.sentence.medio.medio)
  }

  @Test
  fun aRuleOverridesTheReadingItCorrected() {
    val rules =
        rulesOf(
            twice(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "CREDITO", leido = "DEBITO")
        )

    val sentence = interpret("almuerzo 20 mil con la visa con débito", rules)

    assertEquals(MedioPago.CREDITO, sentence.sentence.medio.medio)
    assertEquals("DEBITO", sentence.readings.leido[CampoCorreccion.MEDIO])
    assertEquals("CREDITO", sentence.readings.propuesto[CampoCorreccion.MEDIO])
  }

  @Test
  fun aLearnedCategoryIsPreselected() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.CATEGORIA, "cat_mascotas"))

    assertNull(interpret("zumbido 10 mil").preselectedCategoriaId)
    assertEquals("cat_mascotas", interpret("zumbido 10 mil", rules).preselectedCategoriaId)
  }

  @Test
  fun aWordRuleBeatsADictionaryMatchItCorrected() {
    val rules =
        rulesOf(
            twice(
                listOf("almuerzo"),
                CampoCorreccion.CATEGORIA,
                "cat_comida",
                leido = "cat_restaurante",
            )
        )

    assertEquals("cat_restaurante", interpret("almuerzo 20 mil").preselectedCategoriaId)
    assertEquals("cat_comida", interpret("almuerzo 20 mil", rules).preselectedCategoriaId)
  }

  @Test
  fun anExactSubjectHistoryBeatsAWordRule() {
    val history =
        suggester.index(
            listOf(CategoryHistoryEntry("zumbido", "cat_regalo", now - 1.days)),
            now,
        )
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.CATEGORIA, "cat_mascotas"))

    val sentence = interpret("zumbido 10 mil", rules, index = history)

    assertEquals("cat_regalo", sentence.preselectedCategoriaId)
  }

  @Test
  fun aRuleForAHiddenCategoryIsIgnored() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.CATEGORIA, "cat_gone"))

    assertNull(interpret("zumbido 10 mil", rules).preselectedCategoriaId)
  }

  @Test
  fun anOfferedCategoryIsSuggestedNotSelected() {
    val rules =
        rulesOf(listOf(record(listOf("zumbido"), CampoCorreccion.CATEGORIA, "cat_mascotas")))

    val sentence = interpret("zumbido 10 mil", rules)

    assertNull(sentence.preselectedCategoriaId)
    assertEquals(listOf("cat_mascotas"), sentence.query.offeredCategoriaIds)
    assertEquals(
        "cat_mascotas",
        engine.dictionaryIndex(now).suggestedCategoriaIds(sentence.query, candidates).first(),
    )
  }

  @Test
  fun offeredCategoriesLeadTheSuggestionsWithinTheLimit() {
    val rules =
        rulesOf(listOf(record(listOf("almuerzo"), CampoCorreccion.CATEGORIA, "cat_mascotas")))
    val sentence = interpret("almuerzo 20 mil", rules)
    val index = engine.dictionaryIndex(now)
    val dictionary = index.ranked(sentence.query, candidates).map { it.categoriaId }

    assertEquals(
        (listOf("cat_mascotas") + dictionary).distinct().take(MAX_SUGGESTION_CHIPS + 1),
        index.suggestedCategoriaIds(sentence.query, candidates),
    )
  }

  @Test
  fun aDismissedOfferIsNotSuggested() {
    val rules =
        rulesOf(listOf(record(listOf("almuerzo"), CampoCorreccion.CATEGORIA, "cat_mascotas")))
    val dismissed = candidates.copy(excludedIds = setOf("cat_mascotas"))
    val query = interpret("almuerzo 20 mil", rules).query
    val index = engine.dictionaryIndex(now)

    assertEquals(
        index.ranked(query, dismissed).map { it.categoriaId },
        index.suggestedCategoriaIds(query, dismissed),
    )
  }

  @Test
  fun aTipoRuleRunsBeforeTheCategoryRanking() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.TIPO, "INGRESO"))
    val index = engine.dictionaryIndex(now)

    val sentence = interpret("zumbido 10 mil", rules)

    assertEquals(TipoMovimiento.GASTO, interpret("zumbido 10 mil").tipo)
    assertEquals(TipoMovimiento.INGRESO, sentence.tipo)
    assertEquals(TipoMovimiento.INGRESO, sentence.query.tipo)
    assertEquals(index.ranked(sentence.query, candidates), sentence.ranking)
  }

  @Test
  fun aTipoVerbWinsOverAHabit() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.TIPO, "INGRESO"))

    assertEquals(TipoMovimiento.GASTO, interpret("gasté 10 mil en zumbido", rules).tipo)
  }

  @Test
  fun aLearnedCuentaFillsAndImpliesItsMedio() {
    val rules =
        rulesOf(
            twice(listOf("almuerzo", "visa"), CampoCorreccion.CUENTA, NU.id),
            twice(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "EFECTIVO"),
        )

    val sentence = interpret("almuerzo 20 mil con la visa", rules)

    assertEquals(NU, sentence.sentence.cuenta.cuenta)
    assertEquals(MedioPago.CREDITO, sentence.sentence.medio.medio)
  }

  @Test
  fun aLearnedMedioAdaptsToALearnedBankCuenta() {
    val rules =
        rulesOf(
            twice(listOf("arriendo"), CampoCorreccion.CUENTA, NEQUI.id),
            twice(listOf("arriendo"), CampoCorreccion.MEDIO, "TRANSFERENCIA"),
        )

    val sentence = interpret("arriendo 900 mil", rules)

    assertEquals(NEQUI, sentence.sentence.cuenta.cuenta)
    assertEquals(MedioPago.TRANSFERENCIA, sentence.sentence.medio.medio)
  }

  @Test
  fun aLearnedMedioReplacesTheOneANamedBankCuentaImplied() {
    val rules =
        rulesOf(twice(listOf("arriendo"), CampoCorreccion.MEDIO, "TRANSFERENCIA", leido = "DEBITO"))

    val sentence = interpret("arriendo 900 mil con Nequi", rules)

    assertEquals(NEQUI, sentence.sentence.cuenta.cuenta)
    assertEquals("DEBITO", sentence.readings.leido[CampoCorreccion.MEDIO])
    assertEquals(MedioPago.TRANSFERENCIA, sentence.sentence.medio.medio)
  }

  @Test
  fun theMedioReadingUsesTheParsersOwnTipo() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.TIPO, "INGRESO"))

    val sentence = interpret("zumbido 10 mil con débito", rules)

    assertEquals(TipoMovimiento.INGRESO, sentence.tipo)
    assertEquals("DEBITO", sentence.readings.leido[CampoCorreccion.MEDIO])
    assertEquals("TRANSFERENCIA", sentence.readings.propuesto[CampoCorreccion.MEDIO])
  }

  @Test
  fun aTipoRuleOverridesTheVerbItCorrected() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.TIPO, "INGRESO", leido = "GASTO"))

    val sentence = interpret("gasté 10 mil en zumbido", rules)

    assertEquals(TipoMovimiento.INGRESO, sentence.tipo)
    assertEquals("GASTO", sentence.readings.leido[CampoCorreccion.TIPO])
  }

  @Test
  fun aLearnedMedioIsAdaptedToAnIncome() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.MEDIO, "DEBITO"))

    val sentence = interpret("me pagaron 10 mil del zumbido", rules)

    assertEquals(TipoMovimiento.INGRESO, sentence.tipo)
    assertEquals(MedioPago.TRANSFERENCIA, sentence.sentence.medio.medio)
  }

  @Test
  fun anUnsupportedRecurrenceIsNotFilledByAHabit() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.FRECUENCIA, "MENSUAL"))

    val sentence = interpret("zumbido 10 mil cada 15 días", rules)

    assertTrue(PhraseReviewFlag.UNSUPPORTED_RECURRENCE in sentence.parsed.reviewFlags)
    assertNull(sentence.parsed.frecuencia)
  }

  @Test
  fun anOfferedCuentaJoinsTheAlternatives() {
    val rules =
        rulesOf(listOf(record(listOf("almuerzo", "visa"), CampoCorreccion.CUENTA, NEQUI.id)))

    val sentence = interpret("almuerzo 20 mil con la visa", rules)

    assertNull(sentence.sentence.cuenta.cuenta)
    assertEquals(listOf(NEQUI.id), sentence.sentence.cuenta.alternativeIds)
    assertEquals(listOf(NEQUI.id), sentence.offered.cuentaIds)
  }

  @Test
  fun aRuleForAnArchivedCuentaIsIgnored() {
    val archived = rulesOf(twice(listOf("almuerzo"), CampoCorreccion.CUENTA, OLD_BANK.id))
    val deleted = rulesOf(twice(listOf("almuerzo"), CampoCorreccion.CUENTA, "gone"))
    val cuentas = listOf(COP_CASH, NU, OLD_BANK)

    assertNull(interpret("almuerzo 20 mil", archived, cuentas).sentence.cuenta.cuenta)
    assertNull(interpret("almuerzo 20 mil", deleted, cuentas).sentence.cuenta.cuenta)
    assertTrue(interpret("almuerzo 20 mil", archived, cuentas).offered.cuentaIds.isEmpty())
  }

  @Test
  fun aLearnedFrecuenciaFillsAnUndecidedSentence() {
    val rules = rulesOf(twice(listOf("zumbido"), CampoCorreccion.FRECUENCIA, "MENSUAL"))

    val sentence = interpret("zumbido 10 mil", rules)

    assertEquals(Frecuencia.MENSUAL, sentence.parsed.frecuencia)
    assertNull(sentence.readings.leido[CampoCorreccion.FRECUENCIA])
    assertEquals("MENSUAL", sentence.readings.propuesto[CampoCorreccion.FRECUENCIA])
  }

  @Test
  fun readingsKeepTheParserValueAndTheProposal() {
    val rules =
        rulesOf(
            twice(listOf("almuerzo", "visa"), CampoCorreccion.MEDIO, "CREDITO"),
            twice(
                listOf("almuerzo", "visa"),
                CampoCorreccion.CATEGORIA,
                "cat_comida",
                leido = "cat_restaurante",
            ),
        )

    val readings = interpret("almuerzo 20 mil con la visa", rules).readings

    assertEquals(
        mapOf(
            CampoCorreccion.TIPO to null,
            CampoCorreccion.MONTO to "2000000 COP",
            CampoCorreccion.FECHA to null,
            CampoCorreccion.CATEGORIA to "cat_restaurante",
            CampoCorreccion.MEDIO to null,
            CampoCorreccion.CUENTA to null,
            CampoCorreccion.FRECUENCIA to null,
        ),
        readings.leido,
    )
    assertEquals(
        mapOf(
            CampoCorreccion.TIPO to "GASTO",
            CampoCorreccion.MONTO to "2000000 COP",
            CampoCorreccion.FECHA to "2026-10-15",
            CampoCorreccion.CATEGORIA to "cat_comida",
            CampoCorreccion.MEDIO to "CREDITO",
            CampoCorreccion.CUENTA to null,
            CampoCorreccion.FRECUENCIA to null,
        ),
        readings.propuesto,
    )
  }

  @Test
  fun partsUseTheirOwnWords() {
    val text =
        "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la " +
            "tarjeta de crédito"
    val rules =
        rulesOf(
            twice(
                listOf("gaseosa"),
                CampoCorreccion.CATEGORIA,
                "cat_comida",
                leido = "cat_supermercado",
            )
        )

    val parts = interpret(text, rules).parts

    assertEquals(listOf("cat_restaurante", "cat_comida"), parts.map { it.preselectedCategoriaId })
    assertEquals("cat_supermercado", parts[1].readings.leido[CampoCorreccion.CATEGORIA])
    assertEquals("cat_comida", parts[1].readings.propuesto[CampoCorreccion.CATEGORIA])
    assertEquals("3000000 COP", parts[1].readings.leido[CampoCorreccion.MONTO])
    assertEquals("CREDITO", parts[1].readings.propuesto[CampoCorreccion.MEDIO])
    assertTrue("gaseosa" in parts[1].words && "hamburguesa" !in parts[1].words)
  }

  @Test
  fun aPartOffersItsOwnCategory() {
    val text = "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil"
    val rules = rulesOf(listOf(record(listOf("gaseosa"), CampoCorreccion.CATEGORIA, "cat_comida")))

    val parts = interpret(text, rules).parts

    assertEquals(
        listOf(emptyList(), listOf("cat_comida")),
        parts.map { it.query.offeredCategoriaIds },
    )
    assertEquals(
        listOf("cat_restaurante", "cat_supermercado"),
        parts.map { it.preselectedCategoriaId },
    )
  }

  @Test
  fun noRulesChangeNothing() {
    val unrelated =
        rulesOf(
            twice(listOf("xilofono"), CampoCorreccion.CATEGORIA, "cat_mascotas"),
            twice(listOf("xilofono"), CampoCorreccion.MEDIO, "CREDITO"),
            twice(listOf("xilofono"), CampoCorreccion.TIPO, "INGRESO"),
        )
    val corpus =
        listOf(
            "almuerzo 20 mil con la visa",
            "gasté 50 mil en el super ayer con débito",
            "me pagaron el sueldo 3 millones",
            "netflix 40 mil cada mes con NU",
            "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil",
        )

    corpus.forEach { text -> assertEquals(interpret(text), interpret(text, unrelated), text) }
  }
}
