# Wave 5 / Stage 3 / Task 1 — Accounts screen, account sheet and Home total balance

## Objective

Add the Accounts secondary destination, modeled on Categories and Tags, with the account sheet for
create/edit/archive/delete, and switch Home's balance card to the total balance of the
primary-currency accounts.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md) (sections Cuenta, Balances and credit-card
  estimate, Display surfaces), `docs/ui/architecture.md` (Balance summary cards, Navigation
  architecture, Movimiento management and feedback → Shell ownership and admission) and
  `docs/ui/components.md` (`TallybookScreenHeader`, `TallybookSheetForm`, `TallybookModalBottomSheet`).
- Skills: `android-skills:android-dev`, `android-skills:compose`, `chrisbanes-skills:compose-state-and-effects`,
  `chrisbanes-skills:compose-component-design`, `chrisbanes-skills:compose-ui-testing-patterns`,
  `android-skills:android-ux`, `android-skills:koin`, `superpowers:test-driven-development`.
- Model to imitate: `feature/categories/ui/{CategoriesRoute,CategoriesScreen,CategorySheetHost,
  CategoriesEditorSheet}.kt`, `feature/categories/viewmodel/{CategoriesViewModel,CategoryDraftState,
  QuickCategoryViewModel}.kt` and `feature/tags/**`. Same modal admission through
  `PrimaryModalCoordinator`, same `active` flag, same UiState/Actions split.

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/accounts/ui/{AccountsRoute,AccountsScreen,AccountSheetHost,AccountForm,AccountsScreenPreviews}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/accounts/viewmodel/{AccountsViewModel,AccountsUiState,AccountsActions,AccountDraftState,QuickCuentaViewModel}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/account/CuentaBalanceRow.kt`
  (consumes Stage 2/task1's `CuentaTypeIcon`/`CuentaTypeLabels`; Stage 4 adds the selector and medio
  control next to it in its own files)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/{Destinations,TallybookNavHost}.kt`
  (`@Serializable data object Accounts : Destination`, `primaryContentKey` → `"secondary-accounts"`,
  `PrimaryRoute` branch; `PrimaryNavigationState` only if secondary handling needs it)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/profile/ui/ProfileScreen.kt` (Accounts `NavigationCard` before Categories)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/{HomeContent,HomeUiState,HomeViewModel,HomeScreen,HomePresentation}.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/movimiento/BalanceCard.kt` only if a label/click slot is missing
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt`
- Create: `shared/src/commonMain/composeResources/values*/strings_accounts.xml` entries (file exists from Stage 2/task1)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/accounts/viewmodel/{AccountsViewModelTest,AccountDraftStateTest,QuickCuentaViewModelTest}.kt`
  (consuming Stage 2's `FakeCuentaRepository`/`FakeSaldoRepository`) and
  `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/feature/accounts/viewmodel/AccountsViewModelGraphHostTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/home/{HomeViewModelTest,HomePresentationTest}.kt` (update)
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/accounts/ui/{AccountsScreenTest,AccountsNavigationTest}.kt`

## Contracts

```kotlin
internal interface AccountsActions {
  fun onSearchChanged(query: String)
  fun onCreate()
  fun onOpenCuenta(id: String)
  fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState)
  fun onSave()
  fun onRequestArchive(archived: Boolean)
  fun onRequestDelete(); fun onConfirmDelete(); fun onCancelDelete()
  fun onDismissSheet(): Boolean
  fun onRetry()
}

/** Reused by Stage 4's in-place quick-create; the medio presets the type and is not editable. */
internal class QuickCuentaViewModel /* nombre + tipo preset + moneda (default primary) + saldo inicial */

@Composable
internal fun AccountsRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    onTransfer: (origenId: String?, destinoId: String?, prefill: Money?) -> Unit,
)
```

Home receives `onOpenAccounts: () -> Unit` from `TallybookNavHost` (`navigator.openSecondary(Accounts)`).
"View movements" is **not** wired in this task: it needs the Search account filter from Stage 5,
which adds `cuentaIds` to `SearchFilterRequest`/`PrimaryNavigator.showSearchFilter` and the button.

## Exact rules

- Everything the README fixes: [Cuenta](../../README.md#cuenta-account),
  [Balances and credit-card estimate](../../README.md#balances-and-credit-card-estimate),
  [Display surfaces → Home and Accounts](../../README.md#display-surfaces).
- Totals header: one total per currency present, primary currency first, then `Moneda` declaration
  order. Groups: Efectivo, Banco o billetera, Tarjeta de crédito; inside a group by name; Archived last,
  collapsed by default.
- Credit-card rows show "Deuda estimada" and "Consumo del ciclo" with the cycle's civil date range in
  the accessibility label; a positive card balance reads "Saldo a favor".
- The sheet's draft compares with its opening baseline (same guard as the category editor);
  delete/archive need confirmation; inline errors map `CuentaWrite.Duplicate` (same name in that
  currency), `CuentaWrite.CurrencyLocked`, `CuentaArchive.LastActive`, `CuentaDelete.LastActive` and
  `CuentaDelete.InUse` (offering archive).
- Opening debt is entered as a positive amount for `CREDITO`; `AccountDraftState` is the only place
  that negates it into the signed `CuentaDraft.saldoInicial` (and back when editing). The form never
  asks for a sign; the repository stores what it receives.
- Rows show the currency code next to accounts not in the primary currency.
- Home: `HomeContent` gains the total balance and this month's ingresos/gastos (civil month from the
  injected clock/time zone, refreshed on resume like History); recent rows unchanged. The card keeps
  `BalanceCard` typography and becomes clickable with a localized action label.
- No Material dialogs; reuse `TallybookConfirmationDialog`, `TallybookSheetForm`, `TallybookScreenHeader`.

## TDD

- [ ] RED: `AccountDraftStateTest` — normalization, 30-char cap, positive opening debt ↔ negative `saldoInicial` round trip (the only conversion), `diaCorte` only for credit, baseline dirtiness.
- [ ] RED: `AccountsViewModelTest` — grouping/ordering, per-currency totals, credit estimates, every repository rejection mapped inline, retry, stale load rejection.
- [ ] RED: `HomeViewModelTest` — total balance from the read model, month secondary values, month rollover on resume.
- [ ] GREEN each; then screens, previews (light/dark), nav destination and Profile link.
- [ ] Device tests: Profile → Accounts → back; create account from the sheet; Home card opens Accounts.
- [ ] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.
- [ ] Device tests compile (`:shared:compileAndroidDeviceTestKotlinAndroid` or the task the wrapper lists); execution follows the backlog runner limits.

## Blocks

Blocked by Stage 2 review and Stage 0 merge. Blocks Stage 4/task1's quick-create (contract only) and
Stage 5/task1 ("View movements").
