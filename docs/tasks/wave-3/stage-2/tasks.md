# Wave 3 / Stage 2 — Reactive consultation reads

## Objective

Add only the database reads that History actually consumes: a bounded, currency-filtered date-range
stream, available years and structural Search predicates. Follow the [wave contract](../README.md).

## Tasks

- [task1 — Date-range and year observation](task1/task.md).
- [task2 — Structural Search observation](task2/task.md), after task1 to avoid shared schema/repository edits.

## Exit gate

- SQL filters currency and inclusive civil-date boundaries before returning rows.
- Active collectors react to writes/deletes with correct range membership and stable order.
- No generated rows escape the repository; cancellation is preserved and failures are outcomes.
- Query plan and the selected-month stress fixture justify the existing/new index choice.
- No schema migration, all-history cache or summary query is added without an actual need.
- Search All history is explicitly measured; exact result counts never conceal a LIMIT.
- Existing Home and creation repository consumers remain compatible.

## Dedicated review

Reject `getAll()` plus UI filtering, a one-shot Flow wrapper, currency filtering after LIMIT,
platform normalization in SQL, eagerly shared repository state, wildcard interpolation and errors
converted to empty success.
