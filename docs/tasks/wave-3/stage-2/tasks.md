# Wave 3 / Stage 2 — Reactive History reads

## Objective and tasks

Expose only bounded, currency-filtered date-range observation and persisted years consumed by
History, following the [wave contract](../README.md) after reviewed Stage 1.

- [task1 — Date-range and years](task1/task.md): active; one SQL/repository owner.
- [task2 — Structural Search observation](task2/task.md): deferred; no criteria/SQL/filter work now.
- Execute the [S5.T1 early navigation probe](../stage-5/task1/task.md#early-gate-before-stage-3)
  after Stage 1, by this stage's closure; its fresh-owner/Koin evidence precedes Stage 3 transport.

## Exit gate and review

SQL filters currency/inclusive civil dates before mapping and preserves existing stable order.
Collectors started before writes react to insertion/deletion and clean up on cancellation. Errors are
outcomes, not empty success; zero/negative stored amounts fail mapping while valid signed Money
balances remain supported. The scalar year validator has an explicit limited scope.

Record query plan and selected-month stress observation. No structural Search query, all-history
cache, silent LIMIT, independent period-summary query or unjustified migration is added. Existing
Home/form consumers remain compatible. Review code/checks and the early navigation probe before S3.
Reject `getAll()` + UI filtering, one-shot Flow wrappers, post-LIMIT currency filtering, repository
`stateIn`/scopes and swallowed cancellation.
