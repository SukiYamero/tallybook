# Wave 5 / Stage 3 — Accounts and transfers UI

## Objective

Give accounts and transfers their own surfaces: the Accounts destination (from Profile and from
Home's balance card), the account sheet, the transfer form and its management, with Undo through the
existing shell feedback queue. Home's balance card switches to the total balance.

## Tasks

- [**task1 — Accounts screen, account sheet and Home total balance**](task1/task.md): new
  `Accounts` secondary destination, `feature.accounts`, Profile link, Home card. Also publishes the
  account quick-create contract that Stage 4 consumes.
- [**task2 — Transfer form, management and generalized feedback**](task2/task.md): extracts the
  feedback queue out of `ManageMovimientoViewModel` into a shell-level owner first (**commit A**),
  then builds `feature.transfers` on top of it.

task1 and task2 run in parallel after task2's commit A lands, because task1's account sheet offers
"Transfer" and "Pay card" through a callback only (wired in task1's nav host edit, implemented by
task2's route). Commit A edits `core/ui/navigation/{ManageMovimientoOwner,TallybookNavHost}.kt`
before task1 starts; from then on task1 owns `core/ui/navigation/*` and task2 adds its route and
modal admission in a separate commit rebased on task1.

## Parallel with Stage 4

Stage 4 owns `feature/movimientos/**` and `core/data/entry/**`. The only Stage 3 edit inside
`feature/movimientos/**` is task2's commit A (feedback extraction: `MovimientoFeedback.kt`,
`MovimientoFeedbackHost.kt`, the queue section of `ManageMovimientoViewModel.kt` and its feedback
tests). **Commit A lands on `feat/accounts` before Stage 4/task1 edits `ManageMovimientoViewModel`**;
Stage 4 rebases on it. After commit A, Stage 3 never touches `feature/movimientos/**` again.

DI rebase order for `di/ViewModelModule.kt` and `di/RepositoryModule.kt`: Stage 3/task1 → Stage
3/task2 → Stage 4 tasks, each appending its own declarations in a separate commit.

New strings go to `composeResources/values*/strings_accounts.xml` (created by Stage 2/task1, which
proved split resource files on CMP 1.11.1).

## Exit gate

- Accounts lists every live account grouped by type with balances and per-currency totals; credit
  cards show estimated debt and cycle spending; archived accounts sit under Archived.
- Account create/edit/archive/unarchive/delete follow the [Cuenta rules](../README.md#cuenta-account),
  including the referenced-currency and last-active-account rejections surfaced inline.
- Home shows the primary-currency total balance with this month's ingresos/gastos and opens Accounts.
- Transfers can be created (same and cross-currency), edited, deleted with confirmation and restored
  with Undo from the same snackbar queue as movimientos, in arrival order.
- Host tests, Native compilation, `detekt`, `ktfmtCheck` and `:androidApp:assembleDebug` pass.

## Review

No intermediate review: Stage 6 runs the final independent review. If commit A changes the public
feedback contract beyond the generalization below, stop and request a bounded review before Stage 4
rebases on it.
