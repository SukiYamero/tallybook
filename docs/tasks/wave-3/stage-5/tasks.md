# Wave 3 / Stage 5 — Shared shell and navigation

## Objective

Integrate Home, History and Search as retained primary destinations, with one bottom bar and one
accepted creation flow. Read the [wave contract](../README.md); requires reviewed Stage 4.

## Tasks

- [task1 — Shared navigation and sheet lifetime](task1/task.md).

## Exit gate

- Home/History/Search navigate with bounded entries and correct active semantics; Profile stays inert.
- Input and scroll retention is proven through actual entry lifetime and saveable restoration.
- One owner manages bottom insets and one session owns global movement creation.
- Filter and creation modals cannot overlap or leak edit sessions.
- Typed route serialization, Koin entry owners and saved screen input work together on this KMP stack.

## Dedicated review

Reject app-wide screen ViewModels, duplicate sheet/bar owners, filters lost on tab changes, root Back
producing an empty stack, focus loops and untested assumptions about Android-only navigation APIs.
