# Wave 5 / Stage 2 — Accounts data model

## Objective

Give every movimiento an account (`Cuenta`) and a payment method (`MedioPago`), add transfers between
own accounts (`Transferencia`), and expose balances and the credit-card estimate as SQL read models.
The rules are owned by the [wave README](../README.md#domain-rules); this stage implements them below
the UI. No screen changes beyond keeping the existing creation flow working.

## Tasks

- [**task1 — Accounts domain, wording and resource split**](task1/task.md): pure `core.model` types,
  validation and cycle math; wording glossary; proof that `strings_accounts.xml` generates accessors.
  **Runs in parallel with Stage 1** — touches no schema, repository or existing model file.
- [**task2 — Migration v8 and schema plumbing**](task2/task.md): `7.sqm`, `cuenta`/`transferencia`
  DDL, movimiento rebuild, view, default cash seed (migration and fresh-install bootstrap), minimal
  `cuenta`/`transferencia` insert/select-by-id queries and row mappers, `defaultSelection`, the stamp
  seed read over the new tables, and the mechanical rewrite of every existing query and construction
  site so the build stays green. Depends on Stage 1 merged + task1.
- [**task3 — Write repositories and invariants**](task3/task.md): `CuentaRepository`,
  `TransferenciaRepository`, movimiento invariants, restore outcomes, Undo for transfers, shared test
  fakes. Depends on task2.
- [**task4 — Balance read models**](task4/task.md): `SaldoRepository` over a new `Saldo.sq`: balances,
  totals per currency, credit-card estimate. Depends on task2; **parallel with task3**.

## Ownership inside the stage

| File / area | Owner |
| --- | --- |
| `7.sqm`, `databases/8.db`, DDL in `Movimiento.sq`/`Cuenta.sq`/`Transferencia.sq`, the view | task2 |
| Minimal `insertCuenta`/`selectCuentaById`/`insertTransferencia`/`selectTransferenciaById` queries and their row→domain mappers; `ChangeStamp.sq` max over the new tables; default cash seed; `defaultSelection` | task2 (task3 builds repositories on them, task4 uses them for fixtures) |
| Rewrites of existing queries that break when `monto_moneda` leaves the table (`Movimiento.sq`, `Categoria.sq`, `Etiqueta.sq`) | task2 |
| Other mutation/lookup queries in `Movimiento.sq`, `Cuenta.sq`, `Transferencia.sq` | task3 |
| `commonTest/.../core/data/{FakeCuentaRepository,FakeTransferenciaRepository}.kt`; updates to every existing `MovimientoRepository` test fake | task3 (task2 updates the fakes for its own interface changes) |
| `commonTest/.../core/data/FakeSaldoRepository.kt` | task4 |
| `Saldo.sq` (query-only file) and `SaldoRepository` | task4 |
| `MovimientoRepository(Impl)`, `MovimientoMapper` | task2 (plumbing) → task3 |
| `di/RepositoryModule.kt` | task3 adds its bindings first; task4 rebases and appends |
| `core/model/*` new files | task1; later tasks only consume them |

Cross-entity aggregates (balances, totals, card estimate) live in `SaldoRepository`; each entity
repository owns its own rows' reads. Transfer range/year/filter reads for History and Search are not
in this stage: Stage 5/task1 adds them in `ActividadRepository`, read in one snapshot with movimientos.

## Exit gate

- Upgrade from `7.db` preserves every row, assigns `cuenta_efectivo_<moneda>` and `EFECTIVO`, and the
  per-currency balance equals the previous ingresos − gastos (`DatabaseSchemaTest`).
- Fresh install has exactly one live cash account in the primary currency; a restart or a primary
  currency change creates none.
- Repository tests prove every row of the [medio table](../README.md#mediopago-payment-method), the
  account currency/archive/delete rules, transfer validation and transfer delete → Undo with stamps.
- `SaldoRepository` tests prove balances with opening balance and transfers in both directions,
  cross-currency transfers, archived accounts in totals, and the cycle boundaries from task1.
- Existing Home/History/Search/management host tests pass unchanged in behaviour.
- `:shared:testAndroidHostTest`, `:shared:compileKotlinIosSimulatorArm64`, migration verification,
  `detekt`, `ktfmtCheck` and `:androidApp:assembleDebug` pass before review.

## Dedicated review (intermediate, migration and data contract boundary)

The reviewer checks that the currency has exactly one owner (`cuenta.moneda`), that no query reads
tombstoned rows, that every mutation issues a stamp in its own transaction, that invariants live in
repositories and not ViewModels, that money stays exact `Long` and currencies never add up, and that
the default-account IDs are deterministic. Stages 3 and 4 do not start with open findings.
