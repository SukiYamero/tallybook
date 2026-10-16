# Stage 1 / Task 2 — Shared UI contracts and copy

## Objective

Create the stateless shared components and copy that Stage 2 consumes, so the three Stage 2 tasks
never edit the same files.

## Context and skills

- Requires Stage 1 / task1 (models and `Etiqueta`).
- Read `../../README.md` (visual identity, copy table), `docs/ui/architecture.md`,
  `docs/ui/components.md`, `docs/wording.md`.
- Skills: `android-skills:compose`, `chrisbanes-skills:compose-component-design`,
  `chrisbanes-skills:compose-state-and-effects`, `android-skills:android-ux`,
  `chrisbanes-skills:compose-ui-testing-patterns`, `superpowers:test-driven-development`.

## Files

Paths relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.

- New `core/ui/tag/EtiquetaTag.kt`: neutral chip, leading tag/`#` icon (Lucide), optional remove
  action. Reuse `CategoryTagParts.kt` primitives (`CategoryTagSurface`, `CategoryTagLabel`,
  `CategoryTagRemoveTarget`) instead of copying them; rename them to neutral names only if the reuse
  needs it.
- New `core/ui/tag/EtiquetaChip.kt`: selectable chip with optional trailing count (for screens and
  filters).
- New `core/ui/tag/EtiquetaTagField.kt`: section header `movimiento_tags_section`, 0..max removable
  tags, an Add tag hidden at max, optional suggestions. No "last cannot be removed" rule.
- New `core/ui/tag/EtiquetaPickerSheet.kt`: multi-select, flat list, search, selected tags under
  search, limit hint, Done; empty search result offers `tag_picker_create` which calls
  `onCreate(name)` (one tap, no form). Reuse the `CategoryPickerSheet` shell pieces where they are
  generic; do not fork the modal/height plumbing.
- New `core/ui/category/CategorySingleField.kt`: section header `movimiento_category_section`; the
  selected category tag; suggestion tags that render dimmed (existing disabled appearance) while a
  category is selected and remain clickable to switch; a "Choose" action opening the picker; pending
  (Otros) state; `deselectable: Boolean` (true in creation, false in the editor).
- `feature/movimientos/ui/CategoryPickerSheet.kt` + `CategoryPickerSelection.kt`: single-select mode —
  tapping a category selects it and closes; no selected section, Done counter or limit hint; quick-
  create selects the created category. Remove the multi-select path and `CategoriaSelection` usage.
- New `feature/search/ui/SearchFilterRequest.kt` replacing `SearchCategoryRequest.kt`
  (`categoryIds`, `etiquetaIds`), plus the `PrimaryNavigator`/`TallybookNavHost` plumbing to deliver it
  (rename `showCategory` → `showSearchFilter`). Categories screen keeps working through it.
- Delete `core/model/CategoriaSelection.kt`, `MAX_CATEGORIAS_POR_MOVIMIENTO` and their tests
  (`CategoriaSelectionTest.kt`, `CategoryMultiPickerTest.kt`, multi-select cases of
  `CategoryPickerSelectionTest.kt`).
- Call sites broken by these API changes, mechanical adaptation only (new fields are wired in Stage 2):
  `AddMovimientoSheet.kt`, `MovimientoForm.kt`, `ManageMovimientoRoute.kt`,
  `MovimientoCategoryPicker.kt` (`QuickCategorySelectionEffect`), `CategoryPickerGroups.kt`,
  `SearchRoute.kt`, `CategoriesRoute.kt`, `core/ui/catalog/UiCatalogEntry.kt`,
  `UiCatalogEntryTags.kt`; tests `CategoryPickerGroupsTest.kt`, `CategoryPickerSelectionTest.kt`,
  `CategoryPickerQuickCreateTest.kt`, `MovimientoSentenceStepTest.kt`, `MovimientoSummaryStepTest.kt`,
  `MovimientoFormTest.kt`, `AddMovimientoSheetTest.kt`, `CategoriesNavigationTest.kt`,
  `PrimaryNavigationTest.kt`.
- All four `composeResources/values*/strings.xml`: every new key from the README copy table except
  `csv_header_tags` (task1).
- Tests (androidDeviceTest): `core/ui/tag/EtiquetaTagFieldTest.kt`, `EtiquetaPickerSheetTest.kt`,
  `core/ui/category/CategorySingleFieldTest.kt`, update `CategoryPickerTest.kt`,
  `CategoryPickerQuickCreateTest.kt`. Resource parity is already enforced by
  `CategoryFormResourcesHostTest.everyLocaleDeclaresExactlyTheDefaultKeys`.

## Rules

- Components are stateless (state + callbacks), `modifier` at the root, previews light/dark.
- Semantics: section headers are headings; selected category/tag expose `selected`; dimmed
  suggestions keep click semantics and announce they replace the current category; remove actions are
  labeled with `tag_remove`; touch targets ≥ 48 dp.
- Tag chips never use category tints; tokens only, no new hex values.

## TDD steps

- [ ] `CategorySingleField`: selecting a suggestion emits it; with a selection, other suggestions are
      dimmed but clickable; `deselectable = false` hides deselection; pending shows the Otros state.
- [ ] `EtiquetaTagField`: last tag removable; Add hidden at max; suggestions hidden when full.
- [ ] `EtiquetaPickerSheet`: toggle, limit hint, search, create offer emits trimmed name.
- [ ] Single-select `CategoryPickerSheet`: tap selects once and dismisses; hidden categories excluded
      unless current.

## Exit

Stage 1 exit gate items for shared UI; components visible in previews, no feature wiring yet.
