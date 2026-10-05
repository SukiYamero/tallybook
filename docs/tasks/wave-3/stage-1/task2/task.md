# Wave 3 / Stage 1 / Task 2 — Shared search semantics

## Objective and context

Give History and category selection one KMP normalization implementation without changing their
different empty-query policies. Read the [wave contract](../../README.md), current `CategorySearch.kt`
and its tests. This task may run beside task1 with separate file ownership.

Skills: `android-dev`, `kotlin-api-design`, `android-testing`, `test-driven-development`; use
`gradle-run` for commands.

## Files and contracts

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/text/SearchText.kt`: internal,
  platform-free normalization and literal substring matching.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategorySearch.kt`
  to reuse the normalizer. Retain `searchCategorias` returning no suggestions for empty input.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/text/SearchTextTest.kt`;
  retain and extend `feature/movimientos/ui/CategorySearchTest.kt`.

Produces `normalizeSearchText(value: String): String` and
`matchesSearchText(query: String, fields: List<String>): Boolean`.
An empty normalized query matches every candidate in History; category-search short-circuits before
calling the matcher. Fields are supplied by the projection using the exact displayed text.

## Specific approach

- Extract the existing supported Latin-accent replacements; no Java Normalizer, Foundation code or
  new Unicode library in commonMain. Verify lowercasing on supported targets.
- Support composed and decomposed accents for Spanish, English and Brazilian Portuguese, including
  cedilla and combining-mark variants. Keep `ñ` distinct from `n`, preserving the existing category
  search contract; document that this is supported-locale folding, not universal transliteration.
  Recognize decomposed n + combining tilde as ñ before folding other supported accent marks; do not
  strip every combining mark and accidentally erase this distinction.
- Trim leading/trailing query whitespace, lowercase and fold accents. Preserve internal spaces and
  punctuation. Do not split tokens, treat SQL wildcards specially or match raw category IDs.
- No maximum query length or truncation is introduced without a product reason. Do not echo search
  text into logs or exceptions. Normalization remains a pure value-to-value operation.
- The eventual History projection precomputes normalized searchable fields per range/category
  snapshot; it does not rebuild them on every keystroke or mutate persisted names.

## Test-first slices

- [ ] RED: case/outer whitespace and composed/decomposed café, ação and reimbursed descriptions match.
      GREEN: extract and complete the shared normalizer.
- [ ] RED: `%`, `_`, quotes and emoji remain literal; composed/decomposed ñ does not match n; empty query
      matches all History candidates. GREEN: implement matching with explicit field inputs.
- [ ] Category-search regression: empty query still returns no suggestions; existing accents and order
      are preserved. Fix only touched duplication, not the picker tree or category model.
- [ ] Run focused host tests and Native test compilation, then Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*SearchTextTest' --tests '*CategorySearchTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when both consumers can share deterministic normalization without behavior regression.
Blocks Stage 3 projection. Does not change the canonical product document owned by task1.
