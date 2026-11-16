# Stage 1 — Recurrence data

Blocks Stage 2. Contract: [README](../README.md) § Rules.

## Objetivo

Persist recurrences, compute their occurrences and confirm, skip, edit, delete and undo them through
one repository, with host tests.

## Archivos

- `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/10.sqm` — creates
  `recurrencia` and `recurrencia_etiqueta`, adds `movimiento.recurrencia_id`. Follow `9.sqm` and the
  existing migration tests (`DatabaseSchemaTest`, `verifyCommonMainTallybookDatabaseMigration`, the
  `databases/*.db` snapshots). `movimiento` gains a column, so its rebuild must also recreate the
  views and indexes that depend on it, matching a fresh install.
- `Recurrencia.sq` (new) — reads and writes; `Movimiento.sq` — the insert carries `recurrencia_id`.
- `core/model` — `Recurrencia`, `Frecuencia`, `RecurrenciaDraft`/write outcomes as the codebase's
  `Cuenta`/`Movimiento` equivalents do, and pure occurrence rules (`RecurrenciaRules` or similar):
  occurrence n from the anchor, the first occurrence after a date, pending occurrences through
  `today`.
- `core/data` — `RecurrenciaRepository` + `Impl` (injected dispatcher, `DataOutcome`, change stamps
  through `ChangeClock`, transactions spanning `recurrencia`, `recurrencia_etiqueta` and
  `movimiento`). Operations: observe live recurrences with their pending occurrences for a `today`;
  observe the pending count; create together with the first movimiento; confirm (amount, fecha) the
  oldest pending; skip the oldest pending; update fields; update schedule (rejected with pending);
  delete; undo confirm/skip with stamp-based conflict detection. Reuse `MovimientoRepositoryImpl`'s
  validation of tipo/medio/cuenta/categoría/tags instead of duplicating it.
- `di/RepositoryModule.kt` — bind the repository.
- Bootstrap: `ChangeClock` preparation reads the maximum stamp of every stamped table; add
  `recurrencia`.

## Tests (androidHostTest / commonTest, extend existing files where they fit)

- Rules: monthly anchor on the 31st across February and 30-day months, annual 29 Feb, weekly; first
  occurrence after a back-dated and a future fecha; pending through today.
- Repository: create with movimiento (one stamp shared by both rows), confirm with a different amount
  (movimiento inserted with `recurrencia_id`, template amount updated, `proximaFecha` advanced, one
  stamp), skip, undo of each, undo conflict after an edit, schedule edit rejected with pending,
  delete keeps movimientos, tombstones excluded from reads, invalid cuenta/categoría surfaces as the
  movimiento write error.
- Migration from the `10.db` snapshot equals a fresh install.

## Fuera de alcance

UI, strings, navigation (Stage 2).
