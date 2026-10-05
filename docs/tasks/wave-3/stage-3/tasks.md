# Wave 3 / Stage 3 — History projection and screen state

## Objective and order

Turn accepted range/observation contracts into immutable History state with exact totals and explicit
loading/empty/error/retry. Read the [wave contract](../README.md). Requires reviewed Stage 2 and
[S5.T1's early real navigation probe](../stage-5/task1/task.md#early-gate-before-stage-3).

- [task1 — Pure History projection](task1/task.md): one bounded snapshot → rows/count/totals.
- [task2 — ViewModel and retained selection](task2/task.md): after task1; owns range/year observation,
  resume, cancellation, presentation input and supported restoration.
- [task3 — Search state/filter sessions](task3/task.md): deferred; no projection, DI or state work now.

## Exit gate and review

Rows/count/period totals use one snapshot; checked arithmetic overflow is a durable typed failure.
Latest request identity prevents old currency/range emissions appearing under new labels. Selection
survives retry/tab retention/supported restoration; data always reloads from SQLDelight. Explicit
fallback-label updates affect visible row text without a database emission. Global summary remains
separately labelled with independent loading/failure. First re-entry state marks/revalidates stale
cache; upstream reads stop after visibility settling and the agreed five-second timeout.

History has no query/filter/debounce/matching count/normalized index/effects contract. Reject copied
selection state, failure-as-empty, fresh `.value` assumptions on uncollected flows, UI calculations and
single-consumer use-case layers. Current host tests, Native test compilation, Detekt/ktfmt and fresh
cumulative review gate Stage 4.
