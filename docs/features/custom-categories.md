# Custom categories

Users create their own root categories, manage every category from a Categories screen reached from
Profile, and hide the ones they do not use. Product rules live in
[business-logic.md](../business-logic.md#custom-categories); visual ownership and tokens live in
[UI architecture](../ui/architecture.md).

## Interaction

- **Categories screen** (Profile → Categories): a secondary destination over Profile with the bottom
  bar hidden and Back to Profile. The header aligns with other page headers (`TallybookScreenHeader`),
  with a ghost back arrow and an icon-only add action. Below the search field, chips grouped like the
  picker: the user's categories first (sorted by name), then one block per predefined family; a search
  shows one flat block. Each chip shows its count (primary currency, a parent including its children);
  hidden chips are dimmed with an eye-off mark. The list starts `space8` below the search field so
  scrolled chips never clip flush against it.
- **Category sheet**: tapping a chip opens one sheet at a time. Custom categories open the editor
  (name, icon, tint, description) with a header eye and trash, like edit movimiento; predefined ones
  open a read-only sheet (avatar, name, count) with the eye, absent for Otros. The eye shows the
  current state (eye visible, eye-off hidden) and is labeled with the action. "View N movements"
  appears when the count is above 0: it closes the sheet and, once hidden, opens Search with only
  that category over All history. Delete confirms when unused and offers "Hide instead" when in use.
  A duplicate name in the editor offers "Open <name>", which swaps the sheet to that category.
- **Category form** (`CategoryForm`): the name suggests the icon and tint from the smart-entry
  dictionary until the user picks either by hand; tint options preview the current icon in each tint.
  Inputs cap at 40 (name) and 340 (description) characters. The full editor and the compact
  quick-create pane share `TallybookSheetForm`, the same keyboard handling as the movimiento form:
  the focused field stays above the keyboard, the footer gives way while it is open, and closing it
  keeps the scroll position (a body that fits at rest returns to the top).
- **Quick-create**: a picker search without results offers "Create “<query>”", which switches the
  picker sheet to the compact form (name and icon; tint and description behind "More options"). The
  created category is selected within the six-category limit; a duplicate offers "Use <name>",
  showing a hidden category again first.

## Integration

- Schema 4 (`3.sqm`) rebuilds `categoria` with `descripcion`, `origen` (`sistema`/`usuario`) and
  `oculta`, because SQLDelight 2.3.2 cannot resolve a `CHECK` on a column added with
  `ALTER TABLE ... ADD COLUMN`. Custom ids come from `IdGenerator`, never `cat_*`, and
  `localizeSeeded` only renames `sistema` rows.
- `CategoriaRepository` owns create/update/hide/delete and counts; validation, duplicate checks and
  writes share one transaction, and no-op writes do not notify observers.
- Pickers exclude Otros and hidden categories except those already selected
  (`pickerExcludedCategoryIds`); entry suggestions only consider visible categories.
- Navigation keeps one `secondary` slot (UI catalog or Categories). Search receives category requests
  as a one-shot `SearchCategoryRequest` applied by its retained ViewModel.

## Verification boundaries

The merged tree passed Detekt, ktfmtCheck, `verifyCommonMainTallybookDatabaseMigration`, the shared
host tests, Native production and test compilation, `compileAndroidDeviceTest` and the Android debug
APK. A migration test upgrades a populated schema-3 snapshot. Independent reviews covered the data
layer, the Categories screen and sheets, and the final branch. Manual checks on the Xiaomi Mi 11X
covered the in-place schema 3 → 4 upgrade, create, duplicate detection, edit, hide/show, delete,
quick-create from the picker, Search from a category, Back, header alignment and keyboard scrolling,
in English and Spanish.

Not covered: iOS runtime and the new device test fixtures (compiled only; both tracked in the
[backlog](../backlog.md)), TalkBack and enlarged-text passes.
