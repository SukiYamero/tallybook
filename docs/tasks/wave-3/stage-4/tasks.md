# Wave 3 / Stage 4 — History and Search composition

## Objective

Render pure History and Search state with Tallybook controls and the user-supplied visual guide.
Read the [wave contract](../README.md) and canonical UI architecture. Shell integration is Stage 5.

## Tasks and order

- [task1 — Pure screen and row migration](task1/task.md): screen states, resources, targeted shared-row
  token migration, previews and semantics tests.
- [task2 — Search screen and transactional filter sheet](task2/task.md): reference-based search
  composition, active chips and Aplicar without quantity. Requires task1/shared row verified.

## Exit gate

- Every visible state and new string works in supported locales, themes and enlarged text.
- Filter selections edit draft only; Apply/clear/dismiss callbacks implement their distinct meanings.
- Search row follows the reference while Home's presentation remains compatible.
- Existing creation validation, keyboard, category entry and discard protection remain accepted.
- Both screens remain pure/previewable; only Stage 5 routes own wiring/navigation.

## Dedicated review

Reject cloned movement rows, anonymous visual literals, duplicate bars/sheets, app-wide History state,
live filter writes disguised as Apply and click semantics for unavailable movement detail.
Final visual acceptance remains a physical-device gate in Stage 6.
