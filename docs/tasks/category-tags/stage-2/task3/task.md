# Stage 2 / Task 3 — Search and rows

## Objective

Let Search filter and match by tags, and make every movimiento row reflect the single category.

## Context and skills

- Requires Stage 1 reviewed. Read `../../README.md`, `docs/features/search.md`,
  `docs/business-logic.md` § Search.
- Skills: `android-skills:android-dev`, `android-skills:compose`, `android-skills:kotlin-flows`,
  `android-skills:android-testing`, `superpowers:test-driven-development`.

## Files (owned by this task)

Paths relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.

- `feature/search/viewmodel/*`: `SearchFilters.kt` (`etiquetaIds`, toggle, `hasActiveFilters`),
  `SearchUiState.kt` (tags catalog state, criteria), `SearchSavedInput.kt` (saved `etiquetaIds`),
  `SearchObservation.kt`, `SearchActions.kt` (`onRemoveTagFilter`), `SearchViewModel.kt` (use the
  `EtiquetaRepository` already in `SearchRepositories`, consume `SearchFilterRequest.etiquetaIds`),
  `SearchProjection.kt` (text match on tag names).
- `feature/search/ui/*`: new `SearchTagFilters.kt` (flat `EtiquetaChip` toggles, hidden when the user
  has no tags), `SearchFilterSheet.kt` (Tags section after Categories, `search_tags_label`),
  `SearchScreen.kt` (applied tag chips with the tag style, unknown/deleted ids dropped),
  `SearchRoute.kt` (scroll identity includes tags).
- Rows: `core/ui/movimiento/MovimientoRowContent.kt`, `MovimientoRow.kt`, `MovimientoRowText.kt`,
  `feature/search/ui/SearchResultRow.kt`, `SearchResultsUi.kt`, `feature/history/ui/HistoryScreen.kt`,
  `feature/history/viewmodel/HistoryProjection.kt`, `feature/home/HomeOuterShell.kt` — finish any
  cleanup Stage 1 left (single category, pending = Otros, no "+N" code paths).
- Tests: `SearchFiltersTest.kt`, `SearchProjectionTest.kt`, `SearchViewModelTest.kt`,
  `SearchFilterSheetTest.kt`, `SearchScreenTest.kt`, `SearchUiTestFixture.kt`,
  `MovimientoComponentsTest.kt`, `HistoryProjectionTest.kt`.

## Behavior

- Tag filter: OR within tags, AND with category, tipo, dates and text (enforced by the Stage 1 query).
- Text search matches title, description, category label and every tag name.
- A filter request from the Tags screen replaces current filters with that tag, like the category
  request does today.
- Saved state restores tag filters; a tag deleted while filtered disappears from applied chips and the
  criteria without an error.

## TDD steps

- [ ] Filters: toggle, clear, `hasActiveFilters`, saved-state round trip.
- [ ] Projection: text query matching only a tag name returns the row.
- [ ] ViewModel: request from Tags applies the filter; deleted tag pruned.
- [ ] Device: filter sheet Tags section, applied chips, rows without "+N".

## Exit

Search by tag and by text-on-tag works on host tests; rows show only the category.
