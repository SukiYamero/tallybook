# Wave 3 / Stage 2 / Task 1 — Date-range and year observation

## Objective and context

Expose SQLDelight-backed observation for History, following the [wave contract](../../README.md).
Requires Stage 1 review. Read current repository, mapper, SQL schema, error wrappers and host tests.

Skills: `android-dev`, `android-data-layer`, `kotlin-flows`, `kotlin-coroutines`, `android-testing`,
`test-driven-development`, `gradle-run`.

## Files and responsibility

- Modify `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`.
- Add focused positive-amount validation to the existing `MovimientoMapper.kt` read boundary; signed
  `Money` remains valid for balances. Zero/negative persisted movement amounts produce `DataError.Local`
  through the existing repository wrapper, with malformed-row regressions.
- Extend `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` and
  its `MovimientoRepositoryImpl.kt`, reusing `MovimientoMapper.kt` and `FlowDataOperation.kt`.
- Create `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/HistoryRepositoryTest.kt`.
- Update repository fakes/contract implementations in existing Home/form tests for new methods;
  do not replace meaningful assertions with defaults to make compilation pass.

## Consumed and produced contracts

- Consumes validated `MovimientoDateRange`, `Moneda`, existing injected database dispatcher and
  `DataOutcome`/`DataError`.
- Produces `observeRange(moneda: Moneda, range: MovimientoDateRange): Flow<DataOutcome<List<Movimiento>>>`.
- Produces `observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>>`, distinct persisted years only,
  descending. The ViewModel adds today/current selection; the repository never reads the clock.
- Existing `observeRecent`, `observeSummary`, insert/getAll/delete remain compatible.

## Specific approach

1. Range query selects `monto_moneda = ?` and `fecha >= ? AND fecha <= ?`; preserves the existing
   descending date/createdAt/id order. Query-only additions need no schema migration.
2. Year query reads distinct years for the active currency from canonical stored civil dates. Do not
   load every movement to obtain a short menu. Reject invalid/out-of-range returned year metadata
   through a focused metadata validator/error boundary instead of manufacturing year zero or a
   silently empty list. A year projection is not a full movement mapper: document its validation scope
   and cover malformed year representations; complete stored-date validation remains the row mapper's
   responsibility.
3. Use SQLDelight observable Query support and the existing dispatcher/error wrappers. Guard query
   construction as well as collection where it can throw. A failed stream can be resubscribed by retry.
4. Preserve cancellation, including during large reads. Close test drivers and collectors. No
   repository `stateIn`, extra CoroutineScope or platform exception in public contracts.
5. Inspect `EXPLAIN QUERY PLAN` with realistic date/currency distribution. The existing date-first
   index may suffice; add a currency/date/order index only if the plan demonstrates a benefit. An
   index change is a schema change: generate schema and migration with SQLDelight and verify them.
6. Respect the existing `created_at` text order. Do not claim it is a normalized chronological
   instant comparison for every fractional representation; a domain-wide ordering change requires a
   separate decision/migration and is not silently folded into History.

## Test-first slices

- [ ] RED: inclusive first/last dates, adjacent excluded dates, one-day and empty ranges, mixed
      currencies and stable existing tie order. GREEN: implement the bounded range query and mapping.
- [ ] RED: empty years, duplicates, multiple years/currencies and a write introducing a new year.
      GREEN: implement year observation.
- [ ] Begin collectors before insert/delete; verify second emissions and cancellation of subscriptions.
      Calling `first()` only after a write is insufficient evidence of query invalidation.
- [ ] Force collection/storage failures, invalid mapper data and cancellation; assert failure versus
      cancellation separately. Test a construction-time exception if the adapter permits injection.
- [ ] Exercise a 10,000-row selected-month fixture plus many excluded-date/currency rows; record query
      plan and processing observation without using wall-clock timing assertions in unit tests.
- [ ] Run focused repository tests, full host regression, target compilation and lint/format checks.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*HistoryRepositoryTest' --tests '*MovimientoRepositoryTest'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`.
If schema changes, first discover the exact generated SQLDelight tasks and run migration verification.

## Completion / blockers

Complete when real SQL streams meet the bounded read contract. Blocks Stage 3. If measured volume
requires pagination, stop the affected design and revise list/summary/search contracts together;
do not introduce arbitrary LIMIT truncation or partial financial totals.
