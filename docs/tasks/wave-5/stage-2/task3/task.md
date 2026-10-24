# Wave 5 / Stage 2 / Task 3 — Write repositories and invariants

## Objective

Own every write of accounts and transfers and enforce the cross-table invariants of movimientos —
including restore outcomes — through repositories, with a change stamp per mutation and tombstones on
delete, as Stage 1 established. The default cash seed and `defaultSelection` are task2's.

## Context and skills

- **Blocked by task2** (v8 schema, `Movimiento.cuentaId/medio`) and therefore by Stage 1. Runs in
  parallel with task4; you own the files below, task4 owns `Saldo.sq`/`SaldoRepository`.
- Read the [wave README](../../README.md) (Cuenta, MedioPago, Movimiento changes, Transferencia,
  Change stamps), `CategoriaRepositoryImpl`/`EtiquetaRepositoryImpl` (normalize → duplicate check →
  `*Write.Saved/Duplicate`, `CategoriaDelete.InUse`), `MovimientoRepositoryImpl` (validation,
  `missingReference`, `deleteAndCapture`/`restoreDeleted` after Stage 1), `LocalDataBootstrapper`,
  `CategorySeedProvider` (localized seed strings) and `di/RepositoryModule.kt`.
- Skills: `android-dev`, `android-data-layer`, `kotlin-concurrency-and-flow`, `android-testing`,
  `test-driven-development`.

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CuentaRepository.kt` + `CuentaRepositoryImpl.kt`
- Create: `.../core/data/TransferenciaRepository.kt` + `TransferenciaRepositoryImpl.kt`
- Create: `.../core/model/{CuentaDraft,CuentaWrite,CuentaDelete,CuentaArchive,TransferenciaUpdateResult}.kt`
- Modify: `.../core/model/MovimientoMutationResult.kt` — generalize `MovimientoRestoreResult` to
  `RestoreResult` (+ `MissingCuenta`) shared by movimientos and transfers
- Modify: `.../core/data/MovimientoRepository.kt` + `MovimientoRepositoryImpl.kt`
- Modify: `.../core/database/{Movimiento,Cuenta,Transferencia}.sq` — mutation and lookup queries only
- Modify: `.../di/RepositoryModule.kt`
- Modify: `.../feature/movimientos/viewmodel/ManageMovimientoViewModel.kt` — rename to `RestoreResult`
  and map `MissingCuenta` to the existing restore-failed informational feedback (the complete
  behaviour; Stage 3/task2 only carries it through the queue extraction)
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/{FakeCuentaRepository,FakeTransferenciaRepository}.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/{CuentaRepositoryTest,TransferenciaRepositoryTest}.kt`
- Test: extend `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryTest.kt`;
  `commonTest/.../feature/movimientos/viewmodel/{MovimientoUndoTest,ManageMovimientoFeedbackTest}.kt` for `MissingCuenta`

## Contracts

```kotlin
interface CuentaRepository {
  fun observeAll(): Flow<DataOutcome<List<Cuenta>>>            // live rows, archived included
  suspend fun getById(id: String): DataOutcome<Cuenta?>
  suspend fun create(draft: CuentaDraft): DataOutcome<CuentaWrite>   // Saved | Duplicate
  suspend fun update(id: String, draft: CuentaDraft): DataOutcome<CuentaWrite> // + CurrencyLocked
  suspend fun setArchivada(id: String, archivada: Boolean): DataOutcome<CuentaArchive> // Updated | LastActive
  suspend fun delete(id: String): DataOutcome<CuentaDelete>          // Deleted | InUse(count) | LastActive
}

interface TransferenciaRepository {
  suspend fun getById(id: String): DataOutcome<Transferencia?>
  suspend fun insert(transferencia: Transferencia): DataOutcome<Unit>
  suspend fun update(id: String, changes: Transferencia): DataOutcome<TransferenciaUpdateResult>
  suspend fun deleteAndCapture(id: String): DataOutcome<Transferencia?>
  suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult>
  suspend fun getAll(): DataOutcome<List<Transferencia>> // live rows; CSV export and future backup
  /** Origin of the newest live transfer if active and not CREDITO, else the primary cash account. */
  suspend fun defaultOrigin(primary: Moneda): DataOutcome<String>
}

// MovimientoRepository additions (defaultSelection is task2's)
fun observeSummary(moneda: Moneda, range: MovimientoDateRange): Flow<DataOutcome<MovimientoSummary>>
```

Range, year and filter reads of transfers are not part of this task: Stage 5/task1 adds them
together with movimientos in one snapshot, when History and Search consume them.

## Exact rules

- Name normalization, uniqueness per (nombre, moneda) among live accounts, currency lock
  (`CuentaWrite.CurrencyLocked`), archive/last-active/delete rules: README
  [Cuenta](../../README.md#cuenta-account). "Referenced" counts live movimientos and live transfers on
  either side. `saldoInicial` is stored exactly as received (signed); the repository never negates it.
- Movimiento insert/update: account live and not archived (update keeps an archived account only if
  unchanged); medio compatible with account type and movimiento tipo per task1's table;
  `monto.moneda == cuenta.moneda`; on update a different account must have the stored currency.
  Failures are `DataError.InvalidInput`, checked inside the write transaction.
- Restore outcomes: README [Movimiento changes](../../README.md#movimiento-changes) — archived account
  allowed (stays archived, bypassing the insert-time "not archived" check), missing or tombstoned
  account → `MissingCuenta`, account currency differing from the snapshot → `Conflict`; otherwise
  revive/`AlreadyPresent`/`Conflict` exactly as Stage 1 defined for movimientos.
- Transfer validation reuses `TransferenciaRules` against the two loaded accounts in the transaction.
- `TransferenciaRepository.defaultOrigin(primary)`: README
  [Transferencia](../../README.md#transferencia-transfer-between-own-accounts) Pay card rule.
- Every mutation issues exactly one stamp from `ChangeClock` inside its transaction; normalized no-ops
  write nothing.

## TDD

- [x] RED/GREEN `CuentaRepositoryTest`: create/duplicate (accent/case, same name in another currency
      allowed, tombstoned name reusable), update currency while referenced → `CurrencyLocked` and
      unreferenced → saved, archive last active → `LastActive`, delete in use → `InUse`, delete unused
      → tombstone, signed `saldoInicial` stored as received, stamps advance.
- [x] RED/GREEN `MovimientoRepositoryTest`: every incompatible medio/account/tipo is rejected;
      archived account on insert rejected and kept on untouched update; cross-currency account change
      rejected; restore into an archived account succeeds; restore with deleted account →
      `MissingCuenta`; restore after the account's currency changed → `Conflict`.
- [x] RED/GREEN `TransferenciaRepositoryTest`: validation errors, cross-currency amounts, `getAll`
      excludes tombstones, `defaultOrigin` fallbacks, delete → tombstone → restore revives with a new
      stamp, the same restore outcomes as movimientos.
- [x] Create `FakeCuentaRepository`/`FakeTransferenciaRepository` in `commonTest/.../core/data/` and
      update every existing `MovimientoRepository` fake for the new methods.
- [x] Bind both repositories in `RepositoryModule`; Koin graph test still resolves.
- [x] `./gradlew :shared:testAndroidHostTest`, `:shared:compileKotlinIosSimulatorArm64`, `detekt ktfmtCheck`.

## Implementation notes

- `defaultOrigin` returns `DataOutcome<String?>`: after the newest transfer's origin and the primary
  cash cuenta it falls back to the first active non-credit cuenta in the primary currency, then in
  any currency (one ranked SQL read, `selectDefaultOrigin`); null only when no active non-credit
  cuenta exists, e.g. a lone credit card.
- Transfers accept archived cuentas on either side (README: "both live"), so an archived card can
  still be paid; tombstoned or unknown cuentas are invalid input. Restore skips only the "not future"
  check, which a captured date cannot newly fail.
- Movimiento update keeps a stored (cuenta, medio) pair while untouched, as it keeps an archived
  cuenta: a cuenta's type may change after the fact, and an edit of other fields must not be refused
  for it. `medio.allows(tipo)` is always enforced.
- `CuentaDelete.LastActive` wins over `InUse` (archiving would be refused too). `update` checks
  `Duplicate` before `CurrencyLocked`.
- `MissingCuenta` maps to `RestoreFailed` with the snapshot released: `MovimientoFeedback.hasAction`
  now follows whether a snapshot is held, so no Retry is offered. The existing copy still ends with
  "Try again"; a dedicated message is a copy decision for Stage 3.
- `observeSummary(moneda, range)` is added next to the all-time overload; Home keeps the all-time
  one until its display task switches to the month.

## Blocks

Blocked by task2. Blocks Stage 3 (accounts and transfer UI) and Stage 4 (entry selection).
