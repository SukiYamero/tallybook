# Wave 5 / Stage 1 / Task 3 — Tombstones, revive and live-only reads

## Objective

Replace hard deletes with tombstones and make every read path ignore them, with no user-visible change:
Undo, counts, uniqueness, Search, History, suggestions and CSV behave as today. Undo revives the same
row; deleting a tag bumps the movimientos that lose it; no-op writes issue no stamp.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md#change-stamps-and-tombstones-stage-1) and the
  Undo contract in `docs/business-logic.md#editing-and-deletion`.
- Skills: `android-dev`, `android-data-layer`, `kotlin-concurrency-and-flow`, `android-testing`,
  `test-driven-development`.
- Depends on task1 (production clock) and task2 (schema 7, stamped writes, view filter).

## Files

- Modify `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`:
  - replace `deleteMovimiento` with `tombstoneMovimiento(stamp, id)` (`... WHERE id = ? AND deleted_at IS NULL`);
  - add `selectStoredMovimientoById` (table row including tombstone state, no links) and
    `reviveMovimiento(<content fields>, stamp, id)`, which writes a row over its tombstone: Undo writes
    the captured content, and an `insert` reusing a tombstoned ID takes the row over (newer write)
    instead of failing on the primary key, as inserting after a hard delete did;
  - `updateMovimiento` adds `AND deleted_at IS NULL`;
  - direct table reads add `deleted_at IS NULL`: `selectMovimientoYears`, `selectCategoryHistory`,
    `sumMovimientosByTipo`, `sumMovimientosPorCategoria`.
- Modify `Categoria.sq`: `selectAllCategorias`, `selectCategoriaById` live only; `deleteCustomCategoria`
  becomes `tombstoneCustomCategoria`; `countMovimientosForCategoria` and `countMovimientosPorCategoria`
  count live movimientos only.
- Modify `Etiqueta.sq`: `selectAllEtiquetas`, `selectEtiquetaById`, `selectExistingEtiquetaIds` live
  only; `deleteEtiqueta` becomes `tombstoneEtiqueta`; add `touchMovimientosForEtiqueta(stamp,
  etiqueta_id)`; counts join live movimientos.
- Modify: `core/data/MovimientoRepositoryImpl.kt` (`deleteAndCapture`, `delete`, `restoreDeleted`,
  `update`), `CategoriaRepositoryImpl.kt` (`delete`, `update`), `EtiquetaRepositoryImpl.kt` (`delete`).
- Test (`shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/`):
  `MovimientoRepositoryTest.kt`, `CategoriaRepositoryTest.kt`, `EtiquetaRepositoryTest.kt`,
  `SearchRepositoryTest.kt`, `HistoryRepositoryTest.kt`; `core/database/DatabaseSchemaTest.kt`
  (`deleteMovimiento removes the row` becomes a tombstone test); integration tests
  `CategoryTagsIntegrationTest.kt`, `HistoryIntegrationTest.kt` must pass unchanged.

## Contracts

Public repository interfaces (`MovimientoRepository`, `CategoriaRepository`, `EtiquetaRepository`) keep
their signatures and result types (`MovimientoRestoreResult`, `CategoriaDelete`, `EtiquetaWrite`).
Stamps stay persistence metadata in this stage: `Movimiento`, `Categoria` and `Etiqueta` gain no stamp
fields, so the existing equality-based restore comparison keeps comparing content only.

## Exact rules

Owned by [README — change stamps and tombstones](../../README.md#change-stamps-and-tombstones-stage-1).
How they apply to the current code:

- `deleteAndCapture` captures through `selectMovimientoById` (live view, with links), then
  `tombstoneMovimiento` and `deleteMovimientoEtiquetas` in the same transaction. `delete(id)` tombstones
  too; a missing or already tombstoned ID stays a no-op as today (`value in 0L..1L`).
- `restoreDeleted(snapshot)` inside one transaction:
  1. live row via `selectMovimientoById`: identical to the snapshot → `AlreadyPresent`, otherwise
     `Conflict` (unchanged, including the unreadable-row case);
  2. else stored row via `selectStoredMovimientoById` is a tombstone whose fields equal the snapshot's
     (tipo, monto minor + moneda, fecha, categoria, titulo, descripcion, created_at; links excluded,
     because tombstones own none) → `reviveMovimiento` with the surviving-reference fallback from
     `withSurvivingReferences` (Otros when the categoria is gone or tombstoned) and re-insert surviving
     live tags → `Restored`;
  3. a tombstone with different fields → `Conflict`;
  4. no row at all → existing insert path → `Restored`.
- `update` returns `Updated` without writing (and without a stamp) when the normalized changes equal
  the stored row, and `NotFound` for tombstones. Same no-op rule for custom categoria updates;
  `setOculta` and etiqueta `update` already skip unchanged writes — keep them stamp-free.
- Etiqueta `delete`: one stamp; `touchMovimientosForEtiqueta` on live movimientos linked to it, then
  `deleteLinksForEtiqueta`, then `tombstoneEtiqueta`. A tombstoned ID is `unknownEtiqueta`.
- Categoria `delete`: in-use check counts live movimientos only; deleting tombstones the row. A captured
  movimiento in a pending Undo no longer counts, exactly as today (see the comment above
  `withSurvivingReferences`).
- `findDuplicate` in both repos reads live rows only, so a tombstoned name can be reused with a new ID.
- `localizeSeeded` keeps writing through `updateCategoriaNombre` with no stamp.
- Every direct-table aggregate/count includes `deleted_at IS NULL` so the partial indexes apply; keep or
  update the index comments in the `.sq` files to match the measured plans.

## TDD implementation

- [x] RED: movimiento — delete leaves a tombstone (row present, `deleted_at = updated_at`, no links,
      absent from recent/range/filtered/years/summary/category history/getAll).
- [x] RED: restore — revive keeps ID and `createdAt`, issues a newer stamp, restores surviving tags only,
      falls back to Otros when the categoria was tombstoned; tombstone with different content →
      `Conflict`; live identical → `AlreadyPresent`; live different → `Conflict`.
- [x] RED: update of a tombstone → `NotFound`; unchanged update writes nothing (stamp unchanged,
      observers not re-emitted where the existing reactive tests can observe it).
- [x] RED: etiqueta delete bumps each affected live movimiento's `updated_at` to the delete stamp,
      removes links, tombstones the tag; a new tag with the same name gets a new ID.
- [x] RED: categoria delete tombstones; counts/observe exclude it; name reuse creates a new ID.
- [x] GREEN: implement query and repository changes.
- [x] Re-run Search/History/CSV/integration suites unchanged; re-check the `EXPLAIN QUERY PLAN` tests
      from task2 against the direct-table aggregates touched here.
- [ ] Checks below.

## Checks

`./gradlew :shared:testAndroidHostTest --tests '*RepositoryTest*' --tests '*DatabaseSchemaTest*'` while
iterating, then `./gradlew :shared:testAndroidHostTest`, `./gradlew :shared:compileKotlinIosSimulatorArm64`,
`./gradlew :androidApp:assembleDebug`, `./gradlew detekt ktfmtCheck`. Then the stage gate in
[tasks.md](../tasks.md#exit-gate), including the physical Xiaomi smoke.

## Blocks

Blocked by task1 and task2. Closes Stage 1; blocks Stage 2/task2 onwards.
