# Stage 2 — Recurrence UI

Needs Stage 1's `RecurrenciaRepository`. Contract: [README](../README.md).

## Objetivo

Let the user mark a new movimiento as recurring, review and confirm occurrences from Profile, and
notice pending ones from Home.

## Piezas

- **Repetir in entry** — `feature/movimientos`: a switch plus frequency choice (Mensual default) in
  `MovimientoForm` and `MovimientoSummaryStep`, held in the add flow's draft state
  (`AddMovimientoViewModel`, `MovimientoFormState`); Save routes to the repository's create-with-
  movimiento when on. Not shown when editing an existing movimiento.
- **Recurrentes section** — new `feature/recurrentes/{ui,viewmodel}`: `RecurrentesViewModel`
  (`StateFlow<UiState>`, effects `Channel(Channel.BUFFERED).receiveAsFlow()`, `Actions` interface),
  injected `today` source as other screens do, route in `core/ui/navigation/Destinations.kt` reached
  from a Profile row (next to Accounts/Categories/Tags) and from Home.
  - Pending list first, then live recurrences by next date; empty state when none exist.
  - Confirm sheet for a pending occurrence: amount (prefilled), fecha (due date), **Pagado**,
    **Todavía no**, **Saltar**, and Edit (opens the full form with the draft).
  - Recurrence sheet: edit fields; frequency/next date disabled with an explanation while pending
    occurrences exist; delete with confirmation.
  - Pagado/Saltar/delete feedback with Undo through the existing `ManagementFeedbackQueue`
    (`core/ui/feedback`), generalized rather than duplicated.
- **Home row** — `feature/home`: compact row "N por confirmar" while N > 0, opening the section; it
  never changes balance or recent movimientos.
- **Strings** — every locale folder (`values`, `values-es-rAR`, `values-en`, `values-pt-rBR`), copy
  per `docs/wording.md`.
- **Catalog/previews** — add the new components to the UI catalog the way other features do.

## Tests

ViewModel tests for the section (pending ordering, confirm with a changed amount, skip, Undo,
schedule lock), the entry switch (save with and without Repetir), Home pending count; device-test
compilation for any semantics test added.

## Accesibilidad

Touch targets ≥ 48dp, labeled switch and frequency choice, pending rows announce title, amount and
due date, actions reachable with enlarged text.
