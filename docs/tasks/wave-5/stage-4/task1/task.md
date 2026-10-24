# Wave 5 / Stage 4 / Task 1 — Medio and account in form, summary and editor

## Objective

Add the Medio section and the Account selector to the sentence dialog's Detected area, the review
summary, the creation form and the movimiento editor, with currency derived from the account.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md) (MedioPago, Movimiento changes, Entry and
  selection), `docs/business-logic.md` → Natural-language entry and Editing and deletion (the contracts
  being extended), `docs/ui/architecture.md` → Movimiento management and feedback → Direct editor and
  shared form, `docs/ui/components.md` (`CategoryPickerSheet`, `TallybookSegmentedControl`,
  `TallybookChoicePill`, `TallybookSelectionField`).
- Skills: `android-skills:android-dev`, `android-skills:compose`,
  `chrisbanes-skills:compose-state-and-effects`, `chrisbanes-skills:compose-component-design`,
  `chrisbanes-skills:compose-ui-testing-patterns`, `android-skills:android-ux`,
  `superpowers:test-driven-development`.
- Current state: `MovimientoFormState` carries `moneda` directly; `AddMovimientoViewModel` reads the
  primary currency for new drafts; `MovimientoSentenceUi`/`MovimientoSummaryUi` expose category and
  tags; `MovimientoPrimaryFields` renders tipo/amount/date; `EditMovimientoDraftState` compares with
  the opening baseline.

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/account/{MedioPagoControl,CuentaSelectorField,CuentaPickerSheet}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoCuentaField.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/CuentaSelectionState.kt` (pure: compatible-account filtering, ownership layering over `core.model.CuentaSelection`, tipo-switch rule; **no fallback logic** — that is `MovimientoRepository.defaultSelection`)
- Modify: `feature/movimientos/viewmodel/{MovimientoFormState,MovimientoFormActions,AddMovimientoUiState,AddMovimientoEntryUi,AddMovimientoViewModel,AmountInput,EditMovimientoDraftState,EditMovimientoUiState,ManageMovimientoActions,ManageMovimientoViewModel}.kt`
- Modify: `feature/movimientos/ui/{MovimientoSentenceStep,MovimientoSummaryStep,MovimientoForm,MovimientoFormLayout,ManageMovimientoContent,AddMovimientoSheet}.kt` and their `*Previews.kt`
- Modify: `di/ViewModelModule.kt` (constructor changes only)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/{CuentaSelectionStateTest,AddMovimientoCuentaTest,ManageMovimientoCuentaTest}.kt`; update `AddMovimientoFixture.kt`, `ManageMovimientoFixture.kt`, `AmountInputTest.kt` in the same folder (fakes come from Stage 2's `commonTest/.../core/data/`)
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoCuentaFieldTest.kt`

## Contracts

```kotlin
/** Stateless; the caller filters options by tipo with MedioPago rules from core.model. */
@Composable
fun MedioPagoControl(selected: MedioPago?, options: List<MedioPago>, onSelect: (MedioPago) -> Unit, modifier: Modifier = Modifier)

@Composable
fun CuentaSelectorField(selected: Cuenta?, compatibleCount: Int, onOpenPicker: () -> Unit, modifier: Modifier = Modifier)

internal data class CuentaSelectionState(
    val medio: MedioPago?,
    val cuentaId: String?,
    val owner: SelectionOwner, // SENTENCE or USER, mirroring the tag ownership model
)

/** Layers ownership only; [default] is already resolved by MovimientoRepository.defaultSelection. */
internal fun preselect(default: CuentaSelection): CuentaSelectionState
```

`CuentaSelectorField` and the picker show the type with Stage 2/task1's `CuentaTypeIcon` and the
medio control uses `MedioPagoIcon`/`MedioPagoLabels`; Stage 4 creates no icon or label of its own.

Actions gain `onMedioSelected(MedioPago)`, `onCuentaSelected(String)`, `onCreateCuenta(nombre)`. The
draft's `moneda` becomes a derived getter from the selected account; no second stored copy.

## Exact rules

- [MedioPago](../../README.md#mediopago-payment-method), [Movimiento changes](../../README.md#movimiento-changes)
  and [Entry and selection](../../README.md#entry-and-selection) are authoritative.
- Order in every surface: tipo/amount/date, **Medio**, **Account**, Category, Tags, then optional texts.
- Medio options are filtered by tipo; switching tipo applies the README's DEBITO → TRANSFERENCIA rule.
- Picking a medio whose current account is incompatible selects `defaultSelection(primary, medio)`'s
  account; when it returns `null` (no compatible account) → empty selector with in-place create.
- Account picker: live, non-archived, compatible accounts; non-primary currencies show their code;
  search appears from 8 accounts up (same threshold rule as other pickers if one exists — check the
  category picker before inventing one).
- Amount: `AmountInput` parses with the selected account's `Moneda`; account change re-validates a
  typed amount and re-derives a sentence-owned one (task2 provides the re-derivation hook).
- Editor: account options restricted to the stored currency; an archived/missing stored account stays
  visible and valid while untouched; a no-op still writes nothing.
- Save failure for incompatible medio/account from the repository maps to the field error and focus,
  like other invalid required values.
- Quick-create (last step): reuses Stage 3/task1 `QuickCuentaViewModel` with the type preset from the
  medio; the created account becomes selected.

## TDD

- [x] RED `CuentaSelectionStateTest`: compatible-account filtering, tipo switch, ownership layering over a resolved default (fallbacks themselves are tested in Stage 2/task2).
- [x] RED `AddMovimientoCuentaTest`: defaults, user ownership, currency follows account, CLP scale error, Save persists medio/cuenta, Fill manually keeps user-picked medio/account.
- [x] RED `ManageMovimientoCuentaTest` (after rebasing on Stage 3 commit A): same-currency options, untouched archived account, no-op, repository rejection mapping.
- [x] GREEN; shared components with previews; device test for medio → account selection (quick-create pending).
- [x] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.
- [ ] Quick-create: waits for Stage 3/task1's `QuickCuentaViewModel` and `AccountForm` on this branch.

## Implementation record (quick-create pending)

- `CuentaSelectionState` has a third owner, `DEFAULT`, besides `SENTENCE`/`USER`: clearing the
  sentence or Discard must return to the repository's selection, which neither of the other two
  owners describes.
- The view models cache `defaultSelection` once without a medio and once per medio (read as gasto,
  adapted with `forTipo`), re-read on every cuenta emission (`CuentaReads.kt`). The rules stay
  synchronous, so the engine can resolve the sentence's currency before reading the amount. The
  editor reads them with the stored currency as `primary`; a default in another currency is dropped.
- With one compatible cuenta the field selects it (`MovimientoCuentaUi.selected`), so no fallback
  of its own exists beyond "the only option".
- A repository `InvalidInput` on Save re-reads the cuenta (`CuentaRepository.accepts`); only a cuenta
  it no longer takes becomes the field error, anything else stays the shared save error.
- Order: the form keeps its layout (tipo, **Medio, Account**, Category, Tags, date, amount, texts);
  the review keeps Title after Amount and inserts Medio/Account before Category.
- `MovimientoSentenceCallbacks.cuenta`/`MovimientoSummaryCallbacks.cuenta` default to an inert set so
  the UI catalog (`core/ui/catalog`, not Stage 4's) keeps compiling unchanged.

## Blocks

Blocked by Stage 2 review; editor part by Stage 3/task2 commit A; quick-create by Stage 3/task1.
Blocks task2's engine integration and Stage 5.
