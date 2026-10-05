# Wave 3 / Stage 2 / Task 2 — Structural Search observation

## Objective and context

Expose reactive Search reads for the confirmed [wave contract](../../README.md), including All history
without fake bounds or silent result caps. Requires reviewed Stage 1 and task1 repository work.

Skills: `android-dev`, `android-data-layer`, `kotlin-flows`, `kotlin-coroutines`, `android-testing`,
`test-driven-development`, `gradle-run`.

## Files and contracts

- Modify `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`.
- Extend `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` and
  `MovimientoRepositoryImpl.kt`; reuse mapper/error/dispatcher boundaries from task1.
- Create `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/SearchRepositoryTest.kt`;
  update existing contract fakes so real consumers still compile.

Produces `observeFiltered(criteria: MovimientoReadCriteria): Flow<DataOutcome<List<Movimiento>>>`.
Criteria contains currency, optional validated civil-date range, optional type and category ID set;
it intentionally contains no text query. Search consumes existing `observeYears(currency)` for honest
“no movements in this currency” versus “no matching results” status without another full-history read.

## Specific approach

1. Apply structural predicates in SQL with bound parameters, not interpolated IDs/text. Type comes
   from the movimiento, category filter matches exact IDs, empty IDs removes that predicate.
2. Verify SQLDelight 2.3.2 variable-arity category bindings and generated-query dependency tracking
   against its actual artifacts. Use the smallest typed query arrangement that handles optional
   dimensions; do not create a generic SQL builder or one query per combinatorial filter permutation.
3. Preserve existing date/createdAt/id order and cold observable behavior. Invalid input returns
   InvalidInput; read/map failures return Local, cancellation propagates and listeners clean up.
4. All history has no date predicate. The returned list is the complete structural result for the
   selected currency; text matching/count derives from it. No `getAll()` cache and no arbitrary cap.
5. Reuse current index unless real query plans justify a new one. Any new index requires generated
   schema/migration checks; query-only changes do not. Include mixed currencies, many category IDs
   and sparse date filters in the plan inspection, not only a tiny fixture.
6. Profile All history and This year separately from bounded History. A 10,000-row fixture is a
   repeatable stress input, not a timing assertion or promised maximum. If inadequate, stop and
   re-plan exact count/accent-normalized search/paging together instead of publishing partial results.

## Test-first slices

- [ ] RED/GREEN All/one-day/custom/year date reads and exclusion of foreign currency at SQL boundary.
- [ ] RED/GREEN all types versus gasto/ingreso; empty/single/multiple category IDs, exact parent versus
      child and missing IDs; dimensions compose AND, IDs compose OR.
- [ ] Collect before insert/delete; assert entry/exit from each predicate and listener cancellation.
- [ ] Test failures, malformed persisted rows and query parameter safety without exposing user values.
- [ ] Record query-plan and stress observations; no brittle wall-clock performance assertion.
- [ ] Run focused repository tests, full host regression, Native compile and lint/format checks.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*SearchRepositoryTest' --tests '*HistoryRepositoryTest'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when reactive SQL meets the full structural contract. Blocks Search state/UI. Scaling that
requires a migration/pager changes this execution contract and must be resolved before dependent work.
