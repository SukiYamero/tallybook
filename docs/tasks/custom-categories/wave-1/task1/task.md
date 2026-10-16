# Task 1 — Data layer: schema 4, domain fields, repository writes

Blocks: task3, task4, task5. Parallel with task2 (no shared files).

## Objective

Persist custom categories, origin and hidden state; expose create/update/hide/delete/counts with
the contracts in [README](../../README.md#contracts).

## Files

- `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/3.sqm` (new) and
  `Categoria.sq`: new columns; named queries `insertCustomCategoria`, `updateCustomCategoria`,
  `updateCategoriaOculta`, `deleteCustomCategoria`, `countMovimientosForCategoria(id)` (any currency,
  direct links), `countMovimientosPorCategoria(moneda)` (primary currency, parent rolls up its
  children, `COUNT(DISTINCT movimiento.id)`). Keep the two positional inserts unchanged.
- `shared/src/commonMain/sqldelight/databases/4.db` generated with
  `:shared:generateCommonMainTallybookDatabaseSchema` (confirm the exact task name with `tasks`).
- `core/model/Categoria.kt`: `descripcion`, `origen`, `oculta` with defaults; `CategoriaOrigen`;
  `CategoriaDraft`, `CategoriaWrite`, `CategoriaDelete`, `CategoriaRules` (new file
  `core/model/CategoriaRules.kt`; `nombreKey` = `normalizeSearchText(nombre.trim())`).
- `core/data/CategoriaRepository.kt` + `CategoriaRepositoryImpl.kt`: new API; `toDomain` maps
  the new columns; `localizeSeeded` filters `origen == sistema`; ids from the injected
  `IdGenerator` (update `di/RepositoryModule.kt`).
- Every `CategoriaRepository` fake in `commonTest`, `androidHostTest` and `androidDeviceTest`
  (≈10: `AddFakeCategorias`, `ManagementCategorias`, `SearchCategorias`, `HistoryCategorias`,
  `ProfileFakeCategorias`, `DisplayCategorias`, the `HomeViewModelTest` fake,
  `NavigationCategories`, …). Prefer one shared in-memory fake in `commonTest` only if it removes
  duplication without touching unrelated tests; otherwise add minimal `error("unused")` members.

## Rules

- `create`/`update`: trim; reject blank, > 40 name chars, > 340 description chars; duplicate key
  against all rows (excluding the row being updated) returns `Duplicate(existing)` without writing;
  `update` of a `sistema` row is rejected.
- `delete`: `usuario` only; inside one transaction count direct links in any currency; `> 0` →
  `InUse(count)` without writing.
- `setOculta`: rejects `cat_otros` and unknown ids; writes only when the value changes.
- Counts: categories with no movimientos are absent from the map (callers default to 0).

## Tests (androidHostTest, `JdbcSqliteDriver`)

- `DatabaseSchemaTest`: fix the v1 fixture (it never creates `categoria`; start from a copy of
  `1.db`); new 3 → 4 test from a copy of `3.db` with seeded categories, movimientos in two currencies
  and multi-category links: rows preserved, defaults `sistema`/`0`/`NULL`, schema equals a fresh
  `Schema.create` (tables, columns, indexes, view).
- `CategoriaRepositoryTest`: create (trim, accent/case duplicate incl. hidden rows and seed names),
  update (self-rename allowed, seed rejected), hide/show (Otros rejected, no-op does not notify),
  delete (unused → gone; used → `InUse`, nothing written), counts (primary currency only, parent
  rollup, distinct per movimiento with two child categories).
- `CategorySeedTest`: `localizeSeeded` never renames a `usuario` row.

## Exit

`verifySqlDelightMigration`, `testAndroidHostTest`, `detekt`, `ktfmtCheck` green; iOS compile of
`shared` green (new fields have defaults, so production call sites compile unchanged).
