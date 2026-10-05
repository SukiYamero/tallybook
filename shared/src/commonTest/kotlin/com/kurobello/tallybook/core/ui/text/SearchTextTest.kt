package com.kurobello.tallybook.core.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTextTest {

  @Test
  fun foldsSupportedComposedAndDecomposedAccents() {
    assertEquals("cafe acao reembolsado", normalizeSearchText("  CAFÉ AÇÃO REEMBOLSADO  "))
    assertEquals("cafe acao", normalizeSearchText("Cafe\u0301 Ac\u0327a\u0303o"))
    assertEquals("aaaaa eee ii oooo uu c", normalizeSearchText("ÁÀÂÃÄ ÉÊË ÍÏ ÓÔÕÖ ÚÜ Ç"))
    assertEquals(
        "aaaaa eee ii oooo uu c",
        normalizeSearchText(
            "A\u0301A\u0300A\u0302A\u0303A\u0308 E\u0301E\u0302E\u0308 " +
                "I\u0301I\u0308 O\u0301O\u0302O\u0303O\u0308 U\u0301U\u0308 C\u0327"
        ),
    )
  }

  @Test
  fun preservesInternalWhitespacePunctuationAndUnsupportedMarks() {
    assertEquals("cafe  '50%_☕'", normalizeSearchText("  CAFÉ  '50%_☕'  "))
    assertEquals("x\u0301", normalizeSearchText("X\u0301"))
    assertEquals("a\u0304", normalizeSearchText("A\u0304"))
  }

  @Test
  fun preservesEnyeInBothRepresentations() {
    assertEquals("año", normalizeSearchText("AÑO"))
    assertEquals("año", normalizeSearchText("AN\u0303O"))
    assertFalse(matchesSearchText("ano", listOf("Año")))
    assertFalse(matchesSearchText("año", listOf("Ano")))
    assertTrue(matchesSearchText("an\u0303o", listOf("AÑO")))
  }

  @Test
  fun matchesLiteralSubstringsInAnyExplicitField() {
    val fields = listOf("Café", "Compra reembolsada por João")

    assertTrue(matchesSearchText("  CAFE\u0301  ", fields))
    assertTrue(matchesSearchText("reembolsada", fields))
    assertTrue(matchesSearchText("joao", fields))
    assertFalse(matchesSearchText("cafe compra", fields))
    assertFalse(matchesSearchText("compra  reembolsada", fields))
    assertFalse(matchesSearchText("viaje", fields))
  }

  @Test
  fun treatsWildcardCharactersQuotesAndEmojiLiterally() {
    val fields = listOf("Oferta 50%_ '☕'")

    for (query in listOf("%", "_", "'", "☕")) {
      assertTrue(matchesSearchText(query, fields))
      assertFalse(matchesSearchText(query, listOf("Oferta")))
    }
  }

  @Test
  fun matchesEveryCandidateForAnEmptyNormalizedQuery() {
    assertTrue(matchesSearchText("", listOf("Café")))
    assertTrue(matchesSearchText("  ", emptyList()))
    assertFalse(matchesSearchText("cafe", emptyList()))
  }

  @Test
  fun matchesPreNormalizedSnapshotFieldsWithoutFoldingThemAgain() {
    val fields = listOf("cafe", "compra reembolsada")

    assertTrue(matchesNormalizedSearchText("cafe", fields))
    assertTrue(matchesNormalizedSearchText("reembolsada", fields))
    assertTrue(matchesNormalizedSearchText("", emptyList()))
    assertFalse(matchesNormalizedSearchText("cafe", listOf("CAFÉ")))
    assertFalse(matchesNormalizedSearchText("CAFÉ", fields))
  }
}
