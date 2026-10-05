# Wave 3 / Stage 5 / Task 1 — Shared navigation and sheet lifetime

## Objective and context

Enable History and Search as real primary destinations while preserving one creation session and
retained History inputs. Read the [wave contract](../../README.md), canonical UI architecture,
`TallybookNavHost`, `HomeRoute` and the current sheet-session tests. Requires Stage 4 reviewed.

Skills: `android-dev`, `compose`, `compose-state-and-effects`, `kotlin-concurrency-and-flow`, `koin`,
`compose-ui-testing-patterns`, `android-testing`, `test-driven-development`, `gradle-run`.
Complete the navigation API evidence gate before changing entry ownership.

## Files and responsibility

- Modify `core/ui/navigation/Destinations.kt`, `TallybookNavHost.kt` and `TallybookBottomNavigation.kt`
  under `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.
- Create `core/ui/layout/TallybookAppShell.kt` and a small internal navigation state holder if needed.
  Own enabled primary destinations, selected route, saved entry state and shared creation wiring.
- Create `feature/history/ui/HistoryRoute.kt`: lifecycle collection, presentation resources and the
  pure HistoryScreen boundary. The content composable still sees only immutable state/callbacks.
- Create `feature/search/ui/SearchRoute.kt`: Search state collection and filter draft lifecycle.
- Modify `feature/home/HomeScreen.kt`, its `HomeSheetSessionLifecycle.kt` and
  `core/ui/layout/TallybookScreenFrame.kt` for the targeted ownership transfer.
- Modify `feature/home/HomeViewModel.kt` sharing for retained-entry visibility after checking its
  production `.value` consumers. Update `HomeViewModelTest.kt` and `FirstMovimientoIntegrationTest.kt`
  with active test collectors, preserving their existing assertions and shared scheduler.
- Move/generalize `AddMovimientoSheetSession` to `core/ui/movimiento/AddMovimientoSheetSession.kt`
  because it now serves all enabled primary destinations. Move tests without weakening assertions.
- Extend `core/ui/navigation/DestinationSerializationTest.kt` in commonTest; create
  `core/ui/navigation/PrimaryNavigationTest.kt` there and
  `TallybookAppShellLifecycleHostTest.kt` in androidHostTest.
- Create `core/ui/navigation/HistoryNavigationTest.kt` in androidDeviceTest; extend sheet route tests.
- Update `docs/ui/architecture.md` to describe the now-real primary destinations and shell ownership.

The paths above are relative to the existing `com/kurobello/tallybook` package in their named source
sets. Resolve exact filenames before moving tests; do not broaden this task into package cleanup.

## Navigation and retention contract

1. Add serializable `History : Destination` and `Search : Destination`. The active route determines
   selection. Home/History/Search receive callbacks; Profile remains a reservation with no click role.
2. Retain one navigation entry/store/saveable-state scope per enabled primary destination. Use a
   small Navigation 3 primary-state pattern with typed saved back stacks and selected primary key,
   keeping decorators/state holders above the active display. Lazily create History/Search on first visit.
   Do not create a general navigation framework or stacks for unimplemented destinations.
3. Switching tabs selects retained primary entries instead of popping/destroying them. Each screen's
   ViewModel and scroll owner remain scoped to that entry, not the Activity or app root. Inactive
   screen collectors follow their actual visibility/lifecycle; measure that they do not leak work.
   Home currently uses Eagerly, so lifecycle UI collection alone does not stop its upstream reads.
   After confirming production has no uncollected fresh `.value` contract, use WhileSubscribed with
   the agreed timeout and retain UiState cache. Tests reading `.value` add a real collector rather
   than weakening assertions. Prove inactive upstream counts reach zero after the timeout and that
   reactivation re-reads changes made while hidden without an unmarked stale success state.
4. Tapping the selected destination is a no-op: no duplicate route, forced scroll reset or filter reset.
   Repeated Home/History/Search cycles have bounded entry/store counts.
5. System Back first dismisses a year popup/IME or the sheet under its existing guard. From History
   or Search without a transient surface, select Home while preserving retained entries. From root
   Home, let the platform handle its normal exit/back behavior; never empty the root stack into a blank UI.
6. UiCatalog remains a development-only secondary route. Its return restores the previously active
   primary destination. Preserve explicit catalog launch mode; do not expose production catalog actions.
7. Persist typed route and each screen's small input/scroll state through supported saveable/entry-store
   integration. Process restoration recreates repository observations; it does not persist live rows,
   jobs, keyboard focus or an in-progress sheet submission.
8. Fresh app entry without saved navigation starts Home; a saved entry may restore History or Search.
   A cold start deliberately creates new screen state rather than copying filters to DataStore.
9. If the declared KMP artifacts cannot retain/decorate inactive primary entries as required, stop and
   revise the ownership design with evidence. Do not silently replace it with ambient ViewModels or
   a second live copy of editable filter state in the shell.

## Sheet and inset contract

- Shell owns one `AddMovimientoSheetSession`, one ViewModelStore and one AddMovimientoSheetRoute.
  Home/History/Search add callbacks delegate to that owner; there is no second creation form.
- Keep the accepted category-first entry, initial cancellation, save validation/retry, keyboard
  behavior, detail disclosure and changed-draft dismissal guard untouched.
- The session remains alive throughout the hide transition, releases after `onHidden`, rejects reopen
  during closing and clears once on disposal. Opening a new session starts a new draft as today.
- While the modal is open, underlying primary navigation cannot change destination via tap. Saving
  returns to the same destination/filter; a newly dated item outside the current period need not appear.
- Do not append movements, refresh History manually or reset search after save; SQL invalidation owns it.
- Shell owns the persistent bottom bar and its safe inset. Screens reserve its published clearance;
  screen/frame and bar must not both add bottom safe space. The sheet retains its own IME/safe policy.
- Home's search surface now opens retained Search. A deliberate Home entry focuses its input once;
  ordinary tab selection and restoration do not repeatedly request focus.
- Search's filter sheet and global creation sheet are mutually exclusive. Dismiss/apply completes
  editing but creation waits until onHidden finishes Closing. No filter reopen during Closing.
  The shell coordinates availability, without owning filter values or another create session.

## Test-first slices

- [ ] RED: primary selection/no-op taps, bounded entries, Back History -> Home -> platform root and
      secondary catalog return. GREEN: implement navigation holder and typed History/Search routes.
- [ ] RED: full serialized primary navigation state restores the active route. GREEN: integrate the
      existing sealed serializer plus verified per-entry state/store APIs for the resolved versions.
- [ ] RED: actual Home/History/Search switching preserves each input/scroll and creates distinct entry
      ViewModels. GREEN: wire entry owners; serializer-only tests do not complete this step.
- [ ] RED: recreation restores small History input and scroll while data reloads; removed/disposed
      owners clear exactly once and cancel subscriptions. GREEN: connect saved-state lifecycle.
- [ ] RED: inactive Home/History/Search reads stop after virtual-time timeout, reactivation catches
      database changes and distinct owners do not leak collectors. GREEN: sharing/lifecycle integration;
      retain existing Home assertions with explicit active collectors in their fixtures.
- [ ] Move sheet/session ownership and run existing lifecycle regressions unchanged; test creation
      from all primary destinations, scrim/drag/Back guard, close/reopen and save failure retry.
- [ ] Test one bar, disabled reservations, selected semantics and content clearance/IME behavior.
- [ ] Test filter Closing blocks global creation/reopen until hidden, then permits exactly one fresh
      creation session; stale hidden callbacks cannot clear a newer modal session.
- [ ] Verify resolved Nav3/Lifecycle/Koin coordinates and execute the actual entry integration on Android
      and iPhone when available; report each target separately.
- [ ] Run host regression, Android build, Native test compilation, Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*PrimaryNavigationTest' --tests '*DestinationSerializationTest' --tests '*TallybookAppShellLifecycleHostTest' --tests '*SheetSession*'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`
and `./gradlew :androidApp:assembleDebug`. Discover exact instrumentation tasks before execution.

## Completion / blockers

Complete when entry/state/sheet lifetime is proven through the real shell. Blocks Stage 6.
A missing physical iPhone leaves that runtime evidence pending; compile success is explicitly separate.
