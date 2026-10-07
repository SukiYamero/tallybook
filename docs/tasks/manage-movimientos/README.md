# Manage movimientos — planning contract

## Goal and planning status

Complete the local movimiento lifecycle: open a detail bottom sheet from Home, History or Search,
edit an existing record, delete it, and undo that deletion through reusable snackbar feedback.
The user selected this feature first and confirmed detail-first bottom-sheet interaction and Undo.
The execution contract is approved for implementation. Delivery and physical acceptance are in
progress; current evidence and task ownership live in [the continuation ledger](execution.md).

Read [repository workflow](../../README.md), [domain rules](../../business-logic.md),
[UI architecture](../../ui/architecture.md), [component catalog](../../ui/components.md) and
[wording](../../wording.md). Existing delivery boundaries remain in
[Add](../../features/add-movimiento.md), [History](../../features/history.md) and
[Search](../../features/search.md). This active spec owns the proposed change; promote accepted
domain/UI rules into their canonical owners during integration, without duplicating this spec there.

## Included and excluded scope

Included: single-record detail, editing type/amount/date/category/description, confirmed deletion,
Undo, operation feedback, source-list integration, and a reusable snackbar with its own commit.
Existing money, date, category and description rules continue to apply.

Excluded: tags, bulk actions, swipe-to-delete, duplication, currency conversion/selection, category
authoring, charts, Google/Drive, sync/outbox/tombstones, persistent trash, dependency upgrades and
iOS cross-process restoration. Undo reverses deletion only; it does not revert edits. Adding a new
movimiento retains its accepted behavior; adding snackbar feedback to creation is outside this slice.

## Repository observations and consequences

| Evidence | Consequence | Owner |
| --- | --- | --- |
| `core/data/MovimientoRepository.kt` exposes insert and idempotent delete; `Movimiento.sq` has select-by-ID but no update | Add read-by-ID and typed mutation outcomes; keep existing delete compatibility | Wave 1 / persistence |
| `MovimientoRepositoryImpl.kt` uses exact integer money and dispatcher-bound local operations | Update editable columns only; atomically capture the actual deleted record for Undo | Wave 1 / persistence |
| `AddMovimientoViewModel.kt` generates IDs and follows primary-currency preferences | Reuse controls/validation, not creation identity or currency ownership | Wave 2 / integration |
| `MovimientoForm.kt` consumes Add-specific state/actions; Add starts category-first | Extract the small shared form contract; Edit starts with populated fields and no automatic picker/focus | Wave 2 / integration |
| `PrimaryModalCoordinator.kt` excludes creation and filter sessions, including closing | Extend this exclusion to management; no competing overlay flag | Wave 2 / integration |
| `AddMovimientoSheetSession.kt` retains its store through origin-checked hidden callbacks | Preserve Add behavior; apply origin checking to management closure and feedback publication | Wave 2 / integration |
| `TallybookAppShell.kt` measures bar clearance; viewport scrim and modal layers can cover root content | Place one snackbar above measured chrome; reveal only after every modal is hidden | Wave 2 / integration |
| Home/History/Search already observe SQLDelight | Let DB emissions refresh lists/counts/balances; never patch a second list manually | Wave 2 / integration |
| No snackbar exists in the component catalog or shared UI | Build the reusable component independently, without repository/domain dependencies | Wave 1 / snackbar |

All Kotlin evidence above is under `shared/src/commonMain/kotlin/com/kurobello/tallybook/`;
SQL lives under `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/`.

## Detail and editing interaction

1. Tapping a real row requests its ID, not a copied editable record. Fetch the current DB row; show
   loading, read failure with Retry, or a distinct missing-record state. Never render invented data.
   Placeholder rows and empty states are not clickable. A repeated tap cannot create another session.
2. Detail uses `TallybookModalBottomSheet`: localized title, signed exact amount/currency, type, date,
   category and complete description when present. Missing categories use the existing fallback.
   Content scrolls when necessary; Edit and destructive Eliminar remain reachable in a fixed footer.
3. Edit replaces Detail inside the same modal session. It reuses movement controls and validation.
   No second editor sheet, category-first phase or automatic keyboard opening is added. A populated
   description starts expanded. No amount field uses grouped/symbol-bearing display text as input.
4. Edit fixes currency to the stored movement currency. Type, positive exact amount, civil date,
   category and optional description are editable. ID and `createdAt` cannot be edited. Category
   selection retains the current parent-only policy; an existing child or missing category is shown
   and preserved if untouched. A newly selected category must exist in the loaded catalog.
5. Capture civil today/timezone at editing entry and again on submission using injected providers.
   Future dates cannot be saved. A backward clock change shows a date error; it never silently clamps
   the input. Reuse amount parsing and description normalization from creation.
6. Save remains available to trigger field validation, except while loading or writing. Field errors
   remain beside their fields. A valid normalized no-op performs no UPDATE, returns to Detail and
   shows no misleading saved notification. Dirty guarding compares editable input to its opening
   baseline; database-write comparison uses normalized domain values.
7. Successful changed Save closes the whole management sheet. After hidden, show the saved snackbar
   on the original destination. Failed Save retains the exact draft and shows a persistent inline
   message with explicit retry through Save. Retrying captures fresh validation inputs.
8. Cancel/Back from Edit returns to Detail. Changed input first uses the existing discard guard;
   Continue keeps editing, Discard resets the draft and returns to Detail. Scrim/drag requests closure
   of the whole session, with the same dirty guard. Back/scrim/drag during a category/date child
   surface closes that surface first. Detail closes without a dirty confirmation.
9. Returning from Edit rereads Detail from the DB; a failed reread offers Retry instead of stale
   mutation actions. A missing row disables Edit/Delete and offers Close. Failed edit against a
   missing row retains its draft with explicit Close/discard rather than resurrecting the record.

Use content-sized sheet detents bounded by the existing safe viewport, scrolling the body while
reserving header/footer. Reuse current keyboard reveal and detent measurement. Update pane titles
when modes change and remove hidden mode content from input/accessibility. No new fixed sheet
percentage or hardcoded visual token is required.

## Deletion, Undo and feedback

- Delete is offered from Detail only, never over an unsaved edit. Reuse the confirmation dialog,
  identify the movement with its amount/date/category, and state that Undo is available briefly.
  Cancel, Back or scrim cancels confirmation. Confirm executes once; pending writes disable editing,
  navigation to another modal and dismissal. Delete failure keeps Detail with localized inline retry.
- Delete commits immediately. Its DB transaction returns the exact deleted row. Only that snapshot
  is eligible for Undo. Already missing closes to an informational snackbar with no Undo.
- Successful delete closes the sheet and presents Eliminar feedback with Deshacer after hidden.
  Undo restores the same ID, currency, `createdAt`, type, exact amount, date, category and description.
  Never generate a replacement ID, overwrite a present ID, or use an old detail snapshot.
- There is one current feedback/Undo token. A newer completed management operation replaces older
  feedback and expires the older unclaimed Undo. Opening a modal hides feedback and suspends its
  usable window; it does not immediately discard the snapshot. Switching primary tabs keeps feedback.
- Proposed duration: informational success is 4 seconds; Undo and retry feedback is 10 seconds.
  Every foreground reveal after a modal/background gives a full usable duration. Ordinary
  recomposition never restarts it. Accessibility may extend this duration. No visible countdown.
  Close or timeout expires an unclaimed Undo and releases its snapshot. Process termination/fresh
  launch also loses Undo; the deletion already committed remains applied.
- Undo atomically claims its token before launching restoration. Disable action/Close while restoring
  and suspend timeout. A late timeout cannot expire a claimed operation. Tabs may change, but new
  creation/management/filter sessions wait until restoration finishes. No unbounded feedback queue.
- Successful restore replaces feedback with Restored. An exactly equal record already present also
  counts as restored without a write. A different record at the same ID is a terminal conflict:
  preserve it, release the deleted snapshot and show a clear non-retry error.
- Storage failure retains the snapshot and offers Retry for a new 10-second usable window. Retry
  claims the same operation once. Expiry/Close after failure releases the snapshot. Never report
  restored before commit, or claim the deleted record is still saved after restoration failed.
- Inline errors are the durable recovery mechanism while a sheet is open. The snackbar confirms
  committed outcomes and owns the actionable Undo/retry feedback outside the sheet.

Draft UX wording (localize idiomatically per `wording.md`): Movimiento actualizado; Movimiento
eliminado / Deshacer; Movimiento restaurado; Este movimiento ya no está disponible; No pudimos
restaurar el movimiento / Reintentar; No pudimos restaurar esta versión. El movimiento actual se conservó.
Do not expose raw storage errors, IDs, amounts or descriptions through logs/analytics.

## Ownership and lifecycle

SQLDelight is the persisted source of truth. A shell-scoped `ManageMovimientoViewModel`, resolved
from a remembered child ViewModelStoreOwner outside primary Navigation 3 entries, owns one detail/edit session, read generation, editable draft,
submission, closing outcome and transient Undo snapshot. This placement is required because Undo
outlives the sheet. It does not change Add's ephemeral session/store or primary entry retention.

Use immutable UiState plus Actions and the project's buffered Channel convention only for actual
one-shot effects. Pending committed feedback and restoration status are state, not a lossy event.
Composition owns focus, expansion, keyboard, sheet animations and snackbar timers. Plain
`ManageMovimientoContent` and `TallybookSnackbar` render state/callbacks without Koin or repositories.
`PrimaryModalCoordinator` admits management through one attached controller, with active status
including loading, detail, edit, confirmation, submission and closing. Root callbacks carry tokens.

Keep the closing render snapshot until its origin hidden callback. On that callback, promote pending
feedback, clear draft/detail data and stop reads. An old callback cannot close a new session or
publish a second notification. Cancel stale read jobs and reject late results by session generation.
Do not overwrite editable input from incidental category/read emissions.

Within the supported Android parent ViewModel lifetime, configuration recreation retains the active
management session and an in-flight mutation/Undo; reattach the modal/host to that owner. This is
process-local only: do not put financial drafts, snapshots, jobs or tokens in preferences or saved
state. A fresh process/scene starts closed with no feedback. Current iOS architecture ownership is
scene-based, not a guarantee for the full UIViewController lifetime. The remembered child owner must
clear management/Undo jobs on scene disposal while retaining them across Android configuration
recreation. Prove this supported-owner boundary; Native compilation is insufficient.

## Dependencies, ownership and commits

Use the existing managed feature worktree. Follow the project branch convention at handoff;
commits are coordinated by the orchestrator. Merge remains gated by physical user acceptance.

| Wave | Execution unit | Dependencies | Future commit |
| --- | --- | --- | --- |
| 1 | [Reusable snackbar](wave-1/task1/task.md) | Reviewed presentation/callback contract | `feat(ui): add reusable snackbar` |
| 1 | [Persistence mutations](wave-1/task2/task.md) | Reviewed mutation/Undo contracts | `feat(movimientos): add safe update and undo persistence` |
| 2 | [Management integration](wave-2/task1/task.md) | Both Wave 1 deliveries | `feat(movimientos): add detail editing deletion and undo` |

Wave 1 can run in parallel: snackbar owns components/theme/catalog; persistence owns repository,
domain mutation types and SQL. Wave 2 exclusively owns the shell, ViewModel DI, feature routes,
shared form extraction and production feedback/copy. Neither Wave 1 worker touches those resources.
Only the orchestrator stages/commits and schedules Gradle/device runs: shared build outputs cannot
be driven concurrently. Keep the snackbar diff independent and free of movimiento wiring.

Review the shared contract before dispatch. No automatic intermediate review per task is required;
perform one final independent feature review under the proportional policy. Stop for a targeted
boundary review only if actual owner/API/persistence/platform evidence invalidates this contract.

## Acceptance and verification scope

| Requirement | Smallest meaningful evidence | Execution owner |
| --- | --- | --- |
| Identity-safe edit and transactional delete/restore | Real in-memory SQLite repository tests, including invalid/missing/conflicting records | Persistence |
| Exact edit prefill, dirty/no-op rules and validation | Common tests with valid COP/USD/CLP amounts, fixed clock and one test scheduler | Integration |
| One session and origin-safe closure | Coordinator/ViewModel tests plus real sheet/owner recreation integration | Integration |
| Undo claim versus timeout/replacement and failure retry | Deterministic host/common state tests; no wall-clock sleeps | Integration |
| Reactive Home/History/Search consistency | Existing repository observation fixtures, mutation + emission assertions | Integration |
| Timer, action semantics and placement | Snackbar component/device fixture; physical usable-window and accessibility smoke | Snackbar + integration |

At execution: first run affected host classes with `./gradlew :shared:testAndroidHostTest --tests
'<fully-qualified-class>'` (repeat `--tests` for the chosen classes), then
`./gradlew :androidApp:assembleDebug` and mandatory `./gradlew detekt ktfmtCheck`.
Compile shared production/tests for iOS Arm64 using confirmed task names from the wrapper; do not
run the simulator test command. Discover the device-test build task before building its fixture.
Broaden checks only for a confirmed regression/shared integration risk.

Physical smoke uses isolated, authorized records: open from each list; edit into/out of its existing
criteria; confirm delete; Undo; cancel a dirty edit; confirm snackbar above chrome and large-text
action reachability. Cover modal/background pause and supported Android recreation. Check TalkBack
on Android and VoiceOver/safe areas on an authorized physical iPhone. Never alter production records
or seed a dataset without dataset authorization. Remove only fixture-owned data.

Before installing, instrumentation or visual review, identify exactly one authorized Xiaomi,
Android tablet or iPhone and resolve local toolchain paths without editing `local.properties` or
signing settings. If unavailable, stop that target step and report it. Existing blocked Android
instrumentation and deferred iPhone runtime work remain limits, not passes. Use one bounded runner
diagnosis and an explicit manual alternative; no scale benchmark is needed for this feature.

## External API evidence and re-plan gates

Declared in `gradle/libs.versions.toml`: Kotlin 2.4.10, Compose Multiplatform 1.11.1, Unstyled 2.9.0,
Navigation 3 1.1.1, Lifecycle 2.11.0-beta01, Koin 4.2.2, SQLDelight 2.3.2. Execution resolution confirms
these components in Android runtime/iOS Arm64 compile configurations; Android Compose UI/runtime
resolve to AndroidX 1.11.2 beneath the common Compose 1.11.1 coordinates. No dependency change is proposed.

- [Compose 1.11.1 accessibility timeout contract](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/AccessibilityManager.kt)
  supports adapting the usable duration to content/controls. Handle an effectively indefinite
  recommendation without duration overflow. The versioned
  [Skiko default manager](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/platform/DefaultAccessibilityManager.skiko.kt)
  returns the original timeout; the [root owner](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/node/RootNodeOwner.skiko.kt)
  supplies that manager. Current iOS common-provider behavior therefore does not promise VoiceOver
  extension. Verify its action usability physically; adapt only if target evidence shows a need.
- [Official semantics guidance](https://developer.android.com/develop/ui/compose/accessibility/semantics)
  supports polite snackbar announcements and localized pane titles; a moving countdown is unsuitable
  for a live region. [KMP accessibility](https://kotlinlang.org/docs/multiplatform/compose-accessibility.html)
  does not substitute for a VoiceOver runtime check.
- Modal measurement/layer references already live in Add's delivery document. Reuse those contracts
  and verify the actual sheet → hidden → root snackbar sequence; a timer unit test cannot prove it.
- [Published Lifecycle 2.11.0-beta01 iOS source](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose-iosarm64/2.11.0-beta01/lifecycle-viewmodel-compose-iosarm64-2.11.0-beta01-sources.jar)
  confirms the remembered child-owner/provider cleanup contract. Existing PrimaryEntries uses this
  API family. Host Activity recreation and active-scene disposal tests exercise the actual provider;
  physical iOS scene teardown remains a separate gate.
- [Versioned NavigationBackHandler source](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/navigationevent/navigationevent-compose/src/commonMain/kotlin/androidx/navigationevent/compose/NavigationEventHandler.kt)
  supplies explicit Back handling without deprecated Compose BackHandler. Published Native
  navigationevent-compose 1.0.1 and Android navigationevent-compose 1.0.2 signatures were audited;
  common 1.0.1 is resolved transitively by Navigation 3. Full nested Back is a target integration check.
- The Android hook must be composed inside the native modal content: published
  [Unstyled 2.9.0 modal sources](https://repo.maven.apache.org/maven2/com/composables/composeunstyled-modal-android/2.9.0/composeunstyled-modal-android-2.9.0-sources.jar)
  create a separate ComposeView in a ComponentDialog. Published
  [ComponentDialog 1.13.0 sources](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0-sources.jar)
  install its own navigation dispatcher; published
  [navigationevent-compose Android 1.0.2 sources](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose-android/1.0.2/navigationevent-compose-android-1.0.2-sources.jar)
  resolve that owner through the modal LocalView tree. An Activity-level hook does not establish
  Back handling for this window. Physical KEYCODE_BACK must demonstrate the relocated integration.
- SQL transaction integration is exercised through the project's real SQLDelight drivers. Added
  queries do not change the table schema; confirm generated APIs/affected-row results from the
  resolved version. Do not hand-edit generated queries or historical schema snapshots.

Re-plan if Undo needs cross-process recovery, multiple queued deletions, sync participation, currency
editing, unavailable root-owner retention, or a dependency/platform adapter beyond current APIs.
Those are material scope/architecture changes, not implicit implementation defaults.
