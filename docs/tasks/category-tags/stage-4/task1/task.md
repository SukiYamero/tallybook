# Stage 4 / Task 1 — Design refinements after device review

## Objective

Apply four refinements the user requested after testing on the Xiaomi. Product rules are already in
`docs/business-logic.md` (§ Category and tags, § Tags).

## Context and skills

- Read `AGENTS.md`, `docs/README.md`, `../../README.md`, `docs/ui/architecture.md`,
  `docs/ui/components.md`, `docs/wording.md`.
- Skills: `android-skills:compose`, `android-skills:android-ux`,
  `chrisbanes-skills:compose-component-design`, `chrisbanes-skills:compose-state-and-effects`,
  `android-skills:android-data-layer`, `android-skills:android-testing`,
  `chrisbanes-skills:compose-ui-testing-patterns`, `superpowers:test-driven-development`,
  `chrisbanes-skills:gradle-run`.

## 1. Category picker keeps the current category visible

Problem: the category list is long; once scrolled, the user cannot see which category is selected.

- `feature/movimientos/ui/CategoryPickerSheet.kt`: when `selectedId` resolves to a category in the
  catalog (not Otros), show a pinned row between the search field and the `LazyColumn`: a muted
  caption label ("Actual"/"Current") and the selected `CategoryChip` in its selected state. It stays
  while searching. Tapping it closes the sheet (keeps the selection).
- On open (not while searching), scroll the `LazyColumn` so the group that contains the selected
  category is visible (`LazyListState.scrollToItem` to that group's index).
- The selected chip in the results keeps its selected state.

## 2. Barely perceptible "optional" in the Tags header

- Header shows "Tags" with the same section style as "Categoría", followed by a small "opcional"
  marker in the lowest-emphasis foreground tier available and caption/smaller type, on the same
  baseline. No middle dot.
- Copy: `movimiento_tags_section` becomes just "Tags" in all locales; add a neutral `optional_marker`
  key ("opcional" / "optional" / "opcional" / "opcional") in all four `strings.xml`. Semantics: the
  header reads as one heading, e.g. "Tags, opcional".
- Owner: `core/ui/tag/EtiquetaTagField.kt` header (shared by every surface).

## 3. Tag picker usable with the keyboard open

Problem (seen on device): the sheet is content-sized; with few tags it is short and the keyboard
covers the search field and results.

- `core/ui/tag/EtiquetaPickerSheet.kt`: the results area gets a fixed height equal to the category
  picker's (`TallybookTheme.dimensions.categoryPickerResultsMaximumHeight`; add a neutral shared
  token if renaming is cleaner), so the sheet opens tall with the search near the top, like the
  category picker. Keep `pickerSearchHeight` behavior.
- Move "Listo (n)" to the title row as a header action (text button aligned end), so it stays
  reachable while the keyboard is open; remove the bottom button. The limit hint stays under the
  search/selected area.
- Empty states inside the fixed area, vertically centered:
  - No tags at all and no query: tag icon, `tags_empty`-style title and a one-line hint to type a
    name to create the first one (new key `tag_picker_empty_hint`).
  - Query without results: the "no tags match" text plus a prominent full-width create action
    `tag_picker_create` (not a small text button). Keep the `failedCreateNombre` error under it.
- Apply the same "Done stays visible" check to nothing else; the category picker closes on tap.

## 4. Tag description (optional, max 140)

- Model/persistence: `Etiqueta(id, nombre, descripcion: String?, createdAt)`;
  `ETIQUETA_DESCRIPCION_MAX = 140` next to the tag rules. Add `descripcion TEXT` to `etiqueta` in
  **both** `Etiqueta.sq` and the existing unreleased `5.sqm` (this feature's single migration; do not
  add a new migration), regenerate `databases/6.db`, keep `verifyMigrations` green.
- Repository: `create(nombre, descripcion = null)`, `update(id, nombre, descripcion)` (replaces
  rename; normalization: trim, blank → null, collapse repeated whitespace like other optional texts;
  > 140 → `InvalidInput`). Quick-create from the movimiento picker passes no description.
- Tags screen sheet (`feature/tags/**`): add a multi-line description field under the name with a
  counter `n/140` (reuse the category form's description field pattern/strings structure; new keys
  `tag_description_label`, `tag_description_placeholder`, `tag_description_too_long`). Save is enabled
  when name or description changed and both are valid; no-change save closes without writing.
- Search/CSV/rows do not use the description.

## Tests

- Repository: create/update with description, blank → null, > 140 rejected; migration test asserts
  the column exists and is null for migrated data.
- `TagsViewModelTest`: description edit, validation, no-change close.
- Device tests (compile; run only if a device can run them): pinned current category row and
  initial scroll; tag picker Done in header visible; empty and no-results states with create action;
  Tags header marker semantics.

## Exit

`./gradlew :shared:generateCommonMainTallybookDatabaseSchema` then
`:shared:verifyCommonMainTallybookDatabaseMigration :shared:testAndroidHostTest
:shared:compileAndroidDeviceTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64
detekt ktfmtCheck` green. Update `docs/features/tags.md`, `docs/ui/components.md` where these
components are described (current state only).

## Spec deviations

- `Etiqueta` keeps `createdAt` third and adds `descripcion: String? = null` last, matching
  `Categoria`'s optional description and leaving the positional `Etiqueta(id, nombre, createdAt)`
  call sites (fixtures, previews) unchanged. The repository maps the column explicitly.
- Save in the tag sheet is enabled whenever the name and description are valid, not only after a
  change: "no-change save closes without writing" needs a reachable Save, and it keeps the existing
  tested behavior for an unchanged rename.
- The 420 dp token is renamed to the neutral `pickerResultsHeight` (category picker maximum, tag
  picker fixed height, category quick-create body maximum).
- New key `category_picker_current` ("Actual" / "Current" / "Actual" / "Atual") labels the pinned row;
  the spec named the copy but not the key.
- `normalizeOptionalText` moves from `AddMovimientoViewModel.kt` to `core/text/OptionalText.kt` so the
  repository reuses it without depending on a feature package.
- The migration test asserts the `descripcion` column exists and is the only nullable `etiqueta`
  column; schema 5 has no tags, so there are no migrated rows whose value could be read.
