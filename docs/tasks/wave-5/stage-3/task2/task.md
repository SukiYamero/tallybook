# Wave 5 / Stage 3 / Task 2 — Transfer form, management and generalized feedback

## Objective

Let the user create, edit and delete transfers between own accounts, including cross-currency and
"Pay card", and restore a deleted transfer with Undo through the **same** shell feedback queue as
movimientos.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md) (Transferencia, Balances, Change stamps →
  Undo revives), `docs/ui/architecture.md` (Movimiento management and feedback: Shell ownership and
  admission, Snackbar presentation) and `docs/ui/components.md` (`TallybookSheetForm`,
  `TallybookModalBottomSheet`, `TallybookAmountInput`, `TallybookDateChipPicker`).
- Skills: `android-skills:android-dev`, `android-skills:compose`,
  `chrisbanes-skills:compose-state-and-effects`, `chrisbanes-skills:kotlin-concurrency-and-flow`,
  `chrisbanes-skills:compose-ui-testing-patterns`, `android-skills:koin`,
  `superpowers:test-driven-development`.
- Current owner: `ManageMovimientoViewModel` holds `pendingFeedback`, the FIFO `feedbackQueue`, the
  per-token deleted `Movimiento` snapshot and the claim/restore cycle (`onFeedbackAction`,
  `onFeedbackDismiss`, `onSheetHidden`). `MovimientoFeedbackRoute` renders the queue head; the shell
  attaches management through `rememberManageMovimientoViewModel` / `MovimientoManagementController`.
  The README's anti-pattern table forbids a second queue: generalize this one.

## Commit A — feedback generalization (lands first, before Stage 4 edits `ManageMovimientoViewModel`)

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/feedback/{ManagementFeedback,ManagementFeedbackQueue,DeletedSnapshot}.kt`
- Modify: `feature/movimientos/viewmodel/{ManageMovimientoViewModel,ManageMovimientoUiState,MovimientoFeedback}.kt`,
  `feature/movimientos/ui/MovimientoFeedbackHost.kt` (renamed or moved to `core/ui/feedback/ManagementFeedbackHost.kt`),
  `core/ui/navigation/{ManageMovimientoOwner,TallybookNavHost}.kt`
- Test: move/adapt `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/{ManageMovimientoFeedbackTest,MovimientoUndoTest,ManageMovimientoFixture}.kt`; add `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/feedback/ManagementFeedbackQueueTest.kt`

```kotlin
sealed interface DeletedSnapshot {
  data class OfMovimiento(val movimiento: Movimiento) : DeletedSnapshot
  data class OfTransferencia(val transferencia: Transferencia) : DeletedSnapshot
}

/** One per shell, outliving every sheet; tokens/revisions keep today's claim semantics. */
internal class ManagementFeedbackQueue(scope: CoroutineScope, restore: suspend (DeletedSnapshot) -> DataOutcome<RestoreResult>)
```

Behaviour is unchanged for movimientos: same kinds, timing, arrival order, single claim, retry on
failure, release on close/expiry. The queue lives in one shell-scoped owner reachable by both
management ViewModels (the existing remembered child `ViewModelStoreOwner` hosts a small
`ManagementFeedbackViewModel`, or an equivalent single owner — choose one and justify it in the
commit). Restore dispatch picks `MovimientoRepository.restoreDeleted` or
`TransferenciaRepository.restoreDeleted` by snapshot type. The `RestoreResult.MissingCuenta`
mapping Stage 2/task3 added is carried through the extraction unchanged and applies to both snapshot
kinds; do not add a second mapping.

## Commit B — transfers

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/transfers/ui/{TransferSheetRoute,TransferForm,TransferFormPreviews}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/transfers/viewmodel/{ManageTransferenciaViewModel,TransferDraftState,TransferUiState,TransferActions}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/account/TransferenciaRowContent.kt` (row used by Accounts now and History/Search in Stage 5)
- Modify: `core/ui/navigation/{TallybookNavHost,PrimaryModalCoordinator}.kt` (attach transfer management as another admitted modal), `feature/accounts/ui/AccountsRoute.kt` callback wiring only
- Modify: `di/ViewModelModule.kt`; strings in `strings_accounts.xml`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/transfers/viewmodel/{TransferDraftStateTest,ManageTransferenciaViewModelTest}.kt` (consuming Stage 2's `FakeTransferenciaRepository`/`FakeCuentaRepository`); `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/transfers/ui/TransferFormTest.kt`

```kotlin
internal interface TransferActions {
  fun onOrigenSelected(id: String); fun onDestinoSelected(id: String)
  fun onMontoOrigenChanged(value: String); fun onMontoDestinoChanged(value: String)
  fun onFechaChanged(value: LocalDate); fun onDescripcionChanged(value: String)
  fun onSubmit(); fun onRequestDelete(); fun onConfirmDelete(); fun onCancelDelete()
  fun onRequestDismiss(): Boolean; fun onContinueEditing(); fun onDiscardChanges()
}
internal sealed interface TransferEntry {
  data class New(val origenId: String?, val destinoId: String?, val prefill: Money?) : TransferEntry
  data class Existing(val id: String) : TransferEntry
}
```

## Exact rules

- [Transferencia](../../README.md#transferencia-transfer-between-own-accounts) and
  [Balances](../../README.md#balances-and-credit-card-estimate) own the domain rules.
- Received amount field appears only when the two accounts' currencies differ; switching back to the
  same currency drops it. Amounts use `AmountInput` round-tripping with each side's currency scale.
- Pay card: destination = the card, origin = `TransferenciaRepository.defaultOrigin(primary)`
  (Stage 2), and the estimated debt prefills the **received** amount (`montoDestino`); for a
  same-currency origin that is the single amount field, for a different-currency origin
  `montoOrigen` starts empty. The prefill is empty when the debt is zero. `TransferEntry.New.prefill`
  is therefore the destination-side amount.
- Unsaved-change guard, Continue/Discard, Back, scrim and drag follow the movimiento editor contract
  in `docs/ui/architecture.md` → Direct editor and shared form. Submission cannot repeat.
- Archived accounts are not offered as new endpoints; an existing transfer keeps them while untouched.
- Feedback is delivered after the sheet hides, exactly as movimiento management does.

## TDD

- [x] Commit A RED: queue tests for mixed movimiento/transfer snapshots, arrival order, single claim, retry, release; existing feedback tests stay green unchanged in assertions.
- [x] Commit A GREEN, then run the movimiento management suite; push commit A and notify the orchestrator so Stage 4 can rebase.
- [x] Commit B RED: draft validation (same account, zero, future date, missing received amount), Pay card prefill (same and cross-currency origin), edit/delete/Undo through the queue, late callback rejection.
- [x] GREEN; form UI, previews, device test for create → delete → Undo.
- [x] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.

## Blocks

Blocked by Stage 2 review and Stage 0 merge. Commit A blocks Stage 4/task1's editor work; commit B
blocks Stage 5/task1 (transfer row).
