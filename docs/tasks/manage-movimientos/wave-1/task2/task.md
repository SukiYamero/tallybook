# Wave 1 / Task 2 — safe movimiento mutations

## Objective and ownership

Expose read-by-ID, identity-safe editing, atomic delete-and-capture and exact restoration through the
existing SQLDelight repository. Read the [feature contract](../../README.md). This task is independent
of snackbar/UI. Use `android-dev`, `android-data-layer`, `kotlin-api-design`, `android-testing` and
the repository test-first workflow; SQLDelight remains the selected persistence stack.

Own these files under `shared/src/`:

- Modify `commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` and
  `MovimientoRepositoryImpl.kt`.
- Create `commonMain/kotlin/com/kurobello/tallybook/core/model/MovimientoChanges.kt` and
  `MovimientoMutationResult.kt` (update and restore result types share this mutation responsibility).
- Modify `commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq` for update only;
  reuse select-by-ID, insert and delete queries.
- Extend `androidHostTest/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryTest.kt` and
  the existing `ReactiveRepositoryTest.kt`, `HistoryRepositoryTest.kt`, `SearchRepositoryTest.kt`
  only where needed to verify real observation invalidation.
- Update existing repository fakes in tests for new methods with explicit behavior. Discover all
  implementations first; do not insert throwing fake defaults that hide changed behavior.

No theme/components/catalog/nav/DI/production copy edits. You are not alone: never revert others'
changes. Orchestrator owns shared Gradle runs/index/commits. No schema field/index change is needed;
do not modify generated sources, `.db` snapshots or add a migration for a new query alone.

## Produced contracts

```kotlin
data class MovimientoChanges(
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaId: String,
    val descripcion: String?,
)
enum class MovimientoUpdateResult { Updated, NotFound }
enum class MovimientoRestoreResult { Restored, AlreadyPresent, Conflict }

suspend fun getById(id: String): DataOutcome<Movimiento?>
suspend fun update(id: String, changes: MovimientoChanges): DataOutcome<MovimientoUpdateResult>
suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?>
suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<MovimientoRestoreResult>
```

These are proposed additions to `MovimientoRepository`; existing consumers remain compatible after
their concrete implementations/fakes implement the new methods. Preserve `insert`, reads and
`delete(id): DataOutcome<Unit>` semantics, including successful deletion of an absent/blank ID and
deletion of a stored row whose other fields cannot be mapped. Keep its raw SQL/idempotent path;
share only a lower-level deletion helper if useful. Do not delegate it through strict read/capture
validation. New capture may fail safely on an unreadable row. Never load every row to locate an ID.

## Exact persistence behavior

- `getById` uses the existing ID query. Success(null) means absent; mapping/storage failure remains
  Failure. Blank ID is InvalidInput, not a wildcard. Parse through the existing domain mapper.
- `update` validates positive amount, nonblank ID/category and description length using the existing
  validation policy. UI owns current-date/catalog interaction rules; repository does not read a clock
  or primary-currency preferences. Missing legacy category can remain if unchanged by the caller.
- In one dispatcher-bound transaction, read current row; return NotFound if absent. Reject a currency
  different from its stored currency as InvalidInput. UPDATE only tipo, monto_minor, fecha,
  categoria_id and descripcion WHERE id. Never write id/created_at/monto_moneda, upsert, delete/insert
  or allocate identity. Assert the actual affected-row contract; successful update returns Updated.
- `deleteAndCapture` reads and deletes the current row inside the same transaction, returning that
  exact immutable row only after successful commit. Absent returns Success(null). A failure rolls
  back both steps and never yields an Undo snapshot. Two delete calls cannot both capture the row.
- `restoreDeleted` validates the captured record and checks ID in one transaction. If absent, insert
  it exactly, including metadata. If present and domain-equal, return AlreadyPresent with no write;
  if different, return Conflict with no write. An insertion/commit failure is Failure. No replace
  conflict clause, new ID, new creation instant or overwrite is allowed.
- Use the existing injected dispatcher/error boundary. Expected results stay typed; platform
  exceptions never reach the UI. Rethrow cancellation. A completed commit is not undone by a later
  coroutine cancellation; never claim otherwise. Lifecycle/result delivery is Wave 2's owner.
- Current writes are local and serialized by the management owner. No optimistic version column,
  HLC, outbox or future-sync abstraction is introduced. Revisit this boundary when sync is selected.

## Test-first slices and meaningful inputs

- [ ] Real SQLite read-by-ID round trip, missing versus failure, and blank-ID rejection.
- [ ] Update every editable field, preserving exact ID/createdAt/currency and total row count. Verify
      fecha changes reorder a tied list correctly and a missing update never inserts anything.
- [ ] Invalid amount/description/currency writes preserve the original row; use valid COP/USD/CLP
      fixtures and Money integer limits. Do not invent an invalid domain type just to fill coverage.
- [ ] Atomic capture returns the latest stored row, not an earlier UI fixture; repeated deletion
      yields null. A real closed/unavailable driver maps storage failure, without a false capture.
- [ ] Delete→restore yields complete domain equality and the original stable sort position. Same-ID
      equal/different fixtures produce AlreadyPresent/Conflict without overwrite or duplication.
- [ ] Existing old delete API remains idempotent for absent/blank IDs and can delete a raw unreadable
      legacy fixture; its original regression checks remain green. New capture does not return a
      malformed snapshot or remove a record when domain mapping fails.
- [ ] Observe existing flows before mutation: Home summary/recent, History date range/year metadata,
      Search structural rows/currency years. Assert updated emissions after update/delete/restore;
      lists must not depend on manual invalidation. Use a few rows at predicate/six-row boundaries.
- [ ] Close drivers/scopes in finally; use one scheduler and in-memory fixtures, never production DB.
- [ ] Run selected host classes, affected build and mandatory style through the orchestrator.

Exit: contracts compile for existing consumers/fakes; transaction and observation tests pass;
Native source compilation remains a separate target gate. Future commit:
`feat(movimientos): add safe update and undo persistence`.

## Execution evidence

- Public mutation types preserve the existing public repository interface. New strict mutation
  validation is separate from the legacy raw/idempotent delete path.
- An unreadable same-ID row is a terminal restoration conflict; the narrow mapper-only
  `IllegalArgumentException` handler never catches query/storage failures. The mapper uses
  [LocalDate parsing at 0.8.0](https://raw.githubusercontent.com/Kotlin/kotlinx-datetime/v0.8.0/core/common/src/LocalDate.kt)
  and [Instant parsing](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-instant/-companion/parse.html),
  whose documented malformed-input failures share that exception family.
- Resolved SQLDelight runtime is **2.3.2** on Android (`runtime` / `runtime-jvm`) and iOS Arm64
  (`runtime`), confirmed by the orchestrator's targeted dependency resolution.
- [SQLDelight transaction contract](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/transactions/)
  and [2.3.2 transaction implementation](https://raw.githubusercontent.com/sqldelight/sqldelight/2.3.2/runtime/src/commonMain/kotlin/app/cash/sqldelight/Transacter.kt)
  confirm synchronous transaction result delivery after completion and commit-scoped invalidation.
  [2.3.2 JDBC driver](https://raw.githubusercontent.com/sqldelight/sqldelight/2.3.2/drivers/jdbc-driver/src/main/kotlin/app/cash/sqldelight/driver/jdbc/JdbcDriver.kt)
  returns the actual affected-row count for writes; insert/update/capture require exactly one row.
- RED: the orchestrator ran `MovimientoRepositoryTest` and `ReactiveRepositoryTest` against minimal
  API stubs. Both classes compiled and executed; ten new-behavior assertions failed as expected,
  while the original repository behavior checks passed.
- GREEN: the orchestrator reran both selected host classes on the implementation: repository
  **14/14** and reactive **10/10**, with zero failures, errors or skips.
- Formatter was applied by the orchestrator. Mandatory style, Android build, device-fixture
  compilation and iOS Arm64 production/test compilation remain pending. Real SQLite abort triggers
  verify write-failure rollback. Commit-failure injection and physical driver runtime behavior
  remain unverified by these host tests.

- Structural Detekt exception: `TooManyFunctions` is scoped only to `MovimientoRepository`
  (**13** methods) and its implementation (**17** methods), against the default **11** threshold.
  The existing compatible typed contract and one storage/error boundary remain cohesive; splitting
  interfaces or moving boundary-owned helpers only to change the count adds complexity without a
  caller need. The orchestrator adjudicated this bounded exception; final independent review must
  reassess cohesion. No lint config or other warning suppression changes are included.
