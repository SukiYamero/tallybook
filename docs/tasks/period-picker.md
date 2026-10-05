# Period picker

## Goal

Turn the catalog period picker into a reusable date-selection component for History.
It selects a day, month-bound week block, or month; it does not calculate balances or movement
counts.

## Approved behavior

- The component receives `today`; it never reads the system clock itself.
- The caller supplies the allowed year list. The year menu shows four rows before scrolling.
- The active year and month define the visible context. Selecting a past year preserves the
  month. Selecting the current year clamps a future month to the current month.
- Day and week options are limited to `today` only in the current month. Historical months are
  complete.
- Week blocks are 1–7, 8–14, 15–21, 22–28 and 29–month end, never crossing months. Month chips
  represent the full calendar month. History read-range and anchor rules live in
  [business logic](../business-logic.md#tracking-and-history).
- Selecting a month makes it the context used by the day and week scopes.
- A contextual title and optional subtitle render to the left of the year menu.
- The three scope options remain neutral. The active period chip retains the brand selection
  color.

## Ownership

The History ViewModel owns scope and anchor, derives the selected typed period and receives `today`,
allowed years and movement data from their owners. The component remains stateless and renders
values plus callbacks. Shared framework-free range calculation lives in
`core.model.MovimientoPeriodRanges`; core UI adapts those ranges without importing History.
The catalog captures the device date once and injects it into the pure range builders; tests use
fixed dates.

## Validation environment

Review the picker on one confirmed physical device: Xiaomi phone, Android tablet, or iPhone. Do not use
an emulator or simulator. Before installation, instrumented testing, or visual review, resolve the
connected device and the machine's required toolchain paths. If no supported physical device is
connected, stop that verification step and request one from the user.

## Non-goals

- Calculating or displaying balance, income, expense, or movement counts.
- Loading years or movements from persistence or the network.
