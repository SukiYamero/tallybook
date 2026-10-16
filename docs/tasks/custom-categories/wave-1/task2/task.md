# Task 2 — Appearance suggestion from a name

Blocks: task3. Parallel with task1 (no shared files).

## Objective

Map a typed category name to a suggested icon and tint, reusing the smart-entry dictionary.

## Files

- `core/model/CategoryIcon.kt`: add `TAG("tag")`; `core/ui/category/CategoryIconVector.kt`: map to
  `Lucide.Tag` (confirm the symbol exists in the resolved `icons-lucide-cmp` version).
- `core/data/entry/CategoryAppearanceSuggester.kt` (new):

  ```kotlin
  data class CategoryAppearance(val icono: CategoryIcon, val color: CategoryTint)
  class CategoryAppearanceSuggester(dictionary: CategoryKeywordDictionary, seeds: List<CategorySeed>) {
    fun suggest(nombre: String): CategoryAppearance
  }
  ```

  `suggestionTokens(nombre)` → `dictionary.evidence(tokens)` → highest score (ties: strong first,
  then seed catalog order) → that seed's `icono`/`color`. No tokens or no evidence → `TAG`/`NEUTRAL`.
  `cat_otros` never wins.
- `di/EntryModule.kt`: `single { CategoryAppearanceSuggester(CategoryKeywordDictionary.builtIn,
  CategorySeedCatalog.categories) }` (or reuse the existing dictionary single).

## Tests (commonTest)

"GYM" and "gimnasio" → the dictionary's gym seed icon/tint (`DUMBBELL` expected; assert against
the seed, not a literal, if the seed differs); accents/case/plural folding ("Cafés"); unknown word
and blank → `TAG`/`NEUTRAL`; deterministic tie-break.

## Exit

commonTest green via `testAndroidHostTest`, `detekt`, `ktfmtCheck`.
