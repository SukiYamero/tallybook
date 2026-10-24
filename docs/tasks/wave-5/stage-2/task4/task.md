# Wave 5 / Stage 2 / Task 4 — Balance read models

## Objective

Expose the derived money views of accounts — balance per account, total per currency and the
credit-card estimate — as reactive SQL aggregates behind one `SaldoRepository`. Nothing is stored:
every value is recomputed from live rows by SQLite `SUM()` in exact minor units.

## Context and skills

- **Blocked by task2** (v8 tables) and task1 (`CuentaSaldo`, `CreditEstimate`, `creditCycle`).
  **Parallel with task3**: do not edit `Movimiento.sq`, `Cuenta.sq`, `Transferencia.sq` or the entity
  repositories. Put every query in a new query-only `Saldo.sq` (SQLDelight generates `SaldoQueries`).
  The rewrites of existing aggregates that read `movimiento` directly were done in task2. Build test
  fixtures with task2's `insertCuenta`/`insertTransferencia` queries and the existing
  `insertMovimiento`, and map rows with task2's `CuentaMapper`; never depend on task3's repositories.
- Read the [wave README](../../README.md#balances-and-credit-card-estimate), `MovimientoRepositoryImpl`
  (`observeSummary`, `asFlow().mapToList(dispatcher)`, `asLocalDataOutcome()`), `MovimientoSummary`,
  `FlowDataOperation.kt`, and the comments in `Movimiento.sq` on why sums stay in SQL.
- Skills: `android-dev`, `android-data-layer`, `kotlin-flows`, `android-testing`,
  `test-driven-development`.

## Files

- Create: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Saldo.sq`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/SaldoRepository.kt` + `SaldoRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/RepositoryModule.kt` (append the
  binding after task3's, rebasing on it)
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/SaldoRepositoryTest.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/FakeSaldoRepository.kt`

## Contracts

```kotlin
interface SaldoRepository {
  /** Every live account, archived included, with its balance. */
  fun observeSaldos(): Flow<DataOutcome<List<CuentaSaldo>>>

  /** One total per currency that has at least one live account. */
  fun observeTotales(): Flow<DataOutcome<Map<Moneda, Money>>>

  /** Live credit-card accounts; the cycle comes from creditCycle(today, diaCorte). */
  fun observeCreditEstimates(today: LocalDate): Flow<DataOutcome<Map<String, CreditEstimate>>>
}
```

- `today` is a parameter captured once by the caller (the clock/time zone owner), never read inside
  the repository; a new `today` is a new subscription.
- Queries return per-account integer components (opening balance, ingresos, gastos, transfers in,
  transfers out; or cycle gastos/ingresos between two dates). Kotlin only combines them with `Money`'s
  exact operators; currencies are never mixed. A multi-row snapshot (accounts + components) comes
  from one statement or one transaction so balances never mix two database states.

## Exact rules

- Balance, total, `deuda` and cycle spending formulas: README
  [Balances and credit-card estimate](../../README.md#balances-and-credit-card-estimate). Live rows
  only; transfers contribute `montoDestino` to the destination and `montoOrigen` to the origin.
- Archived accounts count in balances and totals; tombstoned accounts, movimientos and transfers never.
- The cycle is computed by task1's pure `creditCycle` and passed to SQL as inclusive civil dates; the
  credit estimate never reimplements date math.
- Index use: confirm with `EXPLAIN QUERY PLAN` that per-account aggregates use the
  `movimiento(cuenta_id)` and transfer account indexes from task2 rather than scanning per account;
  record the plan assertion in the test like the existing History/Search plan tests.

## TDD

- [ ] RED: balance = opening + ingresos − gastos for one cash account; then add incoming and outgoing
      transfers, a cross-currency transfer (each side in its own currency), a tombstoned movimiento and
      a tombstoned transfer (both excluded).
- [ ] RED: totals per currency with an archived account included and a tombstoned account excluded;
      a currency without accounts is absent.
- [ ] RED: credit card with opening debt, purchases inside and outside the cycle, a refund (ingreso),
      a payment transfer, `diaCorte` null versus 15; `deuda` is zero when the balance is positive.
- [ ] RED: reactivity — inserting a movimiento or transfer re-emits balances once per committed write.
- [ ] GREEN: `Saldo.sq` queries and `SaldoRepositoryImpl`; plan assertion.
- [ ] Bind in `RepositoryModule`; Koin graph test still resolves.
- [ ] `./gradlew :shared:testAndroidHostTest --tests '*SaldoRepository*'`, full host suite once before
      review, `:shared:compileKotlinIosSimulatorArm64`, `detekt ktfmtCheck`.

## Blocks

Blocked by task1 and task2. Blocks Stage 3 (Home total, Accounts screen, "Pay card" amount).
