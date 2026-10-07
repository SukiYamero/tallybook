# Wave 2 / Task 1 — detail, edit, delete and Undo integration

## Objective and dependencies

Connect both Wave 1 contracts into one shell-owned flow and preserve accepted Add/Home/History/Search
behavior. Read the [overview](../../README.md), [snackbar](../../wave-1/task1/task.md) and
[persistence](../../wave-1/task2/task.md) contracts. Do not start against invented APIs or partially
passing producer boundaries. Use relevant Android/Compose state, component, Flow and test skills;
load the Gradle execution skill before any Gradle command.

## Sole integration ownership and files

Under `shared/src/commonMain/kotlin/com/kurobello/tallybook/`:

- Create `feature/movimientos/viewmodel/ManageMovimientoViewModel.kt`, `ManageMovimientoUiState.kt`,
  `ManageMovimientoActions.kt` and `MovimientoFeedback.kt`. Keep draft/state and feedback models in
  their actual responsibility files. Add a Channel/effect file only for required one-shot effects.
- Create `feature/movimientos/ui/ManageMovimientoRoute.kt`, `ManageMovimientoContent.kt` and
  `MovimientoDetailContent.kt`.
- Modify `MovimientoForm.kt`, `MovimientoPrimaryFields.kt`, `MovimientoCategoryEntry.kt` and
  other directly affected movement controls for an internal shared form presentation/actions
  contract. Adapt `AddMovimientoSheet.kt` and Add UI state/actions narrowly; preserve creation tests.
- Modify `di/ViewModelModule.kt`, `core/ui/navigation/TallybookNavHost.kt` and
  `PrimaryModalCoordinator.kt` for root-owner resolution, controller admission and feedback hosting.
- Extend `core/ui/component/TallybookModalBottomSheet.kt` narrowly with an optional explicit Back
  hook; preserve existing callbacks/defaults for Add and filters.
- Modify `core/ui/layout/TallybookAppShell.kt` only if a generic overlay slot is needed to expose
  measured clearance; reuse its existing measurement/inset owner.
- Modify shared `core/ui/movimiento/MovimientoRow.kt`/`MovimientoRowContent.kt` and real rows/routes
  in `feature/home`, `feature/history/ui` and `feature/search/ui` to carry an optional click callback.
  Add no clicks to existing loading placeholders.
- Localize production movement/Undo/error/Close labels in all existing values locales; coordinate
  with Wave 1's catalog keys, now completed.

Tests: create `ManageMovimientoViewModelTest.kt` and `MovimientoUndoTest.kt` under the matching
commonTest feature package; extend `AmountInputTest.kt`, `PrimaryModalCoordinatorTest.kt`, form
and existing source-screen tests. Add a root-owner recreation integration test under androidHostTest
and a focused `ManageMovimientoSheetTest.kt` under androidDeviceTest. Discover exact existing fixture
owners before editing. One worker owns this whole integration; no parallel shell/form/copy edits.
You are not alone: accommodate Wave 1 changes and never revert another worker's work.

## State, actions and wiring seams

`ManageMovimientoViewModel` receives MovimientoRepository, CategoriaRepository, Clock,
TimeZoneProvider and testable coroutine dependencies following existing constructors. It does not
depend on primary-currency preferences or IdGenerator. Resolve it once with Koin from a remembered
child owner at the shell, outside decorated primary entries and the Add session's temporary owner.
Use Lifecycle's existing `rememberViewModelStoreOwner(parent=..., savedStateRegistryOwner=null,
defaultCreationExtras=...)` and stable captured parent extras. Its provider retains the child across
Android configuration destruction and clears composition-owned children on active iOS scene disposal.
Do not make it a process-global DI singleton.

Its immutable UiState has separate concerns: optional current session with monotonic token/record ID,
typed read/mode state, immutable persisted snapshot, editable draft and submitting status; optional
feedback with its own token/revision and private captured Undo data. Expose minimal presentation
instead of exposing financial snapshots to the generic snackbar. Derived readiness/dirty status is
computed, never a second independently writable flag.

Actions must include open(id), retryLoad, edit, field edits, save, cancelEdit, requestDelete,
confirmDelete/cancelDelete, requestDismiss/discard, sheetHidden(origin), feedbackAction(token, revision)
and feedbackDismiss(token, revision, reason). Use narrow callback subsets below the route; never pass a ViewModel
into plain content. The action set's exact names can follow existing conventions without changing
the overview's transitions.

The ViewModel owns Closing plus its committed pending outcome. Sheet visible state derives from the
active phase. `sheetHidden(origin)` alone completes closure and promotes its feedback. Root state
survives the supported parent-owner recreation; dismissal clears session data after animation, not
the whole ViewModel or a running Undo. Reads use cancellable jobs/generation guards; writes are
serialized in this owner. Keep current Add session/store unchanged.

`PrimaryModalCoordinator` attaches a management controller with `hasActiveSession` including Closing
and `isRestoring` mutation admission. Creation, filters and row opening check the same coordinator.
Block tab navigation while a management modal is active/closing; allow tabs during Undo restoration,
but forbid starting another modal/write. An unavailable Search controller remains conservative.

Root wiring passes source row IDs and an optional onClick down existing routes. The shared row uses
the established TallybookSurface press feedback and one localized open-detail accessibility action,
retaining amount/title semantics. Loading/no-data/unknown-error regions do not acquire row actions.

The production snackbar host is composed once above shell chrome using the actual measured bar
clearance, available IME and existing safe-inset ownership. `isActive` requires foreground plus no
active/closing creation, filters or management modal. Do not display below viewport scrim or inside
a dismissing sheet. No second snackbar host in individual primary entries.

## Form reuse and operations

- Extract only the Add-specific form seam needed for title/save wording, displayed input/errors and
  callbacks. Retain amount parsing, field validation focus order, description limit/normalization,
  category picker, details scrolling, keyboard reveal, detents and discard primitives. Add still
  starts category-first and uses its existing generated identity/currency owner.
- Edit amount prefill uses the positive minor-unit decimal digits plus currency scale/separator;
  no Double, grouping, symbol or signed display formatter. Preserve long boundaries without numeric
  multiplication overflow. Compare normalized values for no-op Save; equivalent formatting does
  not mutate the DB or announce a false update.
- Currency is the stored movement's. An unchanged child/missing category remains representable;
  replacing it follows current picker policy. Category loads never reset draft fields.
- Back inside Edit is Cancel; clean Cancel returns to Detail, dirty Cancel asks to discard. Whole
  sheet dismissal and child-surface Back follow the overview. Read failures, future-date validation,
  missing writes and storage errors have separate visible recovery states.
- The existing modal wrapper merges Back/scrim/drag. For management's explicit Back hook, disable
  library Back dismissal and use the already available
  `androidx.navigationevent.compose.NavigationBackHandler` with
  `rememberNavigationEventState(NavigationEventInfo.None)`. Compose handlers unconditionally with
  enabled state; nested date/category/confirmation handlers get precedence. Returning to Detail
  vetoes Hidden. Do not use deprecated Compose BackHandler or assume Unstyled's iOS EscapeHandler,
  which is a no-op, proves escape behavior. Confirm exports and full nested interaction on targets.
- For changed Save use update(id, MovimientoChanges), never insert. For delete use deleteAndCapture,
  never the copied detail row. Capture committed outcome before entering Closing. UI never modifies
  source lists or balances directly.
- Mutation tokens reject repeated clicks synchronously before launching. While writing/restoring,
  no second mutation/dismiss action is accepted. Transactions and success delivery must be tested
  with the actual retained root owner, not just a fake isolated ViewModel.
- Feedback is one replaceable typed state. Atomic Undo claim transitions Available→Restoring before
  launching restoreDeleted. Timeout/Close/replacement only removes unclaimed available/failed
  feedback. A stale token or revision cannot act on a newer snapshot or retry window. Restore success/equal presence, conflict
  and failure/retry map to the exact overview outcomes; busy pauses the generic host timer.

## Test-first integration slices

- [x] Pure input tests: exact round-trip prefill for COP/USD/CLP and a Long money boundary;
      normalization/no-op Save; parent/child/missing category preservation; changed clock date error.
- [x] Session tests: real-ID loading, read failure→retry, missing record; detail→edit→clean/dirty
      cancel; discard resets only the draft; save failure retains typed input; duplicate Save/Delete
      submits once; closing rejects input and old hidden callbacks; disposed reads cannot publish.
- [x] Undo tests with one scheduler: delete capture is sole snapshot; timeout/action accepted in
      either order; late timeout after claim cannot cancel restoration; newer notification expires
      older available Undo; no modal admission while restoring; fail→retry/expire; same/different
      existing ID; stale callbacks never consume a newer token. No real clock sleeps.
- [x] Actual owner integration (host): attach root Koin/ViewModel lifecycle, recreate supported Android
      parent while editing and while write/restore is pending, reattach modal/host, verify one DB
      write and one terminal feedback. Fresh owner starts closed without snapshots. This is an API
      integration gate; isolated state tests cannot establish Activity/controller retention.
- [ ] Connect real repository flows: change date/type/category/description so History/Search rows
      leave or enter current criteria; verify count/totals/Home recent-six ordering. Delete last row
      to empty/no-data; restore to original ordering. Preserve selected period, filters, raw query
      and retained scroll state; ordinary lazy-list clamping is allowed when rows disappear.
- [ ] Device fixtures: each source row opens detail; long description scroll/footer; no forced Edit
      keyboard; unchanged category behavior; confirmation cancellation; dirty dismiss; true sheet
      hidden→snackbar sequence; usable Undo/Retry/Close semantics and IME/chrome separation.
- [ ] Run focused tests/build/style and Native compilation; perform the authorized physical smoke
      described in the overview. Runner limitations do not turn compiled device tests into passes.
- [x] Run one independent final diff review, fix material findings and repeat only affected checks.

## Documentation and handoff

Update canonical domain/UI owners with accepted behavior and root/snackbar lifecycle, the component
catalog with actual implemented APIs, and feature registry to IN_PROGRESS during execution. Keep
verification limits explicit; DONE requires user device acceptance and integration in main. Compact
this active spec only at closure, using a small durable `features/manage-movimientos.md` when useful.
Do not add this entire task ledger to business rules or duplicate earlier delivery evidence.

Future commit: `feat(movimientos): add detail editing deletion and undo`. Handoff names changed
behavior, actual checks/target evidence and material limitations. No automatic merge; use the
repository's existing acceptance workflow.

## Integration evidence

- The current fix-wave focused regression output contains 104 passing tests across 18 suites, with zero failures,
  errors or skips. This includes management/Undo/input/Add compatibility, coordinator admission,
  supported Activity/Koin/SQLite owner behavior and existing repository/reactive seams.
- Initial management/Undo behavior, child-owner admission and paused current-token delivery each
  had observed behavioral RED checkpoints. Category recovery also had an observed SAVE_FAILED RED;
  its unchanged assertion passes after the catalog-derived presentation correction.
- Session owns one catalog/load status and submitting status. Edit owns original/draft/validation and
  save-error inputs; `session.form` derives a read-only presentation without another Flow or copied
  writable catalog/status. Category recovery cannot reset typed input or an unretried save failure.
- The actual owner fixture explicitly registers its sandbox-visible JDBC driver and deregisters only
  that owned instance during cleanup. It uses real SQLite, Koin and Activity recreation. Active scene
  disposal cancels pending work; supported Activity recreation retains edit/claimed restore and one
  terminal result. These host assertions do not establish actual iOS scene teardown ordering.
- Root feedback delivery checks live lifecycle/modal/restoration admission at callback execution.
  One shell overlay uses measured chrome/IME/safe insets; pending restore has localized progress copy.
  Closing interactivity derives from phase, with no false submitting spinner. Add defaults remain.
- Device fixtures cover detail actions, Edit prefill/description/focus, Closing controls, explicit Back
  and each source's stored row ID. Android production/device builds, mandatory style checks and Native production/test
  compilation pass. Rebuilt-Xiaomi main-flow and pause-window evidence is recorded below; physical
  iOS and assistive-technology acceptance remain unconfirmed.

## Bounded review and target correction

- Independent review found that capture accepted readable legacy rows that strict restore rejects.
  The added real-SQL regression had an observed RED while the prior 14 repository tests passed.
  Current capture checks the same restore eligibility before DELETE inside the transaction and
  leaves invalid exact rows intact; strict restore and legacy raw delete behavior remain.
- The candidate APK physically confirmed closed keyboard on Edit entry, but amount focus covered
  Save/Cancel with the IME. Current shared form follows Add's existing footer policy: the entire
  Save/Cancel content is absent while the IME is visible and restored after dismissal. A device
  fixture asserts absent semantics and both reachable actions after Done; target execution remains
  unconfirmed until the updated APK/device run.
- Nineteen reported Detekt findings are addressed by focused rendering/wiring sections and equivalent
  guards. No new suppressions or configuration changes were added. The modal correction passed 104 focused tests; final shell extraction passed 24 navigation/owner
  regressions. Android builds/style/Native compilation and scoped independent re-review pass.

## Rebuilt Xiaomi verification

The current production APK was installed and launched on the confirmed Xiaomi. Home, History and
Search open the stored fixture ID. Physical Back closes Detail, clean Cancel returns full detail,
dirty Cancel prompts discard, category Back returns to Edit, and Edit starts without forced IME.
Save/Cancel disappear with IME and return after dismissal.

The authorized sole fixture changed Expense 123 to Income 124 with success feedback and reactive
Search/History/Home rendering. Delete confirmation Cancel preserved it; Delete/Undo restored it.
Undo remained actionable after 12 seconds in background and after more than 12 seconds covered by
Add, then restored the record. Final Delete plus Close removed the fixture; retained Search query
showed 0 results. No original records were modified.

Instrumentation assertions remain blocked by the recorded runner stall. Actual iOS scene teardown,
safe-area/VoiceOver and TalkBack/large-text checks are unconfirmed. User acceptance is pending;
feature remains IN_PROGRESS and canonical docs/spec compaction wait for the accepted-feature workflow.
