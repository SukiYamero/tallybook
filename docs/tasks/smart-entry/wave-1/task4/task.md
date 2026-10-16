# Wave 1 / Task 4 — Otros, child selection support and Search roll-up

## Goal

Add the `cat_otros` system category, let the picker exclude IDs, and make Search category filters
include children of a selected parent, following
[Otros and pending movimientos](../../../../business-logic.md#otros-and-pending-movimientos) and
[Hierarchy and filtering](../../../../business-logic.md#hierarchy-and-filtering).

## Context

- Seeds are inserted with `INSERT OR IGNORE` at every bootstrap (`LocalDataBootstrapper`) and renamed
  by `CategoriaRepository.localizeSeeded` after language changes, so a new seed reaches existing
  installs without a migration.
- `CategoryTint` and `CategoryIcon` are persisted by `storageKey`; new entries must not change
  existing keys. `neutralCategoryTintColors()` already exists for unknown categories — map the new
  `NEUTRAL` tint to it rather than adding new color tokens.
- Search filters are built in `SearchUiState` and read through
  `MovimientoRepositoryImpl.observeFiltered` (`selectFilteredMovimientosByCategory`, `IN :category_ids`).
  Roll-up belongs in the query so membership, counts and currency metadata keep coming from one read
  snapshot: match `categoria_id IN :category_ids OR categoria_id IN (SELECT id FROM categoria WHERE padre_id IN :category_ids)`.

## Changes

- `core/model/Categoria.kt` (or a sibling file): `const val CATEGORIA_OTROS_ID = "cat_otros"`.
- `CategoryTint.NEUTRAL("neutral")`, `CategoryIcon.CIRCLE_DASHED("circle-dashed")` with its Lucide
  vector in `CategoryIconVector.kt` and colors in `CategoryTintColors.kt`.
- `CategorySeedCatalog`: root `cat_otros` with `CIRCLE_DASHED`/`NEUTRAL`; localized names in
  `CategorySeedProvider` and the four `strings.xml` (`category_otros`: Otros / Otros / Other / Outros).
- `CategoryPickerSheet`: new `excludedIds: Set<String> = emptySet()` applied to roots, children and
  search results. Do not change call sites (wave 2 flips `allowSubcategories` and passes the
  exclusion).
- `Movimiento.sq` filtered-by-category query with parent → children roll-up; Search keeps listing
  every category, including Otros, as a filter option.

## Files

`CategoryTint.kt`, `CategoryIcon.kt`, `CategoryIconVector.kt`, `CategoryTintColors.kt`,
`CategorySeedCatalog.kt`, `CategorySeedProvider.kt`, `Categoria.kt`, `CategoryPickerSheet.kt`,
`Movimiento.sq`, `MovimientoRepositoryImpl.kt` only if the query signature changes, strings.

## Tests

- `CategorySeedCatalogTest` / `CategorySeedTest`: Otros present, root, neutral, localized in every
  locale; existing installs get it on the next `ensureSeeded`.
- `SearchRepositoryTest`: parent filter includes children, child filter excludes parent and siblings,
  mixed parent + unrelated child, Otros filterable, unknown IDs still tolerated.
- Picker exclusion covered by an existing picker test file or a focused new one.

## Done when

Targeted host tests pass, `DatabaseSchemaTest` still passes (no schema change), iOS compiles,
`detekt ktfmtCheck` clean.
