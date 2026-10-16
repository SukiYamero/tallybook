# Task 4 — Picker: hidden filtering, custom block, quick-create

Depends on task3. Parallel with task5; shared file: `di/ViewModelModule.kt` (each task adds only
its own `viewModel { }` entry).

## Objective

Hidden categories disappear from movimiento pickers and suggestions; a search without results offers
creating the typed category, which becomes selected.

## Files

- `feature/movimientos/ui/CategoryPickerGroups.kt`: custom (`usuario`) categories form one block
  sorted by `nombreKey`, before the predefined families; search keeps the single flat block.
- Picker callers (`MovimientoForm.kt`, `AddMovimientoSheet.kt`, `MovimientoCategoryPicker.kt`):
  `excludedIds = PICKER_EXCLUDED_CATEGORY_IDS + hidden ids - selected ids`, so a hidden category
  already on the movimiento stays visible and removable. One helper owns this set; no copy per caller.
- `AddMovimientoViewModel` suggestion `availableIds` excludes hidden ids (`ManageMovimientoViewModel`
  too if it suggests).
- `CategoryPickerSheet.kt`: in the no-results state, below the existing message, a
  `TallybookTextButton` "Create “<trimmed query>”" (hidden when the trimmed query is blank). It
  switches the sheet to a compact form pane (title + back arrow, Back returns to results with the
  query kept). The picker stays stateless: it receives the pane state and callbacks.
- `feature/categories/viewmodel/QuickCategoryViewModel.kt` (new): owns `CategoryDraftState`,
  `create`, and a `Channel(BUFFERED).receiveAsFlow()` effect `Created(id)`; on `Duplicate` sets the
  duplicate and the form shows "Use <name>" (unhides it first when hidden; no action for Otros).
  Obtained with `koinViewModel` in the picker host's owner (Add session owner, management owner),
  keyed per host. On `Created`/use, the host calls the existing `onSelectionChange` through
  `CategoryPickerSelection` (respects the max of 6) and returns to the results pane.

## Edge cases

Save twice (disabled while `saving`); dismiss the sheet mid-save (effect collected after reopen is
ignored once the pane is gone — the category still exists); IME open while the pane switches;
picker reopened → results pane with empty query.

## Tests

- commonTest: `CategoryPickerGroupsTest` (custom block order, hidden excluded, selected hidden kept);
  `QuickCategoryViewModelTest` (create → effect, duplicate → use, hidden duplicate → unhide + use,
  Otros duplicate → no action, failure keeps the draft).
- `AddMovimientoViewModelTest`: hidden categories never suggested.
- androidDeviceTest picker fixture: no-results → create → selected (compile-only).

## Exit

`testAndroidHostTest`, `detekt`, `ktfmtCheck`, iOS compile, `assembleDebug`.
