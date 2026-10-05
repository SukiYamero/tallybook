# Wave 3 / Stage 3 / Task 1 — Pure History projection

## Objective and context

Build one deterministic display projection from a persisted range snapshot, following the
[wave contract](../../README.md). Requires reviewed Stage 2 and shared normalization.

Skills: `android-dev`, `kotlin-api-design`, `kotlin-control-flow`, `android-testing`,
`test-driven-development`; `gradle-run` before commands.

## Files and responsibility

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryProjection.kt`:
  pure totals, searchable row indexing, query filtering and date grouping.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryDayGroupUi.kt`:
  group aggregate plus its composed movement-row presentation type, with stable IDs and exact Money.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryFailure.kt`:
  internal typed failure distinguishing repository reads from calculation overflow.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryProjectionTest.kt`.

## Consumed and produced contracts

- Consumes active `Moneda`, `MovimientoDateRange`, ordered domain movimientos, category snapshot,
  settled query and a supplied localized unknown-category label.
- Produces one projection: period `MovimientoSummary`, filtered `HistoryDayGroupUi` values,
  period movement count and matching result count. Counts derive from lists, not editable constructor
  flags; no all-history or cross-currency total is inferred here.
- Produces explicit `HistoryFailure.CalculationOverflow` if checked aggregation cannot represent a
  total. Storage errors remain repository-owned `DataError`; no new exception taxonomy is fabricated.
- Row text/category resolution is shared with rendering: no discrepancy between the searchable label
  and what the user sees, and no Compose resource lookup in this pure projection.

## Specific approach

1. Build searchable candidates when range/category data changes. Store immutable normalized fields
   derived from that snapshot; apply query matches without re-reading SQL or modifying domain models.
2. Sum incomes and expenses separately using checked `Money` arithmetic on the same input list, then
   derive balance. Catch only the expected arithmetic overflow at this pure calculation boundary;
   do not broadly swallow programming errors or introduce `Double`/rounding.
3. The repository guarantees date/currency filtering. Assert/test that contract rather than quietly
   accepting mixed units. Do not re-sort by a different createdAt interpretation in Kotlin.
4. Filter descriptions and displayed category names literally. A null/blank description contributes
   no text. Unknown categories use the supplied displayed fallback and remain searchable by it.
5. Group matched rows by `fecha`, retaining repository order and stable movement IDs. A separate
   header key includes the civil date; row and header keys must not collide.
6. Represent date headers using typed civil dates. Localized presentation formatting belongs at the
   UI boundary, uses the established app resource conventions and is injected where pure tests need it.
7. The period summary is independent of query. A projection that has no matching rows but has period
   movements is “no results”, not an empty period or zero balance.
8. Prefer one focused projection helper plus composed models. No catch-all `HistoryModels.kt`, generic
   search engine or domain-to-presentation mapper framework.

## Test-first slices

- [ ] RED: zero, income-only, expense-only, combined and CLP/COP totals are exact; query leaves them
      unchanged. GREEN: implement checked period calculation from one snapshot.
- [ ] RED: income/expense subtotal overflow produces explicit failure, never zero/partial results.
      GREEN: add the narrow arithmetic boundary. For valid nonnegative subtotals, test extreme
      balances at plus/minus Long.MAX_VALUE; their subtraction remains representable, so do not
      invent an unreachable balance-overflow fixture with invalid negative movement amounts.
- [ ] RED: category/description matches, null description, unknown category, unsupported fields,
      literal punctuation and no results. GREEN: compose the shared normalizer/matcher.
- [ ] RED: date grouping and equal-date stable row order, keys and counts. GREEN: produce UI aggregates.
- [ ] Check that changing only category labels updates display/search without changing monetary totals.
- [ ] Run focused host tests, Native test compilation and lint/format checks before task2 starts.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*HistoryProjectionTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when the same valid snapshot yields the same projection or the explicit overflow failure.
Blocks task2 and Stage 4 UI. A need for paginated totals or cross-currency display requires re-planning,
not a second independent total that can disagree with the rows.
