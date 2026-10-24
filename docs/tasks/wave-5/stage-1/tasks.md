# Wave 5 / Stage 1 — Sync-ready persistence

Track B, branch `feat/sync-ready-schema`. Rules live in
[Change stamps and tombstones](../README.md#change-stamps-and-tombstones-stage-1); this stage only
assigns them to tasks.

## Objective

Give every persisted root entity (`movimiento`, `categoria`, `etiqueta`) a monotonic change stamp and a
tombstone, without any visible behaviour change: screens, counts, Search, export and Undo behave exactly
as today, but deletes leave tombstones and every mutation records who changed what and when.

## Tasks

- [**task1 — ChangeStamp, ChangeClock and installation node**](task1/task.md): pure stamp model, the
  hybrid logical clock, the device-scoped installation ID and its backup exclusion. No schema access.
- [**task2 — Migration to schema 7 and stamped writes**](task2/task.md): `6.sqm`, fresh DDL, backfill,
  live-row views and partial indexes, `7.db` snapshot, migration tests, and the mechanical plumbing that
  keeps the build green: every insert/update passes `changeClock.next()`, bootstrap prepares the clock.
- [**task3 — Tombstones, revive and live-only reads**](task3/task.md): deletes tombstone, Undo revives,
  reads/counts/uniqueness/suggestions/export exclude tombstones, tag deletion bumps movimientos, no-ops
  issue no stamp. **Depends on task1 and task2.**

Order: task1 commits its **contract first** (`ChangeStamp`, `ChangeClock` interface, a `TestChangeClock`
fixture) → task2 ∥ the rest of task1 (production clock, node ID, backup rules) → task3. The new
NOT NULL `updated_at` changes the generated query signatures, so task2 cannot compile without the stamp
types; the SQL itself can be drafted before. task2 is the only owner of `.sqm`, DDL and
`databases/*.db`; `.sq` query blocks and repository impls are owned by task2 and then task3, in sequence.
If task3 needs a DDL change, it goes back to the task2 owner.

## Exit gate

- Table-driven stamp/clock tests cover encoding order, ties, backward wall clock and reseeding.
- `DatabaseSchemaTest` migrates the `6.db` snapshot to 7 with backfilled stamps, and the migrated schema
  equals a fresh install (`schemaObjects()` comparison, as existing migration tests do).
- Existing repository, History, Search, CSV and integration tests pass **unchanged in their
  assertions**; new tests prove tombstones, revive, `Conflict`, tombstoned-name reuse, tag deletion
  bumping movimiento stamps and no-op writes issuing no stamp.
- `EXPLAIN QUERY PLAN` evidence that the recent/filtered reads still use `movimiento_recent` without a
  temporary B-tree after the partial index change.
- `./gradlew :shared:testAndroidHostTest`, `./gradlew :shared:compileKotlinIosSimulatorArm64`,
  `./gradlew detekt ktfmtCheck` and `./gradlew :androidApp:assembleDebug` pass; migration
  verification (`verifyMigrations`) passes with the build.
- Physical Xiaomi smoke (3–5 min): upgrade over the existing mock dataset, check Home/History/Search,
  delete + Undo a movimiento, delete a tag, export CSV.

## Intermediate review (required)

This stage is a migration and repository-contract boundary that Stage 2 builds on, so one bounded
independent review runs before merge. Focus:

- No read path, count, aggregate, uniqueness check, suggestion source or export still sees tombstones.
- Every mutation issues exactly one stamp per transaction; no-ops and `localizeSeeded` issue none.
- Undo: identical tombstone revives with the same ID; differing row or tombstone is `Conflict`; the
  Otros and surviving-tag fallbacks are unchanged.
- The node ID cannot leak through Android auto backup/device transfer, and is never written into
  DataStore files that hold financial preferences.
- Migration preserves every row and matches fresh DDL; index comments in `.sq` files still describe
  real plans.
