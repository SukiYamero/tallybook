# Wave 3 / Stage 1 / Task 2 — Shared search semantics

## Objective and context

Preserve the reviewed KMP normalization used by category selection and available to future Search.
This task is completed at `e0d667b`; active History has no text-search consumer. Read the [wave contract](../../README.md), current `CategorySearch.kt`
and its tests. Do not re-execute or revert the reviewed work for the History-only scope.

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
An empty normalized query matches every candidate for a future text-search consumer; category-search short-circuits before
calling the matcher. Future consumers supply the exact displayed text; History builds no searchable fields.

`matchesSearchText` is a raw-input convenience. Snapshot consumers normalize query once and use
`matchesNormalizedSearchText(normalizedQuery, normalizedFields)` over fields indexed once per snapshot.

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
- A future Search projection can precompute normalized fields per data/category
  snapshot; it does not rebuild them on every keystroke or mutate persisted names.

## Test-first slices

- [x] RED: case/outer whitespace and composed/decomposed café, ação and reimbursed descriptions match.
      GREEN: extract and complete the shared normalizer.
- [x] RED: `%`, `_`, quotes and emoji remain literal; composed/decomposed ñ does not match n; empty query
      matches all generic candidates. GREEN: implement matching with explicit field inputs.
- [x] Category-search regression: empty query still returns no suggestions; existing accents and order
      are preserved. Fix only touched duplication, not the picker tree or category model.
- [x] Run focused host tests and Native test compilation, then Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*SearchTextTest' --tests '*CategorySearchTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Completed and independently reviewed at `e0d667b`; preserve category-search behavior and regression
coverage. No active History task depends on normalization. It does not change canonical rules owned
by task1; its recorded checks do not prove History runtime behavior.
