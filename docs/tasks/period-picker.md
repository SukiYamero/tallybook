# Period picker

## Goal

Turn the catalog period picker into a reusable date-selection component for the future history
screen. It selects a day, calendar week, or month; it does not calculate balances or movement
counts.

## Approved behavior

- The component receives `today`; it never reads the system clock itself.
- The caller supplies the allowed year list. The year menu shows four rows before scrolling.
- The active year and month define the visible context. Selecting a past year preserves the
  month. Selecting the current year clamps a future month to the current month.
- Day and week options are limited to `today` only in the current month. Historical months are
  complete.
- Selecting a month makes it the context used by the day and week scopes.
- A contextual title and optional subtitle render to the left of the year menu.
- The three scope options remain neutral. The active period chip retains the brand selection
  color.

## Ownership

The future history ViewModel owns `today`, allowed years, the selected typed period, and movement
data. The component remains stateless and renders values plus callbacks. The catalog captures the
device date once and injects it into the pure range builders; tests use fixed dates.

## Validation environment

Review the picker on one confirmed physical device: Xiaomi phone, Android tablet, or iPhone. Do not use
an emulator or simulator. Before installation, instrumented testing, or visual review, resolve the
connected device and the machine's required toolchain paths. If no supported physical device is
connected, stop that verification step and request one from the user.

## Non-goals

- Calculating or displaying balance, income, expense, or movement counts.
- Loading years or movements from persistence or the network.
