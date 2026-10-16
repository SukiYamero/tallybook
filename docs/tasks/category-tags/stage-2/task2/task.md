# Stage 2 / Task 2 — Movimiento creation and editor

## Objective

Replace the multi-category field with the single-category switch and add the optional tags section in
the sentence dialog, the summary, the manual form and the editor; detect existing tags from the
sentence.

## Context and skills

- Requires Stage 1 reviewed. Read `../../README.md`, `docs/business-logic.md` § Natural-language
  entry / Category suggestions / Editing, `docs/features/add-movimiento.md`,
  `docs/features/manage-movimientos.md`.
- Skills: `android-skills:android-dev`, `android-skills:compose`,
  `chrisbanes-skills:compose-state-and-effects`, `android-skills:kotlin-flows`,
  `android-skills:android-testing`, `superpowers:test-driven-development`.

## Files (owned by this task)

Paths relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.

- `feature/movimientos/viewmodel/*`: `AddMovimientoUiState.kt`, `AddMovimientoEntryUi.kt`,
  `AddMovimientoActions.kt`, `AddMovimientoViewModel.kt`, `MovimientoEntryEngine.kt`,
  `MovimientoFormState.kt`, `MovimientoFormActions.kt`, `EditMovimientoUiState.kt`,
  `ManageMovimientoUiState.kt`, `ManageMovimientoViewModel.kt`.
- `feature/movimientos/ui/*`: `MovimientoSentenceStep.kt`, `MovimientoSummaryStep.kt`,
  `MovimientoForm.kt`, `MovimientoPrimaryFields.kt`, `MovimientoCategoriesField.kt` (replace with the
  single field wiring), `AddMovimientoSheet.kt`, `ManageMovimientoRoute.kt`,
  `ManageMovimientoContent.kt`, `MovimientoCategoryPicker.kt`, samples and previews.
- New `core/data/entry/EtiquetaMatcher.kt` + `di/EntryModule.kt` registration (only Stage 2 editor of
  that file). The ViewModels already receive `EtiquetaRepository` from Stage 1; do not change their
  dependency holders.
- Tests: `AddMovimientoEntryFlowTest.kt`, `AddMovimientoViewModelTest.kt`,
  `AddMovimientoUiStateTest.kt`, `ManageMovimientoCategoriesTest.kt` (rename to cover tags),
  `ManageMovimientoViewModelTest.kt`, fixtures; device tests `MovimientoSentenceStepTest.kt`,
  `MovimientoSummaryStepTest.kt`, `MovimientoFormTest.kt`, `AddMovimientoSheetTest.kt`,
  `ManageMovimientoSheetTest.kt`; new `EtiquetaMatcherTest.kt` (commonTest).

## Behavior

- **Category (all surfaces):** `CategorySingleField`. Suggestions come from the existing ranking
  (`MAX_SUGGESTION_CHIPS`). While the sentence owns the category, the HIGH suggestion is selected;
  the first manual choice hands the category to the user and later sentences stop replacing it.
  Choosing another suggestion switches. In creation the user may deselect (save falls back to Otros
  pending); in the editor the field always holds one category.
- **Tags (all surfaces):** `EtiquetaTagField` below the category, header `movimiento_tags_section`.
  Add opens `EtiquetaPickerSheet`; `onCreate(name)` calls `EtiquetaRepository.create` and selects the
  result (or the existing tag on duplicate) when under the limit.
- **Detection:** `EtiquetaMatcher` matches whole-token, case/accent-insensitive names of existing tags
  in the sentence (multi-word names allowed; reuse `core/text/SearchText` normalization). While the
  user has not touched tags, the sentence owns them; the first manual add/remove hands them over.
  Clearing the sentence clears sentence-owned tags, never hand-picked ones. Never creates tags.
- **Fill manually** keeps hand-picked category and tags. Tags/category picked in the dialog do not
  count as unsaved changes, matching today's category rule.
- **Editor:** loads the movimiento's tags; a deleted tag simply disappears; no-change detection
  includes tags.
- Tags catalog failures show the existing categories-unavailable pattern for the tags section only;
  the movimiento can still be saved without tags.

## TDD steps

- [ ] Engine/matcher: tag names detected with accents/case, multi-word, no partial-word match.
- [ ] Entry flow: sentence owns → manual switch takes over; deselect → Otros pending at save; tags
      owned by sentence vs hand-picked; clear sentence; Fill manually keeps manual picks; limit.
- [ ] Editor: change category, add/remove tags, no-change save, deleted tag absent.
- [ ] Device: dimmed suggestions clickable, tag picker create, IME keeps Continue visible.

## Exit

Create and edit flows save `categoriaId` + `etiquetaIds` correctly on host tests and render on device
fixtures.
