package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryKeywordDictionaryTest {

  private val seededIds = CategorySeedCatalog.categories.map { it.id }.toSet()
  private val children = CategorySeedCatalog.categories.filter { it.padreId != null }
  // Otros is the never-suggested fallback, so it is the one root without keywords.
  private val parents =
      CategorySeedCatalog.categories.filter { it.padreId == null && it.id != CATEGORIA_OTROS_ID }

  private fun normalizedKey(entry: CategoryKeywordEntry) =
      suggestionTokens(entry.phrase).joinToString(" ")

  @Test
  fun everyEntryTargetsAnExistingNonOtrosCategory() {
    val unknown = builtInKeywords.filter {
      it.categoriaId !in seededIds || it.categoriaId == CATEGORIA_OTROS_ID
    }

    assertEquals(emptyList(), unknown)
  }

  @Test
  fun everyEntryHasAtLeastOneSearchableWord() {
    val empty = builtInKeywords.filter { suggestionTokens(it.phrase).isEmpty() }

    assertEquals(emptyList(), empty.map { it.phrase })
  }

  @Test
  fun noTargetRepeatsANormalizedKey() {
    val duplicates =
        builtInKeywords
            .groupBy { it.categoriaId to normalizedKey(it) }
            .filterValues { it.size > 1 }
            .keys

    assertEquals(emptySet(), duplicates)
  }

  @Test
  fun aKeySharedByTwoCategoriesIsAlwaysWeak() {
    val strongAmbiguities =
        builtInKeywords
            .groupBy(::normalizedKey)
            .filterValues { group -> group.map { it.categoriaId }.distinct().size > 1 }
            .filterValues { group -> group.any { it.weight == KeywordWeight.STRONG } }
            .mapValues { (_, group) -> group.map { "${it.categoriaId}:${it.weight}" } }

    assertEquals(emptyMap(), strongAmbiguities)
  }

  @Test
  fun everyChildCategoryHasAtLeastEightStrongKeys() {
    val strongKeys =
        builtInKeywords
            .filter { it.weight == KeywordWeight.STRONG }
            .groupBy({ it.categoriaId }, ::normalizedKey)
    val thin = children.map { it.id }.filter { (strongKeys[it]?.distinct()?.size ?: 0) < 8 }

    assertEquals(emptyList(), thin)
  }

  @Test
  fun everyParentCategoryHasGenericKeys() {
    val keys = builtInKeywords.groupBy({ it.categoriaId }, ::normalizedKey)
    val thin = parents.map { it.id }.filter { (keys[it]?.distinct()?.size ?: 0) < 3 }

    assertEquals(emptyList(), thin)
  }

  @Test
  fun ambiguousWordsListSeveralWeakTargets() {
    val expected =
        mapOf(
            "aceite" to setOf("cat_supermercado", "cat_transporte"),
            "reservacion" to setOf("cat_hospedaje", "cat_restaurante", "cat_vuelos"),
            "ticket" to setOf("cat_cine", "cat_vuelos", "cat_paseos"),
            "gas" to setOf("cat_gasolina", "cat_servicios"),
        )

    expected.forEach { (word, targets) ->
      val entries = builtInKeywords.filter { normalizedKey(it) == word }
      assertTrue(entries.all { it.weight == KeywordWeight.WEAK }, word)
      assertTrue(entries.map { it.categoriaId }.containsAll(targets), word)
    }
  }

  @Test
  fun entriesWithoutSearchableWordsAreIgnored() {
    val dictionary =
        CategoryKeywordDictionary(
            listOf(
                CategoryKeywordEntry("de la", "cat_comida", KeywordWeight.STRONG),
                CategoryKeywordEntry("cine", "cat_cine", KeywordWeight.STRONG),
            )
        )

    assertEquals(setOf("cat_cine"), dictionary.evidence(suggestionTokens("cine y de la")).keys)
  }
}
