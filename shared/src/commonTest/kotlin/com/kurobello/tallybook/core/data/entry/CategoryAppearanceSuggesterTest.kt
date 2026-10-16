package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.data.CategorySeed
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryAppearanceSuggesterTest {

  private val builtIn =
      CategoryAppearanceSuggester(
          CategoryKeywordDictionary.builtIn,
          CategorySeedCatalog.categories,
      )

  private val unmatched = CategoryAppearance(CategoryIcon.TAG, CategoryTint.NEUTRAL)

  private fun seedAppearance(id: String): CategoryAppearance {
    val seed = CategorySeedCatalog.categories.single { it.id == id }
    return CategoryAppearance(seed.icono, seed.color)
  }

  private fun seed(id: String, icono: CategoryIcon, color: CategoryTint) =
      CategorySeed(id = id, padreId = null, icono = icono, color = color)

  private fun weak(phrase: String, categoriaId: String) =
      CategoryKeywordEntry(phrase, categoriaId, KeywordWeight.WEAK)

  private fun strong(phrase: String, categoriaId: String) =
      CategoryKeywordEntry(phrase, categoriaId, KeywordWeight.STRONG)

  @Test
  fun gymInAnyCaseTakesTheGymSeedAppearance() {
    assertEquals(CategoryIcon.DUMBBELL, seedAppearance("cat_gimnasio").icono)
    assertEquals(seedAppearance("cat_gimnasio"), builtIn.suggest("GYM"))
    assertEquals(seedAppearance("cat_gimnasio"), builtIn.suggest("gimnasio"))
  }

  @Test
  fun accentsCaseAndPluralsFoldBeforeMatching() {
    assertEquals(seedAppearance("cat_cafe"), builtIn.suggest("Cafés"))
    assertEquals(seedAppearance("cat_gimnasio"), builtIn.suggest("  Gimnasios "))
  }

  @Test
  fun unknownWordFallsBackToTagAndNeutral() {
    assertEquals(unmatched, builtIn.suggest("Zxqwv"))
  }

  @Test
  fun blankOrStopWordOnlyNameFallsBackToTagAndNeutral() {
    assertEquals(unmatched, builtIn.suggest(""))
    assertEquals(unmatched, builtIn.suggest("   "))
    assertEquals(unmatched, builtIn.suggest("de la"))
  }

  @Test
  fun otrosNeverWinsEvenWithTheOnlyEvidence() {
    val suggester =
        CategoryAppearanceSuggester(
            CategoryKeywordDictionary(listOf(strong("misc", CATEGORIA_OTROS_ID))),
            listOf(seed(CATEGORIA_OTROS_ID, CategoryIcon.CIRCLE_DASHED, CategoryTint.NEUTRAL)),
        )

    assertEquals(unmatched, suggester.suggest("misc"))
  }

  @Test
  fun evidenceForAnIdOutsideTheSeedsIsIgnored() {
    val suggester =
        CategoryAppearanceSuggester(
            CategoryKeywordDictionary(
                listOf(strong("rent", "cat_unknown"), weak("rent", "cat_home"))
            ),
            listOf(seed("cat_home", CategoryIcon.HOUSE, CategoryTint.PURPLE)),
        )

    assertEquals(
        CategoryAppearance(CategoryIcon.HOUSE, CategoryTint.PURPLE),
        suggester.suggest("rent"),
    )
  }

  @Test
  fun higherScoreWinsOverSeedOrder() {
    val suggester =
        CategoryAppearanceSuggester(
            CategoryKeywordDictionary(listOf(weak("snack", "cat_a"), strong("snack", "cat_b"))),
            listOf(
                seed("cat_a", CategoryIcon.COFFEE, CategoryTint.AMBER),
                seed("cat_b", CategoryIcon.UTENSILS, CategoryTint.ROSE),
            ),
        )

    assertEquals(
        CategoryAppearance(CategoryIcon.UTENSILS, CategoryTint.ROSE),
        suggester.suggest("snack"),
    )
  }

  @Test
  fun equalScoreGoesToStrongEvidenceBeforeSeedOrder() {
    val dictionary =
        CategoryKeywordDictionary(
            listOf("gamma", "delta", "kappa", "sigma", "omega").map { weak(it, "cat_weak") } +
                strong("alfa beta", "cat_strong")
        )
    val name = "gamma delta kappa sigma omega alfa beta"
    val evidence = dictionary.evidence(suggestionTokens(name))
    assertEquals(evidence.getValue("cat_weak").score, evidence.getValue("cat_strong").score)

    val suggester =
        CategoryAppearanceSuggester(
            dictionary,
            listOf(
                seed("cat_weak", CategoryIcon.COFFEE, CategoryTint.AMBER),
                seed("cat_strong", CategoryIcon.UTENSILS, CategoryTint.ROSE),
            ),
        )

    assertEquals(
        CategoryAppearance(CategoryIcon.UTENSILS, CategoryTint.ROSE),
        suggester.suggest(name),
    )
  }

  @Test
  fun fullTieGoesToTheEarliestSeedInCatalogOrder() {
    val dictionary =
        CategoryKeywordDictionary(listOf(weak("snack", "cat_a"), weak("snack", "cat_b")))
    val first = seed("cat_b", CategoryIcon.UTENSILS, CategoryTint.ROSE)
    val second = seed("cat_a", CategoryIcon.COFFEE, CategoryTint.AMBER)

    assertEquals(
        CategoryAppearance(first.icono, first.color),
        CategoryAppearanceSuggester(dictionary, listOf(first, second)).suggest("snack"),
    )
    assertEquals(
        CategoryAppearance(second.icono, second.color),
        CategoryAppearanceSuggester(dictionary, listOf(second, first)).suggest("snack"),
    )
  }
}
