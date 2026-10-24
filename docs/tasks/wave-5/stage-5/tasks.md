# Wave 5 / Stage 5 — History, Search and export integration

## Objective

Bring accounts, payment methods and transfers into the existing read surfaces: History day groups,
Search filters and results, "View movements" from Accounts, and the CSV export.

## Tasks

- [**task1 — History transfer rows and Search account/medio/transfer filters**](task1/task.md):
  `ActividadRepository` one-snapshot reads, History projection, Search filter dimensions, Transfer type,
  transfer results, the currency selector and the Accounts "View movements" wiring.
- [**task2 — CSV export with accounts, medio and transfers**](task2/task.md): new columns and transfer
  rows, from `TransferenciaRepository.getAll()`.

task1 and task2 run in parallel: task2 owns `core/data/export/**` and the Profile export call site;
task1 owns the new `ActividadRepository`, `MovimientoRepository(Impl)`, `Movimiento.sq` filter queries
and `Transferencia.sq` reads, `feature/history/**`,
`feature/search/**`, `core/ui/navigation/PrimaryNavigator.kt` and the "View movements" button in
`feature/accounts/ui`. Strings for both go to `strings_integration.xml`, task1 adding its keys first
and task2 rebasing.

## Dependencies

Starts after Stage 3 (transfer row `TransferenciaRowContent`, Accounts sheet) and Stage 4 (medio icon
and labels, `MedioPagoControl` reused as filter chips) are on `feat/accounts`.

## Exit gate

- History groups include transfers for the selected currency with the right side's amount; period
  totals and counts of ingresos/gastos are unchanged by transfers.
- Search filters by account and medio, type Transfer works, transfers appear only under the README
  conditions, count and rows come from one snapshot.
- Accounts → View movements opens Search filtered by that account.
- The CSV opens in a spreadsheet with the new columns; transfer rows are well formed.
- The 2,000-row scale fixture compiles with transfers added; its execution follows the backlog runner
  limits and is reported truthfully.

## Review

No intermediate review; Stage 6 runs the final review.
