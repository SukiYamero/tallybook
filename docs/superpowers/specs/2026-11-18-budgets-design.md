# Budgets (Presupuestos) — design

Status: draft for approval. Backlog item: *Accounts follow-ups → Budgets* (first in the work order).

## Intent

Statistics tells the user where the money went; nothing helps them keep a monthly amount in mind
while they spend. A budget is a monthly amount the user assigns to a category. The goal is
**awareness, not restriction**: the app never blocks or discourages a save. It tells the user, at
the moment they record a gasto and whenever they look, that they are close to the amount, past it,
or spending fast enough to pass it before the month ends.

## Goals

- A **Presupuestos** screen in Profile, next to Categories and Tags, to create, see, edit and delete
  budgets.
- A budget on **any category except Otros**: predefined roots, predefined children and custom
  categories.
- One in-app notice after saving a gasto that touches a budget in a non-fine state, showing only the
  most relevant one.
- A compact Home row while any budget needs a look.

## Non-goals (this version)

- System notifications. Every gasto enters through the app, so every change that worsens a budget
  happens in front of the user; time passing without spending only improves the pace.
- A global or per-tag/per-account budget, weekly or annual periods, rollover of unspent amounts.
- FX: a budget counts only gastos in its own currency.
- A Statistics section, a budget line in Statistics' category sheet, or a month picker for past
  months (the sheet's six-month history covers it).
- CSV export of budgets (the export is movement data).

## Anti-goals

- Blocking or delaying a save, or asking to confirm it because of a budget.
- Alarming copy: no "alerta", exclamation marks, warning icons or red-heavy screens
  (see [wording](../../wording.md) §2 and §5).
- Stacking several budget notices for one save.
- A notice on Undo/restore, on transfers, or on ingresos.

## Approaches considered

| Approach | Trade-off | Outcome |
| --- | --- | --- |
| **A. Own `presupuesto` table** with category, currency and amount, stamped and tombstoned like every root entity; a dedicated SQL aggregate and pure status rules | More code; scales to several currencies per category and joins Drive backup/sync with no extra work | **Chosen** by the user ("the option that scales best") |
| B. Amount column on `categoria` | Fastest; one amount per category (no multi-currency), budget edits bump the category's stamp and complicate sync, predefined categories become editable | Rejected |
| C. Derive from `StatisticsRepository.observe` | No new query; but distribution sums roots only (no child budgets) and the snapshot is too heavy per save | Rejected |

## Data

New stamped root table `presupuesto` (migration `12.sqm`, statements in `Presupuesto.sq`), shaped like
`recurrencia`:

```
presupuesto(id TEXT PK, categoria_id TEXT NOT NULL, moneda TEXT NOT NULL,
            monto_minor INTEGER NOT NULL CHECK (monto_minor > 0),
            created_at TEXT NOT NULL, updated_at TEXT NOT NULL, deleted_at TEXT)
```

- Follows [Change stamps and tombstones](../../business-logic.md#change-stamps-and-tombstones): one
  stamp per transaction, strict inserts, every read excludes tombstones, and `ChangeClock`
  preparation includes the new table.
- At most one live budget per `(categoria_id, moneda)`, checked by the repository (tombstones make a
  plain UNIQUE index wrong). Creating one that exists offers the existing budget, like duplicate
  names elsewhere.
- `monto_minor` is capped by `Money.MAX_MINOR_UNITS`.
- The currency is chosen at creation (primary currency by default; the selector appears only when
  the user has live accounts in more than one currency) and is locked afterwards.
- A hidden category keeps its budget and keeps counting. Deleting a custom category tombstones its
  budgets in the same transaction.

## Rules

All reads use the injected civil day (`CivilToday`), never the clock; sums are exact minor units in
SQL.

- **Spent** = Σ live gastos whose account is in the budget's currency, whose category is the
  budget's category or one of its children (filtering rule: a parent includes its children, a child
  only itself), with `fecha` from the first day of the current month through today. Ingresos,
  transfers and recurrente occurrences not yet confirmed do not count.
- Parent and child budgets coexist (Comida and Restaurantes): each is evaluated on its own.
- **Remaining** = monto − spent. **Per day** = remaining ÷ days left including today, rounded down to
  the minor unit; shown only while remaining > 0.
- **States**, the first that applies:
  1. `OVER` — spent > monto.
  2. `NEAR` — spent ≥ 80% of monto (spent == monto is NEAR with nothing left).
  3. `PACE` — from day 8 of the month (after the first week block, so one early large gasto does not
     dominate), spent > 0 and projection = spent ÷ elapsed days × days in month > monto. The day it
     would be reached is the first day `d` with spent ÷ elapsed × d > monto, at most the month's last
     day.
  4. `FINE` — otherwise.
- **Order** on screen: state (OVER, NEAR, PACE, FINE), then share spent descending, then category name.
- **Suggested amount** when creating: average monthly gasto of that category (children included) in
  the chosen currency over up to the three latest whole months with **any** gasto in that currency
  among the previous five — the same window Statistics uses for pace, so months before the user
  started recording are skipped while a month the category had no gasto counts as zero (one 300k
  gasto in three months suggests 100k, the true monthly average) — rounded up to two significant
  digits (420.337 → 430.000). No suggestion without such a month or when the average is zero.

## Screens and copy

Copy follows [wording](../../wording.md): `es` uses tú, `es-AR` voseo, `en`/`pt-BR` rephrase the
intent idiomatically ("$ 20,000 over your budget", "R$ 200 acima do seu orçamento"). Colors go from
neutral (FINE) to amber (NEAR, PACE) to soft red (OVER) using theme tokens; no warning icons.

### Presupuestos (Profile → Presupuestos)

```
Presupuestos                    noviembre · quedan 12 días
┌──────────────────────────────────────────┐
│  Vas bien en 5 de 7                       │
└──────────────────────────────────────────┘
🍔 Comida
   ████████████████████▌  $ 520k de $ 500k
   $ 20.000 por encima de tu presupuesto

🚕 Transporte
   ███████████░░░░░░░░░  $ 110k de $ 200k
   A este ritmo llegarías al tope el día 22

🎬 Ocio
   ██████░░░░░░░░░░░░░░  $ 90k de $ 200k
   Te quedan $ 110.000 · $ 9.000 por día
                                  [ + Presupuesto ]
```

- No group headers: order and color carry the state.
- Summary card: "Vas bien en X de N" (X = FINE budgets). Rows of a currency other than the primary
  show the currency code, like account pickers.
- Empty state invites creating the first budget.
- Row line by state:

| State | `es` row line | `es` notice after saving |
| --- | --- | --- |
| OVER | "$ 20.000 por encima de tu presupuesto" | "Comida: ya estás por encima de tu presupuesto este mes" |
| NEAR | "Te quedan $ 30.000" | "Comida: te quedan $ 30.000 este mes" |
| PACE | "A este ritmo llegarías al tope el día 22" | "Comida: a este ritmo llegarías al tope el día 22" |
| FINE | "Te quedan $ 110.000 · $ 9.000 por día" | (none) |

### Create / edit sheet

- Create: category (the existing single-select category picker, without Otros), currency when
  applicable, amount with the suggestion as a tappable hint ("Tu promedio: $ 430.000").
- A row opens its sheet: amount (editable), delete with confirmation (no Undo, like recurrencias),
  the **last six months** as bars of monthly gasto against the current amount with a met/over mark
  (labelled as compared with the current budget, since amounts are not versioned), and **"Ver
  movimientos"**, which opens Search filtered by the category (children included), the currency,
  type gasto and the current month (a [filter request](../../business-logic.md#search-and-filters)).

### Home

While any budget is not FINE, a compact row "N presupuestos para revisar" sits with the recurrentes
row and opens Presupuestos; it never changes Home's totals, and a failed read hides only the row.

### Notice after saving

- Raised after any successful save of a gasto: single creation, several movimientos, edit, and
  Pagado on a recurrente. Never after Undo/restore.
- Candidates are the budgets whose category subtree contains a saved gasto's category, in that
  gasto's currency, evaluated after the write — only for saved gastos whose `fecha` falls in the
  current month (a backdated gasto, or Pagado on last month's due date, does not touch this month's
  budgets). A budget an edit moved the gasto out of is not a candidate: leaving can only improve it.
  Only non-FINE candidates count; the most severe wins, ties go to the more specific category
  (child), then the higher share spent. Exactly one notice or none.
- **One snackbar per save.** Creation (single or several) has no snackbar today, so the budget notice
  is the only one, with the action "Ver" opening Presupuestos. Edit and Pagado already enqueue their
  own snackbar (`Saved`, `Confirmed` with Undo); there the budget line is merged into that snackbar's
  message and the existing action is kept (Undo wins over "Ver"), so nothing stacks and Undo removes
  the notice together with the save.
- Shown every time a save touches a non-FINE budget, not only on the first crossing: the moment of
  recording is when awareness helps.
- Delivered through the shell's `ManagementFeedbackQueue`; a standalone notice is informational
  (Short). The feedback model gains an optional budget line rather than a second queued item.

## Builder notes (existing pieces to reuse)

- Table and repository patterns: `Recurrencia.sq`, `RecurrenciaRepositoryImpl.kt`, `ChangeClock` /
  `HybridChangeClock.kt`.
- Pace window and "whole months with gasto": `core/model/StatisticsRules.kt` (`pace`).
- Category picker, Profile entry rows and the Home recurrentes row
  (`feature/home/HomeRecurrentesRow.kt`) as visual templates.
- Creation ends with `AddMovimientoEffect.Saved` (`AddMovimientoSubmission.kt`); edits go through
  `ManageMovimientoViewModel`; feedback through `core/ui/feedback/ManagementFeedbackQueue.kt`.
- Business rules land in `docs/business-logic.md` (new *Presupuestos* section), the feature row in
  `docs/features/README.md`, and the backlog item (which still says "built on the Statistics
  queries") is removed when delivered.

## Calls made while writing (check these)

- Pace state starts on day 8 of the month.
- The suggestion averages over months with any gasto in the currency, counting zero category months.
- Backdated gastos and gastos moved out of a budget raise no notice.
- Edit and Pagado merge the budget line into their existing snackbar; Undo keeps priority over "Ver".
- Deleting a custom category tombstones its budgets; a hidden category keeps its budget.
- The six-month history compares past months against the current amount (amounts are not versioned).

## Left to the builder

Package layout (`feature.budgets`), exact component composition, the chart used for the six-month
bars, `en`/`pt-BR` strings within the wording rules, and test split (pure rules in `commonTest`,
SQL in host tests, final check on the confirmed physical Xiaomi).

## Delivered with these changes

The rules, screens and notice as shipped are in
[Presupuestos](../../business-logic.md#presupuestos); scope and verification boundaries in
[Budgets](../../features/budgets.md). Shipped details that differ from the sketches above: amounts use
the app-wide `Money.format()`; a NEAR budget with nothing left reads "Ya usaste todo tu presupuesto de
este mes"; the six-month history draws the current amount as a dashed line and counts only closed
months; "Ver" opens Presupuestos through `PrimaryNavigator.showSecondary`.
