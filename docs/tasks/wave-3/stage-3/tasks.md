# Wave 3 / Stage 3 — Consultation projections and screen state

## Objective

Turn the accepted range and reactive data contracts into immutable History state, with explicit
loading/error/search semantics. Read the [wave contract](../README.md).

## Tasks and order

- [task1 — Pure History projection](task1/task.md): exact totals, searchable presentation and groups.
- [task2 — ViewModel and retained input](task2/task.md): consumes task1; owns selection, observation,
  debounce, retry and saved-input validation. Execute after task1 is verified.
- [task3 — Search state and filter sessions](task3/task.md): follows task1/shared normalization and
  Stage 2; may run beside task2 with separate feature files. Coordinate the shared Koin module edit.

## Exit gate

- Period totals and rows refer to one range snapshot; changing search never changes those totals.
- Arithmetic overflow is a typed durable failure, never a crash, wrapped total or false zero.
- Late emissions from an old range/currency/query cannot replace the active selection.
- Filters/query survive failures and supported restoration; rows always reload from SQLDelight.
- Apply commits a valid draft once; dismiss discards; header count refers to settled applied results.
- No display calculations or database reads live in Compose callbacks.
- Host tests, Native test compilation, Detekt and ktfmt pass with current evidence.

## Dedicated review

Reject simultaneous editable copies of selection, invisible debounce races, failure-as-empty states,
per-keystroke SQL reads, synchronous `.value` assumptions for uncollected derived flows and a use-case
layer with only this single consumer.
