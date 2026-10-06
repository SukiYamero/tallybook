# Stage 1 / Task 2 — Coherent reactive structural reads

Read [Wave 4](../../README.md), canonical business rules and [Task 1](../task1/task.md).
Planning only; coverage A2/A3 and the SQL part of A14. Depends on reviewed/verified criteria shape.

## Goal and ownership

SQLDelight filters dates/type/exact category IDs before loading domain rows. Every emission combines
filtered rows and currency years from one read snapshot so empty-state classification is reliable.

Own existing files under `shared/src/commonMain/`:

- `sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`.
- `kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` and MovimientoRepositoryImpl.kt.
- Proposed `kotlin/.../core/model/MovimientoSearchSnapshot.kt`.
- Proposed `shared/src/androidHostTest/kotlin/.../core/data/SearchRepositoryTest.kt`.
- All repository test fakes affected by the additive interface method; coordinate changes as one
  owner, including Home/History/creation/navigation fixtures. Do not add fake-success default methods.

## Contracts and query strategy

Consumes `MovimientoReadCriteria`. Produces public `MovimientoSearchSnapshot(movimientos:
List<Movimiento>, persistedYears: List<Int>)`, with derived `hasCurrencyData = persistedYears.isNotEmpty()`
and no separately stored count/balance. Years use the existing currency metadata query/parser; they
cover the currency's entire history, not just filtered rows.

Add `observeFiltered(criteria: MovimientoReadCriteria): Flow<DataOutcome<MovimientoSearchSnapshot>>`
to MovimientoRepository. It is cold, observable and dispatched through injected IO. No text/query
normalization, localized category label or framework type enters this boundary.

Use two named structural queries: without category predicate and with a bound IN collection. Both
use nullable bound date/type dimensions and the existing full stable order. This avoids empty-IN
ambiguity and combinatorial queries. All uses absent date bounds, not a fake beginning/end or LIMIT.
Sort/copy bound IDs deterministically. Verify generated parameter nullability/types before coding the
consumer; no interpolated SQL, comma-joined IDs or generic SQL builder.

Observe the generated structural query asFlow. For each invalidation, use withContext(injected IO)
and the database's transactionWithResult to read structural rows and selectMovimientoYears for the
same currency. Fetch both before leaving the synchronous transaction; map immutable raw rows outside
that block with cancellation checkpoints. Do not suspend inside a driver transaction. The query's
table listener must invalidate even when a write does not match its predicates: that write can change
currency years/presence. Prove this generated dependency rather than assuming it.

If generated structural query observation omits required invalidations, use an observable existing
movimiento-table metadata query as the single trigger and read both statements transactionally on
each emission. Do not combine independent result/metadata Flows or add another full-history read.
An integration probe decides this trigger choice before Stage 2 depends on it.

Reuse mapper, year parsing, asLocalDataOutcome and cancellation behavior. Invalid IDs/ranges return
InvalidInput without listeners. Keep observeRange's four-digit cross-year SQLite-text constraint;
All retains its absent range. Malformed rows/metadata/read failures become Local, not empty success.
Native synchronous driver calls can complete after cancellation; map/publication guards must reject
their outdated result. Do not claim cancellation interrupts a SQLite call.

## Meaningful fixtures and boundary proof

- [ ] Create small real SQLite fixtures with two currencies, both types, parent and child categories,
      missing IDs, inclusive same-day/cross-year dates and equal dates with differing createdAt/IDs.
- [ ] Cover null date/type, one range, one/many/no IDs, missing-ID zero results, AND predicates and OR
      IDs. Selecting a parent excludes its child; unrelated type/category selection is a valid empty.
- [ ] Collect before inserting/deleting a matching row; assert snapshot/list order updates and years
      coherence. An excluded row in a new year still updates years while matching rows stay unchanged.
- [ ] Delete last currency row, and last matching row while another nonmatching currency row remains;
      assert true no-data versus no-results metadata from the emitted transaction.
- [ ] Exercise interleaved writes around collection with real transactions; no emitted snapshot may
      have nonempty rows and empty currency years. Validate all returned row years are represented.
- [ ] Cover collection cleanup on cancel, fresh resubscription, failure and mapping cancellation.
      Parameter fixtures include quote/percent/underscore IDs; they remain bound literal IDs.
- [ ] Reuse four-digit cross-year invalidity regression; do not weaken existing History assertions.
- [ ] Inspect generated query signatures/table dependencies and EXPLAIN QUERY PLAN for All, This year,
      sparse IDs and mixed currency. Retain the existing index unless evidence justifies another.
      Query-only work needs no schema migration. Any index/schema change adds migration verification
      and a material review checkpoint before downstream state work.

## Verification / exit

Primary SQLDelight references and version gate are in the overview. Confirm resolved 2.3.2 artifacts
and generated code, then run through gradle-run:

`./gradlew :shared:testAndroidHostTest --tests '*SearchRepositoryTest' --tests '*HistoryRepositoryTest'`

`./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`

Broader host regression is warranted once here because the public repository interface/fakes change.
Discover migration verification tasks only if schema changes; never edit generated database/query files.
Native compilation alone does not prove Native transaction/listener behavior; Stage 3 acceptance
requires target evidence. Exit requires coherent reactive snapshots and passing real SQL tests. If
the transaction/driver boundary fails, stop dependent state work and resolve it or re-plan explicitly.
