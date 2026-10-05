# Wave 3 / Stage 5 / Task 1 — Home/History navigation and sheet lifetime

## Objective and execution slices

Enable only History beside Home while preserving one creation session and retained selection/scroll.
Read the [wave contract](../../README.md), canonical UI architecture, current TallybookNavHost,
HomeRoute/session tests and scratch navigation audit. Search remains a reserved later destination.

Skills: `android-dev`, `compose`, `compose-state-and-effects`, `kotlin-concurrency-and-flow`, `koin`,
`compose-ui-testing-patterns`, `android-testing`, `test-driven-development`, `gradle-run`.

This task has two dependency points: **early gate after Stage 1, by Stage 2 closure, before S3
transport selection**; **remaining full-shell integration after reviewed Stage 4**. A fresh reviewer
checks the early diff/probe before S3. Full Stage 5 review later covers the accumulated shell diff.

## Early gate before Stage 3

- Add the matching official KMP `lifecycle-viewmodel-navigation3` adapter using the existing Lifecycle
  version reference in `gradle/libs.versions.toml` and `shared/build.gradle.kts`. Current audit identifies
  it as absent; resolve the updated Android/Native graph, especially savedstate wrappers, before use.
  No dependency version upgrade, Koin navigation DSL or generic navigation framework is required.
- Check exact resolved decorators/typed serializer/Koin/SavedStateHandle source contracts under the
  repository API gate. Primary decorators/saveable holders stay above active display; popup/visual
  proof is later. The audit's source feasibility is not real integration evidence.
- Stable parent creation extras are required: capture default extras with `remember(parentOwner)`,
  use the official `rememberViewModelStoreProvider` and `ViewModelStoreNavEntryDecorator(provider)`.
  Real Activity Intent extras otherwise produce fresh DEFAULT_ARGS Bundles and reset entry stores on
  recomposition. Preserve the verified provider seam in the full shell, including catalog launch mode;
  do not copy the default decorator helper without proving equivalent stable extras.
- Own a focused `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/ui/navigation/HistorySavedOwnerHostTest.kt`
  (proposed name; existing nonvisual Composition/UnitApplier harness may be reused). Use real runtime
  decorators → entry owner → Koin → injected SavedStateHandle and actual save/restore registry owners.
  Small test-only History input is sufficient to prove typed scope/anchor transport before S3 exists.
- Exercise Home/History, no Search route/ViewModel/filter for the probe. Switch away/back: distinct
  primary owners, same retained History VM/input, bounded store count. Save while History inactive;
  clear/destroy original parent owners; construct fresh registry/store owners from saved state and
  recreate entries/Koin VMs. Assert restored selected route/scope/anchor, new VM identities and reads
  restarting. Reusing the old parent store only proves retention.
- Remove a primary key/dispose shell and prove exactly-once clearance/cancellation. Merely not displaying
  History must not pop/clear its retained entry. Saveable/VM decorators are one integrated chain.
- Record target boundary: Android supported saved-owner/Activity recreation; iOS retained-controller/
  saveable scene scope. Fresh iOS process launches without a restoration bridge and starts normally.
  No new persistence/host bridge or cross-process restoration promise is authorized.
- Run focused probe plus Native production/test compilation, Detekt/ktfmt and fresh bounded review.
  If real integration fails, revise transport with evidence before S3. Full NavDisplay/scroll/Back and
  affected physical targets remain later gates; the nonvisual host probe cannot substitute them.

## Remaining files and responsibilities

CommonMain paths below are relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.

- Modify `core/ui/navigation/Destinations.kt`, `TallybookNavHost.kt`, `TallybookBottomNavigation.kt`.
- Create `core/ui/layout/TallybookAppShell.kt` and small internal primary-state holder only as needed:
  actual Home/History typed primary selection/entries/saveable ownership and shared creation wiring.
- Create `feature/history/ui/HistoryRoute.kt`: lifecycle state collection, explicit fallback-label
  presentation updates, activation/date refresh and pure HistoryScreen boundary.
- Modify `feature/home/HomeScreen.kt`, `HomeSheetSessionLifecycle.kt`,
  `core/ui/layout/TallybookScreenFrame.kt` only for targeted ownership/inset transfer.
- Modify `feature/home/HomeViewModel.kt` sharing after checking production synchronous `.value` usage;
  preserve its UiState cache while marking/revalidating re-entry. Update existing HomeViewModelTest/
  FirstMovimientoIntegrationTest fixtures with real active collectors rather than weakening assertions.
- Move/generalize `AddMovimientoSheetSession` into `core/ui/movimiento/AddMovimientoSheetSession.kt`
  for the two real consumers; move its tests with assertions intact and bind hidden callback identity.
- Extend commonTest `core/ui/navigation/DestinationSerializationTest.kt`; create
  `PrimaryNavigationTest.kt` nearby and `TallybookAppShellLifecycleHostTest.kt` in androidHostTest.
- Create `core/ui/navigation/HistoryNavigationTest.kt` in androidDeviceTest; extend sheet route tests.
- Update `docs/ui/architecture.md` for real History, inert Search/Profile and actual shell ownership.

No SearchRoute, Search destination registration, Search DI/model, filter handshake or Home search
focus/navigation wiring belongs to this task. Preserve Home's current inert search surface until its
separate destination is implemented; do not redirect it to History.

## Navigation, retention and restoration contract

1. Add serializable History : Destination. Enable only Home/History callbacks and selected semantics;
   Search/Profile stay reservations with no click/selection role. UiCatalog remains secondary/dev-only.
2. Retain one visited primary entry/store/saveable scope per enabled destination using a small typed
   Navigation 3 primary pattern. Home always exists; History is visited lazily. Hoist stable decorated
   entries/keys above active display; decoration alone must not compose screens/resolve their VMs.
3. Switching tabs selects existing entries rather than popping them. VMs/scroll are scoped to actual
   entries, not ambient Activity/app root. Keep Home plus the active primary in displayed navigation
   where needed by resolved NavDisplay Back handling. Returning Home does not remove retained History.
4. Selected tab tap is no-op; repeated cycles do not duplicate routes/reset selection/scroll or grow
   entry/store counts. Outgoing animation can retain lifecycle briefly; assert upstream shutdown after
   settling plus the agreed five-second timeout. Home's current Eagerly upstream must be addressed
   based on actual consumers, not assumed fixed by lifecycle UI collection alone.
5. First activation after hidden write/timeout marks/revalidates cached data before claiming success.
   Do not confuse eventual correct data or eventual zero collectors with correct first re-entry state.
6. Back dismisses year popup or creation sheet with its existing guard before primary navigation.
   At History root select Home, preserving History entry. At Home root delegate normal platform
   exit/back; never empty the root into blank UI. UiCatalog return restores selected primary and explicit
   catalog launch mode remains supported without production catalog actions.
7. Save only typed visited/selected navigation, validated scope/anchor and entry composition scroll.
   New owners recreate repository reads; no saved rows/jobs/keyboard/submission/changed creation draft.
   Android Activity/new saved-owner recreation must run through the actual shell. iOS proof exercises
   retained-controller/saveable scope, while a new process uses cold launch defaults.
8. Fresh launch without saved navigation starts Home; first History starts Day/today. Force-stop/
   relaunch is a persistence/default check, not proof of Android saved-state process restoration.
   If actual APIs cannot meet retained lifetime, re-plan with evidence rather than ambient VMs or
   another live editable input copy in shell/preferences.

## Creation and insets

- Shell owns one AddMovimientoSheetSession/store/route. Home/History add callbacks use it, preserving
  category-first flow, initial cancellation, save validation/retry/detail/keyboard/discard guard.
- Session stays alive through hide, releases once after onHidden, rejects reopening while closing;
  new opening creates a fresh draft. Disposal clears once. Bind each hidden callback to its originating
  session token; an old A callback cannot clear a newer Closing B. Do not read the latest token to
  relabel an old completion. Existing cleanup tests remain meaningful when ownership moves.
- Navigation taps cannot change underlying destination while modal is open. Saving returns to the same
  selection; outside-range creation changes global summary only. SQL invalidation owns refresh.
- One shell bar owns bottom safe inset and publishes occupied clearance; screens consume it without
  adding the same inset again. Creation sheet retains its own safe/IME policy. No filter modal exists.

## Test-first slices and checks

- [ ] Early actual adapter/decorator/Koin/saved-owner probe + fresh review before S3 transport.
- [ ] RED/GREEN bounded Home/History selection, selected-tab no-op, actual entry/VM/scroll retention,
      inert Search/Profile, History→Home→root Back and catalog return.
- [ ] RED/GREEN serialized complete primary state restores active route; actual recreated shell restores
      History scope/anchor/scroll while repository data reloads. Serializer alone is insufficient.
- [ ] RED/GREEN disposed/removed owners clear/cancel once, hidden Home/History reads stop after timeout,
      first re-entry state is marked/revalidated and catches hidden writes with distinct owners.
- [ ] Move sheet ownership and run unchanged regressions; create from both primaries, scrim/drag/Back
      guards, save failure/retry/close/reopen and delayed A hidden callback during Closing B.
- [ ] One bar, correct occupied clearance, inert reservations, selected semantics, physical popup/IME
      in creation sheet, supported Android/iPhone lifecycle evidence recorded separately.
- [ ] Full host regression, Native test compilation, Android build, device-test compilation, Detekt/ktfmt.

Early validate: `./gradlew :shared:testAndroidHostTest --tests '*HistorySavedOwnerHostTest'`.
Full focused: `./gradlew :shared:testAndroidHostTest --tests '*PrimaryNavigationTest' --tests '*DestinationSerializationTest' --tests '*TallybookAppShellLifecycleHostTest' --tests '*SheetSession*'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`
and `./gradlew :androidApp:assembleDebug :shared:compileAndroidDeviceTest`.
`:shared:connectedAndroidDeviceTest` requires one confirmed authorized physical Android device;
`:shared:assembleAndroidDeviceTest` assembles test APK. No simulator/emulator runtime.

## Completion and dependencies

Early slice/probe reviewed → S3 may choose transport. Full shell reviewed → Stage 6. Report missing
physical iPhone/unavailable runner precisely; compilation/host harness does not imply iPhone runtime.
No Search work is required to close this task.
