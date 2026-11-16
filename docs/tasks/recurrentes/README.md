# Recurring movimientos

## Contexto

Rent, bills, salary and subscriptions repeat. The user registers one once and, on each due date,
confirms it with the real amount in one step. An unconfirmed occurrence is never money that moved.

## Alcance

- **Repetir** switch with frequency (Mensual by default, Semanal, Anual) in the creation form and in
  the summary sheet of sentence entry. Saving creates the movimiento as today plus a `recurrencia`
  template. Not in the editor of an existing movimiento.
- **Profile → Recurrentes**: pending occurrences first (oldest due date first, across recurrences),
  then every live recurrence by next date. A pending occurrence offers **Pagado** (confirm amount and
  fecha → creates the movimiento), **Todavía no** (closes, stays pending) and **Saltar** (skips that
  occurrence). Pagado and Saltar show Undo through the existing management feedback.
- A recurrence opens a sheet to edit its fields or delete it. Past movimientos never change.
- **Home** shows a compact row "N recurrentes por confirmar" while N > 0, opening the section.

Non-goals: notifications, recurring transfers, pause or end date, making an existing movimiento
recurrent, detecting "cada mes" in sentences, a Search filter by recurrence, exporting templates to CSV.

Anti-goals: a pending occurrence never counts in saldo, Home, History, Search, Statistics or CSV;
confirming late never moves the movimiento into another month.

## Rules

- `recurrencia` holds the movimiento fields (tipo, monto, cuenta, medio, categoría, tags, título,
  descripción), `frecuencia` (`SEMANAL`/`MENSUAL`/`ANUAL`), `fechaAncla`, `proximaFecha`, `createdAt`,
  change stamps and tombstone, under the rules of
  [Change stamps and tombstones](../../business-logic.md#change-stamps-and-tombstones).
  `recurrencia_etiqueta` is its child table. `movimiento.recurrencia_id` (nullable) links a confirmed
  movimiento to its recurrence.
- Occurrence n is `fechaAncla + n` periods, computed from the anchor (no drift): a monthly or annual
  day past the month's end falls on its last day, like `diaCorte` (31 → 30 April → 31 May; 29 Feb →
  28 Feb in non-leap years).
- Creating with Repetir: `fechaAncla` = the movimiento's fecha; `proximaFecha` = the first occurrence
  strictly after max(fecha, today).
- Pending = every occurrence from `proximaFecha` through today (injected `today`, never the clock).
- Pagado and Saltar act on the oldest pending occurrence of that recurrence and advance `proximaFecha`
  by one occurrence, in one transaction and one stamp. Pagado also inserts the movimiento with
  `recurrencia_id`, fecha defaulting to the due date (editable), amount prefilled with the template's
  and editable; a different amount also becomes the template's amount.
- Undo of Pagado/Saltar restores `proximaFecha` and the amount, and tombstones the created
  movimiento. It applies only while neither the recurrence nor that movimiento changed since
  (compare stamps); otherwise the existing conflict feedback.
- Editing frequency or next date is allowed only with no pending occurrence; saving re-anchors
  `fechaAncla` and `proximaFecha` to the chosen next date. Other fields can always be edited and apply
  to pending and future occurrences.
- Deleting tombstones the recurrence; its movimientos keep `recurrencia_id`. Deleting or re-dating a
  confirmed movimiento does not bring its occurrence back.
- An archived or deleted cuenta, or a hidden or deleted category, surfaces when confirming as the
  form's existing field errors (Pagado opens the full form with the draft).

## Stages

- **Stage 1** — data: [stage-1/task.md](stage-1/task.md).
- **Stage 2** (needs Stage 1) — UI: [stage-2/task.md](stage-2/task.md).
- Final independent review, fixes, physical smoke check.

## Verificación

- **Contrato de integración**: host tests that confirm and skip through the real repository and
  database (stamp, movimiento insert, `proximaFecha`, Undo and its conflict), and the pending
  computation over month ends and leap years.
- **Runtime por target**: on the confirmed Xiaomi, create a monthly rent with a back-dated anchor so
  an occurrence is due, confirm it with a different amount, skip another, Undo, and check that balance
  and Statistics move only after Pagado. iOS physical runtime stays under the backlog's iOS item.
