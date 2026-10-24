# Wave 5 / Stage 4 — Entry and editing with medio and account

## Objective

Every creation and editing surface asks for the payment method and the account, the sentence detects
them, and movimiento rows show the method. Runs in parallel with Stage 3.

## Tasks

- [**task1 — Medio and account in form, summary and editor**](task1/task.md): shared
  `core.ui.account` medio control and account selector; drafts carry `cuentaId`/`medio`; currency
  follows the account; preselection from `MovimientoRepository.defaultSelection`; in-place account
  quick-create.
- [**task2 — Sentence detection of medio and account**](task2/task.md): lexicon and account-name
  matching in `core/data/entry`, ownership in the entry engine. Runs in parallel with task1 up to the
  engine integration step, which waits for task1's draft fields.
- [**task3 — Medio on movimiento rows**](task3/task.md): row icon and accessibility label in Home,
  History and Search rows. Independent of task1/task2.

## Ownership against Stage 3

Stage 4 owns `feature/movimientos/**`, `core/data/entry/**`, `core/ui/movimiento/**` except `BalanceCard*` (Stage 3) and the new
`core/ui/account/{MedioPagoControl,CuentaSelectorField,CuentaPickerSheet}.kt`; it consumes Stage
2/task1's `CuentaTypeIcon`, `CuentaTypeLabels`, `MedioPagoIcon` and `MedioPagoLabels`. It never edits
navigation, Home, `feature/accounts/**`, `feature/transfers/**` or `core/ui/feedback/**`.

- `ManageMovimientoViewModel.kt` is edited only after Stage 3/task2 **commit A** is on
  `feat/accounts`; rebase first.
- The in-place quick-create consumes Stage 3/task1's `QuickCuentaViewModel`; that step is last in
  task1 and waits for its commit.
- DI: append to `di/ViewModelModule.kt`/`di/EntryModule.kt` in separate commits after Stage 3's.
- Strings: `composeResources/values*/strings_entry_accounts.xml` only.

## Exit gate

- Creating through sentence → summary → Save and through the manual form persists the chosen medio
  and account; the repository never receives an incompatible combination.
- Editing keeps the currency fixed and offers only same-currency accounts.
- Sentence detection passes the per-language tables in task2; ambiguity offers choices.
- Rows expose the medio to sight and TalkBack.
- Host tests, Native compilation, `detekt`, `ktfmtCheck`, `:androidApp:assembleDebug` pass; existing
  smart-entry, management and Undo tests remain green without weakened assertions.

## Review

No intermediate review; Stage 6 final review covers it.
