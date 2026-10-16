# Wave 4 / Task 1 — Multi-category data

## Goal

Persist one to six ordered categories per movimiento and keep every read, aggregate, search, export
and Undo path correct, following
[Multiple categories](../../../../business-logic.md#multiple-categories-per-movimiento) and
[Hierarchy and filtering](../../../../business-logic.md#hierarchy-and-filtering).

## Schema and migration

- New table `movimiento_categoria(movimiento_id TEXT NOT NULL, categoria_id TEXT NOT NULL,
  posicion INTEGER NOT NULL, PRIMARY KEY (movimiento_id, categoria_id))` plus an index on
  `categoria_id` for filtering.
- `2.sqm` (schema 2 → 3): create the table, copy each `movimiento.categoria_id` at `posicion 0`, then
  rebuild `movimiento` without `categoria_id` (create new table, copy, drop, rename, recreate
  `movimiento_recent`). Android 12 ships SQLite 3.32 without `DROP COLUMN`. Generate the `3.db`
  schema snapshot so `verifyMigrations` covers it, and add a migration test with real v2 data.
- SQLite foreign keys are not enabled; the repository owns referential integrity: insert, update,
  delete and restore write both tables in one transaction.

## Reads and writes

- Every movimiento read returns `categoriaIds` ordered by `posicion`, from the same transaction or
  statement as the row, so list membership, counts and categories come from one snapshot. Observed
  queries must invalidate on `movimiento_categoria` changes. Choose between a correlated subquery and
  a second keyed query by measurement; `group_concat` ordering is not guaranteed on SQLite 3.32, so
  any concatenation must be ordered explicitly or re-sorted by `posicion`.
- `insert`, `update` (replace the set atomically), `deleteAndCapture` (snapshot includes categories),
  `restoreDeleted` (identical-row check includes categories), `delete`.
- Validation at the repository boundary: 1..`MAX_CATEGORIAS_POR_MOVIMIENTO` distinct non-blank IDs;
  otherwise `DataError` invalid input, nothing written.
- Search: `selectFilteredMovimientosByCategory` matches through `EXISTS` on the join table with the
  existing parent → children roll-up. Text search (`MovimientoSearchSnapshot`) matches any category
  name of the row.
- `sumMovimientosPorCategoria`: full amount in every category of the movimiento.
- `selectCategoryHistory`: one row per (movimiento, categoria) so the suggestion index learns every
  assigned category.
- CSV: the category column lists every name in display order joined with `" · "`.

## Keeping callers compiling

Update all existing callers (Home, History, Search projections, Add/Manage view models, catalog
fixtures, tests and fakes) to the new model using the first category where a single one is shown
today. Do not redesign UI here; wave 5 replaces those reads with the tag field and "+N".

## Files

`Movimiento.sq`, new `2.sqm`, `databases/3.db`, `Movimiento.kt`, `MovimientoChanges.kt`,
`MovimientoMapper.kt`, `MovimientoRepositoryImpl.kt`, `MovimientoRepository.kt` (only if signatures
must change), `export/MovimientoCsv.kt`, search snapshot/projection code, and the minimal caller
updates above. Do not touch `core/ui/category/`, `CategoryPickerSheet.kt` or new task2 files.

## Tests

Migration from a v2 database with movimientos (categories preserved, order, indexes), repository
round-trips with 1 and 6 categories, invalid sets rejected, update replacing the set, delete/Undo
restoring categories and conflict detection on a different category set, Search roll-up through any
category, text match on a secondary category name, per-category sums counting the full amount, CSV
joined names, history rows per category, reactive emission when only categories change.

## Done when

`detekt ktfmtCheck :shared:testAndroidHostTest :shared:compileKotlinIosArm64
:shared:compileTestKotlinIosSimulatorArm64 :shared:compileAndroidDeviceTest :androidApp:assembleDebug`
pass on the final tree.
