# Wave 5 / Stage 1 / Task 2 — Migration to schema 7 and stamped writes

## Objective

Move the database from schema 6 to 7: `updated_at` and `deleted_at` on `movimiento`, `categoria` and
`etiqueta`, backfilled for existing rows, with live-row views and partial indexes. Plumb a stamp into
every existing insert/update so the build stays green and every write is stamped. Tombstone semantics
(deletes, revive, live-only reads beyond the view) belong to task3.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md#change-stamps-and-tombstones-stage-1) and its
  [schema plan](../../README.md#schema-plan).
- Skills: `android-dev`, `android-data-layer`, `android-testing`, `test-driven-development`.
- Pattern to follow: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/5.sqm`
  rebuilds `movimiento` so migrated DDL equals a fresh install, dropping the view first because renaming
  a table rewrites dependent views. `verifyMigrations` is on in `shared/build.gradle.kts`.
- Fixtures: `withSnapshot(version)` in
  `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/database/DatabaseSchemaTest.kt` copies
  `src/commonMain/sqldelight/databases/<n>.db`; `freshSchema()`/`schemaObjects()` compare DDL.
- Depends on task1's contract commit (`ChangeStamp`, `ChangeClock`, `TestChangeClock`).

## Files

- Create: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/6.sqm`
- Create (generated, committed): `shared/src/commonMain/sqldelight/databases/7.db` — discover the
  generating task with `./gradlew :shared:tasks --all | grep -i schema`; never hand-edit it.
- Modify DDL and the write statements in `Movimiento.sq` (`insertMovimiento`, `updateMovimiento`),
  `Categoria.sq` (`insertCategoriaIfAbsent`, `insertCategoria`, `insertCustomCategoria`,
  `updateCustomCategoria`, `updateCategoriaOculta`; `updateCategoriaNombre` stays stamp-free),
  `Etiqueta.sq` (`insertEtiqueta`, `updateEtiqueta`).
- Create: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/ChangeStamp.sq` with
  `selectMaxChangeStamp` (max `updated_at` across the three tables, tombstones included).
- Modify: `core/data/MovimientoRepositoryImpl.kt`, `CategoriaRepositoryImpl.kt`,
  `EtiquetaRepositoryImpl.kt` (constructor `ChangeClock`, pass `next()` / `ChangeStamp.SEED`),
  `core/data/LocalDataBootstrapper.kt` (`changeClock.prepare { selectMaxChangeStamp }` after seeding),
  `di/RepositoryModule.kt`.
- Test: `DatabaseSchemaTest.kt` (new migration test; existing tests updated only where generated
  signatures changed), `LocalDataBootstrapperTest.kt`, and repository test constructors switched to
  `TestChangeClock` without changing their assertions.

## Contracts

```sql
-- fresh DDL, each of movimiento / categoria / etiqueta
updated_at TEXT NOT NULL,  -- no DEFAULT: a forgotten stamp must fail, not silently become SEED
deleted_at TEXT

CREATE VIEW movimiento_con_etiquetas AS SELECT ... FROM movimiento WHERE movimiento.deleted_at IS NULL;
CREATE INDEX movimiento_recent ON movimiento(fecha DESC, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;
CREATE INDEX movimiento_by_categoria ON movimiento(categoria_id) WHERE deleted_at IS NULL;
```

```kotlin
class LocalDataBootstrapper(..., private val changeClock: ChangeClock, private val database: TallybookDatabase)
```

If injecting the database into the bootstrapper is rejected in review, pass a
`suspend () -> ChangeStamp?` reader from `RepositoryModule` instead; the clock never imports SQLDelight.

## Exact rules

- Rebuild all three tables (rename → create → copy → drop) so migrated and fresh DDL are identical;
  drop and recreate `movimiento_con_etiquetas` and every index around the rebuild. Keep the existing
  index comments true; update the `movimiento_recent` comment to mention the partial condition.
- Backfill, per [README](../../README.md#change-stamps-and-tombstones-stage-1):
  - `movimiento`, `etiqueta`: `<created_at as epoch millis>-0000-0000000000000000`, computed in SQL
    with `printf('%013d-0000-0000000000000000', max(0, COALESCE(CAST(round((julianday(created_at) -
    2440587.5) * 86400000) AS INTEGER), 0)))`, so a pre-1970 instant or a value SQLite cannot parse
    yields millis 0 instead of a negative or NULL stamp. Prove it on `created_at` with no fraction,
    with nanosecond fraction, on the legacy default `1970-01-01T00:00:00Z` (→ all zeros millis), on a
    pre-1970 instant and on an out-of-range year; every result must pass `ChangeStamp.parse`.
  - `categoria` (no `created_at`): `ChangeStamp.SEED` for system and custom rows.
  - `deleted_at` is NULL for every migrated row.
- Seed inserts (`insertCategoriaIfAbsent` from `ensureSeeded`) write `SEED`; every other insert/update
  writes `changeClock.next()` once per transaction, shared by all rows written in it.
- The view excludes tombstones; nothing else changes behaviour in this task (deletes stay hard deletes
  until task3 replaces them).
- Partial indexes are used only when a query's WHERE implies `deleted_at IS NULL`. The view supplies it
  for view reads; direct table reads are task3's.
- `LocalDataBootstrapper.initialize` order: preferences → `ensureSeeded` → `changeClock.prepare`.
  Startup fails through the existing `DataOutcome` path if preparation fails.

## TDD implementation

- [ ] RED: `DatabaseSchemaTest` — `migration from schema 6 backfills change stamps and keeps every row`
      from `6.db`: movimientos (with/without fraction, legacy default, pre-1970, out-of-range year),
      etiquetas with links, seeded and
      custom categorias; assert stamps, NULL tombstones, links intact, `schemaObjects()` equals
      `freshSchema()`.
- [ ] GREEN: write DDL changes and `6.sqm`; generate `7.db`; confirm `verifyMigrations` passes.
- [ ] RED: `EXPLAIN QUERY PLAN` assertions that `selectRecentMovimientosByMoneda` and
      `selectFilteredMovimientos` scan `movimiento_recent` without `USE TEMP B-TREE FOR ORDER BY`.
- [ ] GREEN: adjust only if the plan regressed (do not add `+` tricks without a failing plan test).
- [ ] Plumb `ChangeClock` into the three repositories and the bootstrapper; switch test constructors to
      `TestChangeClock`; add a bootstrapper test proving `prepare` receives the stored maximum.
- [ ] Add one write test per repository asserting a stamped insert/update reads back the issued stamp.
- [ ] Checks below.

## Checks

`./gradlew :shared:testAndroidHostTest --tests '*DatabaseSchemaTest*' --tests '*Bootstrapper*'` while
iterating, then `./gradlew :shared:testAndroidHostTest`, `./gradlew :shared:compileKotlinIosSimulatorArm64`,
`./gradlew :androidApp:assembleDebug` (runs migration verification), `./gradlew detekt ktfmtCheck`.

## Blocks

Blocked by task1's contract commit. Blocks task3 and Stage 2/task2 (`7.sqm` builds on schema 7).
