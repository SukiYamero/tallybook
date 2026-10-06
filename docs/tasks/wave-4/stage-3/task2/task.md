# Stage 3 / Task 2 — Navigation, modal lifetime and acceptance

Read [Wave 4](../../README.md) and all preceding contracts. Planning only. Depends on pure UI/popup
integration; coverage A10/A11/A13/A14 and final acceptance across A1–A9/A12.

## Goal, files and ownership

Wire a retained Search entry with the actual decorated entry SavedStateHandle. Enable both agreed
entry points and coordinate filter/creation modalities without duplicating feature values.

Own existing `core/ui/navigation/{Destinations,PrimaryNavigationState,TallybookNavHost,
TallybookBottomNavigation}.kt`, additive Home search callbacks through
`feature/home/{HomeScreen,HomeContent,HomeOuterShell}.kt`, and any required route signatures.

Proposed files:

- `feature/search/ui/SearchRoute.kt`: DI/lifecycle/localized fallback and entry focus intent wiring.
- `core/ui/navigation/PrimaryModalCoordinator.kt`: shell-owned fixed creation/filter arbitration.
- `shared/src/androidDeviceTest/kotlin/.../core/ui/navigation/SearchNavigationTest.kt`:
  actual entry, saved-owner, IME/modal and creation integration.
- Test-only `feature/search/ui/SearchLoadingPreviewActivity.kt` if delayed-read physical evidence
  cannot be driven deterministically by the existing runner; reuse the existing preview-Activity
  pattern, without adding a production development screen or seeding production data.
- Extend existing common `PrimaryNavigationTest.kt` and relevant saved navigation/DI regressions.

PrimaryEntries' official decorators, parent extras and stable content keys remain the established
owner boundary; change them only if a concrete Search integration requires it. Stage 2 already owns
Search Koin registration. Coordinate canonical docs/catalog changes through the orchestrator.

## Navigation and focus contract

Add serializable Search Destination and stable `primary-search` content key. Extend the existing
typed visited/selected snapshot with Search visitation using its existing pattern; no parallel Search
back stack or alternate navigation library. Home remains retained, visited History/Search retain
entry owners, selection and scroll; catalog Back returns to the prior selected primary.

Search root Back selects Home; Home exit behavior and History Back stay unchanged. Search tab gets
selected/interactive semantics. Home's search surface becomes a button callback through its existing
content boundary, opens retained Search and does not edit/reset its query or filters.

Home-to-Search emits one transient shell focus-request token. SearchRoute first reveals the lazy
header/input (scrolling to it if needed), waits for composition and requests focus. Acknowledge only
successful focus; older acknowledgment cannot clear a newer request. Cancel a pending intent if Search
becomes inactive before completion. This explicit Home entry overrides retained result scroll only
to reveal the field; ordinary Search-tab return preserves its position.
Do not serialize this intent, replay it after restore, drive focus on every RESUMED event or add an
input copy in Home. Ordinary tab selection never forces keyboard focus. Compose owns FocusRequester,
keyboard and LazyListState; ViewModel contains no runtime UI objects.

SearchRoute follows HistoryRoute's RESUMED collection/revalidation, supplying the localized unknown
category label and resumed civil date context before collecting. Read/presentation changes use Stage 2
identity gates. The actual saved entry handle must reach SearchViewModel through Koin, not a new
ad-hoc handle. New owner restores applied input/scroll and Hidden; retained owner preserves its draft.

## Modal arbitration contract

PrimaryModalCoordinator receives the existing AddMovimientoSheetSession and registers one typed
filter controller from the currently active SearchRoute. Controller functions delegate to synchronous
SearchActions state; the coordinator stores callbacks/owner identity only, no filter values or copied
busy boolean. Its busy predicate derives from creation hasBeenOpened or active filter session.

`tryOpenCreation()` rejects while filters are Editing/Closing; `tryOpenFilters()` rejects while
creation is open/closing and delegates to Search's readiness guard. Navigation/catalog requests reject
while either is active. An already-open filter session rejects another open. No automatic switching,
queued creation after hide or unsaved-filter confirmation is introduced.

Register the controller with a composition lifecycle effect keyed to the actual entry owner. An older
detach cannot remove a newer registration. Detach for ordinary Activity recreation does not cancel a
retained ViewModel's draft; a truly cleared owner releases its jobs/session normally. While selected
Search is attaching a controller, modal/navigation actions must not race its authoritative availability.
Closing controls/data remain owned by the Search ViewModel; sheet motion/popup cleanup remains Compose.

All callbacks carry originating filter IDs or creation tokens. Filters stay busy through Closing until
their matching onHidden. AddMovimientoSheetSession's existing owner/store lifetime and token cleanup
remain single-sourced; do not compose another AddMovimientoSheetRoute inside Search. Closing/disposing
the filter route must release viewport scrims/keyboard work without clearing another session.

Back order is active date/year popup, filter sheet cancellation, then Search root; creation's own
dismiss/dirty-form behavior remains unchanged. Scrim/drag cancel the draft before hidden cleanup.
Opening filters hides IME; hidden cleanup returns accessibility focus to its button and does not
reopen the text keyboard. New movements appear through the existing observation, without append/reset.

## Targeted integration slices

- [ ] Common navigation states cover fresh Search, Home/History/Search visits, repeated selections,
      root Back, catalog Back, retention ordering/stable keys and restored visited states.
- [ ] Actual Koin/decorated graph verifies Search uses its real entry handle/owner; switch tabs and
      assert same ViewModel/input/scroll. From a deeply scrolled retained Search, Home entry reveals
      the initially uncomposed field and focuses exactly once; tab/restore does not. A cancelled focus
      intent cannot focus on a later ordinary tab return.
- [ ] Saved Activity recreation/restoration rebuilds applied inputs and scroll without result/session
      serialization; retained-owner recreation keeps draft; genuinely new owner starts Hidden.
- [ ] Lifecycle stops/resumes share for five seconds, preserve current snapshots including empty,
      re-resolve relative presets and never display stale counts as current before revalidation.
- [ ] Apply/cancel/double-tap, rapid creation request while Closing, navigation during hide, delayed old
      hidden callback and recreated controller registration; assert one modal/store and no overlap.
- [ ] IME Back, popup/year Back, scrim/drag, fixed footer and safe-area clearance are verified at the
      complete shell→route→modal boundary, not inferred from isolated component tests.
- [ ] Create a matching/nonmatching movement through the one shared form; assert SQL-updated results
      and settled count, preserved filters/query and no Home/History creation regression.

## Verification and final acceptance

Use the gradle-run compact-output workflow. Discover current device-test build/runner and dependency
inspection configurations from this wrapper before invoking them; old proposal task names are not
evidence. Run narrow navigation/state tests first, then one host regression for the shared shell change:

`./gradlew :shared:testAndroidHostTest --tests '*PrimaryNavigationTest' --tests '*Search*'`

`./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`

These Native commands compile only; do not run simulator/emulator targets. Confirm resolved versions,
official source and complete restoration/modal integration as required by the overview/API gate.
Do not claim a green build proves launch, actual iPhone interaction or runner assertions.

Identify exactly one compatible authorized physical device for each runtime phase, resolve actual
adb/SDK/JDK/Xcode paths and use its explicit serial/destination. Do not edit local.properties, signing
TEAM_ID or project.pbxproj to make the check run. Android installation uses the generated APK and the
existing MainActivity; iPhone launch uses the existing Xcode project/team. If no hardware is available,
stop that step and report the exact outstanding acceptance; never substitute a simulator.

Physical main-flow smoke (3–5 minutes) covers first slow-read skeleton, fast/no-flash readiness, text
search, category/date/type Apply, discard, clear/chip removal, empty state, tab return and shared creation.
Add only focused popup/IME/large-font/58-category/footer checks for the changed boundary. Android
saved-owner checks need Android runtime; iPhone popup/keyboard/safe-area behavior needs iPhone runtime.
TalkBack selection/removal/loading announcements are a focused accessibility acceptance check.

If instrumentation stalls, stop the unchanged run, diagnose once for at most five minutes and use a
test-only delayed-read preview/manual check where meaningful. Record assertions that did not execute
as unconfirmed; do not disable cleanup, skip assertions or merely extend timeouts. The existing History
runner limitation makes this fallback relevant; it is not permission to call a stalled suite passing.

Perform the agreed bounded volume probe in a disposable test database, with rows/metrics defined in
the overview and user-confirmed before the run. One All query, one This year query, one sparse filter
and rapid text cancellation are enough initially. Include real scroll/stable-key checkpoints, not
every row/screen. Record timing/allocation/subscription observations without claiming a maximum size.
Confirmed blocking work/memory changes require re-planning paging/normalized storage/exact counts.

Finish with one independent focused review of the final feature diff, correctness, state ownership,
duplication and simpler alternatives. Fix verified findings and rerun affected checks only. Report
changed behavior, checks actually run, target limits and the physical acceptance handoff. User
acceptance and main integration are required before DONE; compact the plan into feature documentation
only at closure. Implementation authorization alone does not authorize merge.
