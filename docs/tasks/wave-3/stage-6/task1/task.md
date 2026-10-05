# Wave 3 / Stage 6 / Task 1 — Real-graph regression and closure

## Objective and context

Verify the full [wave contract](../../README.md), review its diff and obtain physical acceptance without
regressing the accepted creation flow. Requires Stage 5 reviewed. Read the existing
`FirstMovimientoIntegrationTest`, creation acceptance notes and backlog evidence boundaries.

Skills: `android-dev`, `android-testing`, `android-debugging` if a real failure occurs,
`gradle-run`, `verification-before-completion`, `requesting-code-review`.

## Files and responsibility

- Create `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/HistoryIntegrationTest.kt` using
  real SQLDelight repositories, initialized preferences and production History/form state holders.
- Create adjacent `SearchIntegrationTest.kt` for real structural filters plus normalized text,
  draft isolation and creation invalidation. Reuse fixtures only where they are real shared setup.
- Extend existing `FirstMovimientoIntegrationTest.kt` only for a relevant cross-screen regression.
- Complete History/Search screen device tests in Stage 4 and navigation device tests in Stage 5.
- Update task checkboxes with exact current evidence, canonical `docs/business-logic.md`,
  `docs/ui/architecture.md`/`components.md` when their contracts change, and `docs/backlog.md` only for
  actual unresolved/resolved items. After user acceptance, create compact `docs/features/history.md`
  and `docs/features/search.md`; keep canonical domain/UI rules in their existing owners.
- Source changes outside History are limited to verified wave defects; do not broaden into auth,
  dashboard redesign, coroutine upgrades or unrelated baseline cleanup.

## Real-graph integration cases

Use a file-backed SQLite test for persistence, not only a fresh wrapper around an in-memory driver.
Use active collectors before writes and the same virtual scheduler for all test dispatchers.

- [ ] Bootstrap actual categories/preferences and collect Home + History against the same database.
- [ ] Create a gasto and ingreso inside the selected period through AddMovimientoViewModel: both
      collectors react, period/overall totals are exact and query/selection stay unchanged.
- [ ] Create an item outside the active range: overall balance changes; period rows/totals do not.
- [ ] Search a description/category, create matching and nonmatching items and clear the query:
      filtering/counts change correctly while the period summary remains independent of search.
- [ ] Search all/date/type/multiple-category predicates across exact parent/child IDs. Open/edit a
      draft and verify no query/results change; Apply changes all dimensions once, cancel changes none,
      Clear preserves query and header count settles without appearing inside Aplicar.
- [ ] From Home's search entry, retained Search focuses once; normal tab/restoration preserves filters
      without focus loops. Search and History inputs remain independent.
- [ ] Insert foreign-currency rows interleaved with recent matching rows. History never mixes units;
      test Home's observable consistency and address its post-LIMIT filtering only if this fails the
      accepted regression contract, preserving a focused fix and regression test.
- [ ] Introduce a previous/current/new year, observe metadata and confirm it does not reset selection.
- [ ] Close collectors, scopes, preferences factory and database driver; reopen the same on-disk file
      and verify persistence plus clean repository subscriptions. Delete only test-owned fixtures.
- [ ] Verify save/read failure retains draft or filters as applicable; retry cannot duplicate a save.

## Physical verification protocol

Identify one authorized physical Xiaomi, Android tablet or iPhone before each device phase. Do not
use emulators/simulators or modify local SDK/signing files. Discover adb/SDK/JDK/Xcode on this host.
When Android is selected, use its confirmed serial explicitly for installation and launch:
`adb -s <confirmed-serial> install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` followed
by `adb -s <confirmed-serial> shell am start -n com.kurobello.tallybook/.MainActivity`.
The placeholder is resolved from device inventory; never run it as a literal command.

| Scenario | Concrete observation |
| --- | --- |
| Existing-data upgrade and empty install | Existing rows survive; fresh History has current-year context and an honest empty-period state |
| Period selection | Accepted exact boundary dates, historical months, leap February and year changes produce the expected rows |
| Search | Case/accent/clear behavior, literal punctuation, matching indicator and no-results copy agree with the visible row text |
| Search filter draft | All six presets, custom cross-year dates, type/category selection, Apply/Clear/cancel and reopen have their defined semantics |
| Search guide/CTA | Description-first date/category rows, active chips and header count match the guide; primary footer says Aplicar without quantity |
| Financial summary | Global and period labels/currency are distinct; query changes do not change period totals |
| Tab/back retention | Repeated tab switches retain filters/query/scroll; popup, IME, sheet and root Back follow their defined hierarchy |
| Creation from History | One sheet, accepted validation/discard protection, successful write and correct filtered result without reset |
| Process recreation/relaunch | Saveable recreation restores agreed inputs; ordinary cold launch uses its defined defaults; movements remain persisted |
| Date/timezone resume | On next resume, today/options refresh without unexpected changes to historical selection |
| Accessibility/locales | Supported translations, TalkBack/VoiceOver order, clear/retry semantics, large text and ambiguous currency symbols are understandable |
| Layout | Light/dark portrait, compact landscape, keyboard open, last item above bottom bar and available Android navigation modes work |
| Volume | Use a test-owned dataset to inspect selected-month query, typing and scroll responsiveness; no production demo seeding |

Also inspect All history/This year with the 10,000-row stress fixture. Do not mistake LazyColumn for
bounded database memory or announce a count from a truncated page. Failure triggers the wave's
explicit paging/count/normalization re-plan gate.

Force-stop/relaunch demonstrates persistence and cold-launch behavior, not Android saved-state process
restoration. Use an actual recreation/restore scenario for that contract and report its setup.
Do not erase a user's existing data to obtain an empty-install scenario; use an explicitly disposable
test install/device or an authorized clean installation.

Run the corresponding iPhone flow through Xcode on the confirmed physical target. Native compilation,
host tests and Android screenshots cannot prove iOS keyboard, lifecycle, popup or safe-area behavior.
If no compatible device is connected, stop only that phase and request the required device. If the
runner stalls, diagnose the runner with bounded evidence and retain manual target evidence separately.

## Final checks and review

- [ ] Run `./gradlew :shared:testAndroidHostTest`.
- [ ] Run `./gradlew :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64`.
- [ ] Run `./gradlew detekt ktfmtCheck`.
- [ ] Run `./gradlew :androidApp:assembleDebug`; run discovered device compile/execution tasks on the
      confirmed target when available. Verify migrations only if this wave changed schema.
- [ ] Record target, commit/tree, command, result and blocked checks without exposing financial data,
      descriptions/search terms, credentials or local signing material in logs/screenshots.
- [ ] Fresh reviewer fixes wave findings and verifies relevant checks before requesting product review.
- [ ] Ask the user to try the actual worktree and concrete History/creation scenarios on device.
- [ ] With user approval, compact active specs, preserve unresolved MVP gaps, commit/merge and archive
      the feature worktree through the repository's documented workflow.

## Completion / blockers

The wave is functionally accepted only with current checks, review and explicit physical product
approval. Android and iPhone evidence are separate statuses. Missing iPhone or instrumented execution
is reported precisely; it cannot be converted into a claim that both platforms are runtime-verified.
If the user accepts a narrower target boundary, preserve that limitation in the feature/backlog docs.
