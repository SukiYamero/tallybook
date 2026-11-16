# Recurring movimientos

A gasto or ingreso marked **Repetir** at creation becomes a recurrence; each due date shows up as a
pending occurrence the user confirms (Pagado), postpones (Todavía no) or skips (Saltar). Rules live in
business logic: [Recurrentes](../business-logic.md#recurrentes), with the Home row under
[Home](../business-logic.md#home) and stamps under
[Change stamps and tombstones](../business-logic.md#change-stamps-and-tombstones). Components are in
the [catalog](../ui/components.md).

## Integration

- **Schema 11 (`10.sqm`)** adds `recurrencia` and `recurrencia_etiqueta` and rebuilds `movimiento`
  with a nullable `recurrencia_id`; views and indexes are recreated and the DDL matches a fresh
  install. `ChangeClock` bootstrap includes `recurrencia`.
- `core.model`: `Recurrencia`, `Frecuencia`, `RecurrenciaDraft`, `RecurrenciaWrite` and the pure
  `RecurrenciaRules` (anchor-based occurrences with month-end clamp, pending through an injected
  `today`).
- `core.data`: `RecurrenciaRepository` creates a recurrence with its first movimiento, confirms or
  skips the oldest pending occurrence and undoes either with stamp-based conflict detection.
  `MovimientoWrites` holds the reference checks and row writes shared with `MovimientoRepositoryImpl`.
  Deleting a tag also removes it from recurrences.
- UI: `feature.recurrentes` (screen, occurrence sheet, recurrence sheet), `core.ui.recurrencia`
  (`RepetirField`, `FrecuenciaField`) and `TallybookSwitchRow`. Pagado and Saltar Undo run through the
  shared `ManagementFeedbackQueue`. Strings live in `strings_recurrentes.xml` in every locale.
- Navigation: Recurrentes is a secondary destination from Profile and from Home's pending row.

## Verification boundaries

The final reviewed tree (`5dcc549`) passed 1,315 shared host tests with zero failures, Detekt,
ktfmtCheck, `verifyCommonMainTallybookDatabaseMigration`, Native compilation, Android device-test
compilation and the debug APK after one final independent review. Host tests cover the occurrence
rules over month ends and leap years, back-dated creation, confirm/skip/undo and its conflict through
the real database, and that a pending occurrence moves no balance until Pagado.

On the Xiaomi M2012K11AG, over the existing mock dataset: the 10 → 11 upgrade, Repetir from the
sentence summary, moving the next date into the past to make two occurrences due, Pagado with a
different amount (template updated) and its Undo, Saltar, the Home row, and balance, month gastos and
"Tu mes en números" moving only after Pagado.

Not covered: delete and the schedule lock on device, TalkBack and enlarged text, light theme and
other locales, the instrumented suite, and iOS runtime (see the backlog's iOS item).
