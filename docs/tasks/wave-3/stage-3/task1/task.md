# Wave 3 / Stage 3 / Task 1 — Pure History projection

## Objective and context

Produce deterministic display rows/groups and exact period totals from one persisted range snapshot.
Follow the [wave contract](../../README.md); requires reviewed Stage 2 and the early S5.T1 integration
gate. History does not depend on shared search normalization or build searchable fields.

Skills: `android-dev`, `kotlin-api-design`, `kotlin-control-flow`, `android-testing`,
`test-driven-development`; `gradle-run` before wrapper execution.

## Files and responsibility

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryProjection.kt`:
  pure checked totals, row text/category resolution and date grouping.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryDayGroupUi.kt`:
  group aggregate and composed row model, stable ID/fecha/category/display text/exact Money.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryFailure.kt`:
  internal repository versus calculation-overflow failure; reuse existing `DataError` for reads.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryProjectionTest.kt`.

## Produced/consumed contract and approach

Consumes active Moneda, validated MovimientoDateRange, ordered domain movimientos, category snapshot
and explicitly supplied localized unknown-category label. Produces period MovimientoSummary,
HistoryDayGroupUi list and period movement count derived from the same complete input list. No matching
count, query/index/filter arguments or all-history total belongs here.

1. Sum income and expense separately with checked Money arithmetic, then derive balance. Catch only
   expected arithmetic overflow at this pure boundary; no Double, wrapped/partial/zero fallback.
2. Repository guarantees range/currency filtering and positive movement amounts. Assert/test these
   invariants without silently mixing units or changing `created_at` interpretation/order.
3. Description-first title for nonblank descriptions, category label/fallback otherwise. Carry civil
   fecha for date subtitle/header formatting at UI; no Compose resource/date formatting in projection.
   Carry category/avatar metadata so rows remain understandable with no text-search behavior.
4. Group every row by fecha, retaining repository order and stable IDs; date-header keys cannot collide
   with movement IDs. Empty input means an empty selected period, not a globally empty database.
5. A category/presentation-label change rebuilds visible row text without changing monetary results.
   Totals and groups are one revision; no second total query or generic presentation mapper framework.

## Test-first slices

- [x] RED/GREEN zero/income-only/expense-only/mixed/CLP/COP totals and count/group consistency.
- [x] RED/GREEN income/expense subtotal overflow is explicit calculation failure. Valid nonnegative
      subtotals support balances plus/minus Long.MAX_VALUE; do not invent negative movement amounts
      to create an unreachable balance-overflow case.
- [x] RED/GREEN description title, null/blank fallback, missing category, fallback update without changed
      rows, stable equal-date order/header keys/count, typed civil dates and unit/range invariants.
- [x] Verify the projection has no dependency on text normalization or future Search types.
- [x] Run focused host tests, Native test compilation, Detekt and ktfmt before task2.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*HistoryProjectionTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion and dependencies

Same valid snapshot/presentation input yields the same complete projection or typed overflow failure.
Enables task2/Stage 4. If bounded scale needs pagination, re-plan list/totals together; no independent
financial revision or silent result limit.
