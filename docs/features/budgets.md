# Budgets (Presupuestos)

A monthly amount per category that tells the user, without blocking anything, when they are close to it,
past it, or spending fast enough to pass it. Rules, screens and the notice live in business logic:
[Presupuestos](../business-logic.md#presupuestos), with the Home row under
[Home](../business-logic.md#home) and stamps under
[Change stamps and tombstones](../business-logic.md#change-stamps-and-tombstones). Approved design:
[spec](../superpowers/specs/2026-11-18-budgets-design.md). Components are in the
[catalog](../ui/components.md).

## Scope

Presupuestos in Profile (create, edit, delete, six-month history, "Ver movimientos"), a Home row while a
budget needs a look, and one in-app notice after saving a gasto. Not included: system notifications, a
global or per-tag/account budget, weekly or annual periods, rollover, FX (a budget counts only its own
currency), a Statistics section, a month picker, CSV export of budgets.

## Integration

- **Schema 13 (`12.sqm`)** adds `presupuesto` (stamped, tombstoned); `Presupuesto.sq` holds the
  statements and the spent aggregate. `ChangeClock` bootstrap includes the table.
- `core.model`: `Presupuesto`, `BudgetState`, `BudgetStatus` and the pure `BudgetRules` (status over an
  injected civil day, order, suggestion, touches and the notice pick).
- `core.data`: `PresupuestoRepository` enforces one live budget per `(categoria, moneda)`; deleting a
  custom category tombstones its budgets in the same transaction.
- UI: `feature.budgets` (`BudgetsViewModel` with `UiState`, effects and `Actions`; screen, `BudgetRow`,
  editor and detail sheets, `BudgetSheetHost`), `HomeBudgetsRow` in `feature.home`, strings in
  `strings_budgets.xml` for every locale.
- Notice: creation enqueues only the budget notice; edit and Pagado merge its line into their snackbar;
  Pagado keeps Undo, while edit gains "Ver" (see
  [snackbar presentation](../ui/architecture.md#snackbar-presentation)).
- Navigation: Presupuestos is a secondary destination from Profile, the Home row and the notice's
  "Ver", which uses `PrimaryNavigator.showSecondary` (over the selected tab when allowed, otherwise over
  Home, replacing an open secondary; Back returns to that primary's root).

## Verification boundaries

Host evidence on the final tree: `:shared:testAndroidHostTest` ran 1,624 tests with zero failures, errors
or skips, and Detekt and ktfmtCheck passed. Coverage: pure rules over states, month ends, order,
suggestion and notice choice; repository SQL over the real schema (spent per budget, children, currency,
tombstones); ViewModel, creation/edit/Pagado notice paths and the six-month tally; resource parity.

On the Xiaomi M2012K11AG, `BudgetsScreenTest` passed (3 tests).

Accepted by the user on the Xiaomi.

Not covered: iOS runtime (see the backlog's iOS item), other locales on device
and enlarged text.
