package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CuentaMatcher
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate

class MovimientoEntryEnginePartsTest {

  private val engine =
      MovimientoEntryEngine(
          parser = MovimientoPhraseParser(),
          suggester = CategorySuggester(CategoryKeywordDictionary.builtIn),
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

  private fun interpret(text: String): PhraseInterpretation =
      engine.interpret(
          text,
          context,
          engine.dictionaryIndex(Instant.parse("2026-10-15T00:00:00Z")),
          candidates,
          SentenceCatalogs(
              emptyList(),
              SentenceCuentas(listOf(COP_CASH, NU), Moneda.COP) { _, _ -> Moneda.COP },
          ),
      )

  @Test
  fun contextSentenceInterpretsTwoPartsWithSharedCredito() {
    val sentence =
        interpret(
            "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la " +
                "tarjeta de crédito"
        )

    assertEquals(MedioPago.CREDITO, sentence.sentence.medio.medio)
    assertEquals(
        listOf(Money(2_000_000L, Moneda.COP), Money(3_000_000L, Moneda.COP)),
        sentence.parts.map { it.monto },
    )
    assertEquals(listOf(false, false), sentence.parts.map { it.amountIsAmbiguous })
    assertEquals(
        listOf<String?>("cat_restaurante", null),
        sentence.parts.map { it.preselectedCategoriaId },
    )
  }

  @Test
  fun singleSentenceHasNoParts() {
    assertTrue(interpret("cine y comida 60 mil").parts.isEmpty())
  }
}
