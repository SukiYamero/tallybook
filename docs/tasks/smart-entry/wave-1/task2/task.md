# Wave 1 / Task 2 — Category suggestion engine

## Goal

Rank category suggestions for a sentence subject from the user's own history and a built-in keyword
dictionary, following [Category suggestions](../../../../business-logic.md#category-suggestions).
Also expose the bounded history read the engine learns from.

## Context

- Contracts: [README](../../README.md#shared-contracts). Inputs are subjects already stripped of
  verbs, amounts and dates by task1; this task owns how subjects are compared.
- Text comparison: `normalizeSearchText` (case + supported accent folding, keeps `ñ`), split on
  non-letters, drop stop words of all supported languages (`de, del, en, el, la, los, las, un, una,
  para, por, con, y, of, the, on, for, at, and, a, no, na, do, da, em, com, pra`…), basic plural
  reduction (`hoteles → hotel`, `reservaciones → reservacion`, `cafés → cafe`, `tickets → ticket`).
  `normalizeSearchText` lives in the framework-free `core.text` package.
- The dictionary is Kotlin data in `commonMain` (no JSON asset): entries of normalized phrase or word,
  target seeded category ID, weight (`STRONG` for unambiguous words, phrases and brands; `WEAK` for
  ambiguous words) and optional language set. It covers all seeded categories in
  `CategorySeedCatalog` for `es` (CO, MX, AR, CL, PE, DO variants: `parqueadero/estacionamiento`,
  `domicilio/delivery`, `nafta/bencina/gasolina`, `colectivo/micro/bus`), `en` and `pt-BR`, plus
  common regional brands (Uber, DiDi, Cabify, Rappi, iFood, PedidosYa, Netflix, Spotify, Disney+,
  Airbnb, Booking, Terpel, YPF, Copec, Éxito, Carulla, Walmart, OXXO, Mercado Libre…). Aim for at
  least 8 entries per seeded child category. Ambiguous words list several targets with `WEAK`.
- Scoring, strongest first: exact subject match in history (token multiset equality after
  normalization); learned token → category associations from history with frequency and recency
  weighting (use a half-life around 90 days; smoothing so one old entry does not dominate); dictionary
  phrases; dictionary words/brands. Income categories (children of `cat_ingresos` and the parent) get a
  boost when `tipo == INGRESO` and a penalty when `tipo == GASTO`; `null` tipo is neutral.
- `HIGH` only for an exact history match whose winning category has a clear majority, or a single
  `STRONG` dictionary match with no competing category. Everything else is `MEDIUM`; results below a
  minimum score are dropped. Never return `CATEGORIA_OTROS_ID` (`"cat_otros"`, string literal until
  task4 lands) or IDs outside `availableIds`/inside `excludedIds`.
- `index` must be cheap enough to rebuild on each history emission (bounded at 1,000 rows) and
  `suggest` cheap enough to run on every debounced keystroke; no per-call rebuild of the dictionary.

## History read

- Add to `Movimiento.sq`:
  `selectCategoryHistory: SELECT descripcion, categoria_id, created_at FROM movimiento WHERE descripcion IS NOT NULL AND trim(descripcion) <> '' ORDER BY created_at DESC, id DESC LIMIT ?;`
  The existing indexes do not cover `created_at` first; measure with `EXPLAIN QUERY PLAN` in a host
  test and add an index only if the scan matters at the 1,000-row bound (prefer no new index; a new
  index needs a migration, which this phase avoids).
- `MovimientoRepository.observeCategoryHistory(limit)` maps rows to `CategoryHistoryRow` through the
  repository error boundary (`asLocalDataOutcome()`), on the injected dispatcher. Update every fake of
  `MovimientoRepository` in tests.

## Files

- Create in `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/`:
  `CategorySuggester.kt` (suggester + index + confidence/suggestion models), `SuggestionText.kt`
  (tokenization), `CategoryKeywordDictionary.kt` (+ `CategoryKeywordEntries*.kt` split by category
  family if Detekt flags size).
- Modify `Movimiento.sq`, `MovimientoRepository.kt`, `MovimientoRepositoryImpl.kt`, test fakes.
- Tests: `commonTest/.../core/data/entry/CategorySuggesterTest.kt`, `SuggestionTextTest.kt`;
  `androidHostTest/.../core/data/MovimientoRepositoryTest.kt` (history query: order, limit, blank
  exclusion, emission after insert).

## Tests that matter

"hotel y reservaciones" → `cat_hospedaje`; "uber" → `cat_taxi`; "videojuegos" → `cat_juegos`;
"aceite" alone → no `HIGH`; "cambio de aceite" → `cat_transporte` `HIGH`; history "aceite" ×3 →
`cat_supermercado` learned over the dictionary;
a correction in recent history beats an older majority after decay; income boost; excluded and
unavailable IDs never returned; Otros never returned; dictionary integrity (every target exists in
`CategorySeedCatalog`, no duplicate normalized keys per target, every child category covered).

## Done when

Targeted host tests pass, iOS test compilation passes, `detekt ktfmtCheck` clean. No UI or DI changes.
