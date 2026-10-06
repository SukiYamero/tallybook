# Wave 3 / Stage 6 / Task 1 — Real History graph and closure

## Objective and context

Verify the complete [History contract](../../README.md), review the diff and obtain physical acceptance
without regressing the accepted creation flow. Requires reviewed Stage 5. Read FirstMovimientoIntegrationTest,
creation acceptance notes and backlog evidence boundaries. Search remains a separate later destination.

Skills: `android-dev`, `android-testing`, `android-debugging` for actual failure, `gradle-run`,
`verification-before-completion`, `requesting-code-review`.

## Files and responsibility

- Create `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/HistoryIntegrationTest.kt` with real
  SQLDelight repositories, initialized preferences and production History/form holders.
- Extend FirstMovimientoIntegrationTest only for relevant cross-screen regressions; share fixtures
  only when real common setup warrants it. No SearchIntegrationTest or Search projection/query is built.
- Finish History screen/navigation/device tests and precise task/progress evidence.
- Update canonical business/UI/component owners only when final implemented contracts require it;
  backlog only for actual unresolved/resolved items. After user acceptance compact delivered History
  into `docs/features/history.md`. Preserve deferred Search specs/reference images for its later plan;
  do not publish a Search feature document or delete useful future specs during History closure.
- Source changes outside History are limited to verified wave defects. Do not broaden into auth,
  dashboard redesign, dependency upgrades or unrelated baseline cleanup.

## Real-graph cases

Use file-backed SQLite for persistence, collectors active before writes and one shared virtual scheduler.
Close scopes/preferences factory/drivers/collectors and delete only test-owned fixtures.

- [x] Bootstrap actual categories/preferences; collect Home/History using the same database.
- [x] Create gasto/ingreso inside selected period through AddMovimientoViewModel; both observers react,
      exact period/global totals update, period count/groups agree and selection remains unchanged.
- [x] Create outside-range item; global changes, period rows/count/totals do not. No manual refresh.
- [x] Interleave foreign currencies with recent matching rows; History never mixes units. Check Home
      compatibility and fix its post-LIMIT filter seam only if a focused accepted regression fails.
- [x] Insert/remove previous/current year rows; available years update without resetting selection or
      removing selected retained year. Date/timezone resume follows accepted live-today policy.
- [x] Update category/unknown-label presentation without changing amounts; visible fallback/row title
      stays correct, no normalization/matching/index is required.
- [x] Save/read failures preserve draft/selection as relevant; retry cannot duplicate save or reset dates.
- [x] Reopen the same on-disk file to prove movement persistence and clean repository subscriptions.
- [ ] Actual shell state retains/restores route/selection/scroll under supported platform scope and keeps
      one creation owner. Search/Profile remain inert; Home search does not open History.

## Physical protocol

Identify one authorized physical Xiaomi/tablet/iPhone before each phase; no emulator/simulator or SDK/
signing edits. Resolve adb/SDK/JDK/Xcode on this host. For Android use its confirmed serial explicitly:
`adb -s <confirmed-serial> install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`, then
`adb -s <confirmed-serial> shell am start -n com.kurobello.tallybook/.MainActivity`.
Resolve the placeholder from actual inventory, never run it literally.

| Scenario | Concrete observation |
| --- | --- |
| Existing data/fresh entry | Existing rows survive; fresh History Day/today/current year and honest empty selected period |
| Period/year | Exact blocks, historical month, truncated current block, leap February, clamp/no-op chip rules and accepted year context |
| Reference | Context/header/count, three scopes/year, compact totals and description/date read-only rows; no search/filter/chart/breakdown/eye action |
| Global/period money | Separately labelled currency/totals; global stays visible while rows scroll; unavailable status is not false zero |
| Tab/Back | Repeated Home/History preserves selection/scroll; year popup/sheet first, History→Home, normal root exit; inert Search/Profile |
| Creation from History | One accepted sheet/validation/guard, successful write, correct period membership without reset |
| Android restore/cold launch | Actual Activity/saved-owner recreation restores small input/scroll; cold launch uses defaults and persisted rows |
| iOS retained controller/fresh launch | Actual supported saveable scene recreation retains small state; fresh process uses defaults with no persistence bridge claim |
| Resume | Midnight/year/timezone refresh updates live-today options while preserving valid historical selection |
| Accessibility/locales | Supported language text, TalkBack/VoiceOver order, complete period dates, retry/read-only sign/currency, large text and long descriptions |
| Layout | Light/dark portrait/tablet/compact landscape, creation keyboard, last row above sole bar and available Android nav modes |
| Bounded volume | Test-owned selected-month fixture, query/projection/scroll responsiveness and retained-memory observation |

Use a reproducible 10,000-row selected-month fixture plus excluded dates/currencies; record target/build
mode, distribution, query plan, read/projection timing/memory and physical scrolling. No production demo
seeding, brittle timing tests, advertised cap or silent LIMIT. All-history/typing/Search counts are later.
A demonstrated bounded-scale failure requires list/period-total re-plan before closure.

Force-stop/relaunch proves persistence/defaults, not saved process restoration. Use actual recreated
Activity/new-owner setup for Android restore and separate retained-controller saveable setup for iOS.
Do not erase existing user data for empty-install evidence; use a disposable/authorized clean install.
Run iPhone through Xcode on a confirmed physical target; Native compile/Android screenshots cannot prove
its lifecycle/popup/safe areas. Missing hardware stops only that phase; runner stall stays unconfirmed
until bounded diagnosis, with manual target evidence recorded separately. Do not promise new iOS
cross-process host persistence just to make acceptance wording pass.

## Final checks, review and closure

- [x] `./gradlew :shared:testAndroidHostTest`.
- [x] `./gradlew :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64` (compilation only).
- [x] `./gradlew detekt ktfmtCheck`.
- [x] `./gradlew :androidApp:assembleDebug :shared:compileAndroidDeviceTest
      :shared:assembleAndroidDeviceTest` (app/test APK build and fixture compilation).
- [ ] `:shared:connectedAndroidDeviceTest` executes on a confirmed awake/unlocked physical target;
      the prior interrupted host-launch attempt proves no assertion pass.
- [ ] Schema tasks only if measured schema/index change: `:shared:generateCommonMainTallybookDatabaseSchema`
      and `:shared:verifyCommonMainTallybookDatabaseMigration`; `:shared:verifySqlDelightMigration` available.
- [ ] Record tree/command/target/result/limitations without financial data, credentials or local signing
      material. Task evidence is current; historical S1.T2 report alone does not prove new History.
- [x] Independent whole-wave source reviewer fixes in-scope findings and verifies relevant checks.
- [ ] User tries this worktree's concrete History/creation scenarios on physical device and approves.
- [ ] With approval compact delivered History/canonical owners, retain useful deferred Search proposals,
      preserve unresolved MVP gaps, commit/merge/archive under documented repository workflow.

## Completion boundary

Source gate is approved at `e093eb4`: 258 host tests/58 suites, Android app/test APK builds, Native
production/test compilation, Detekt0 and ktfmtCheck pass. Real form/SQL/DataStore/Home/History and
closed-file persistence cases pass. Whole-wave review fixed supported wrong-type checkpoint fallback.
No schema/index change occurred, so migration/schema tasks are not applicable.

All actual UI/target assertions, Android MainActivity restoration, physical volume observations,
accessibility/visual acceptance and iPhone retained-controller proof remain unexecuted. Host owner
helpers and compiled fixtures do not close those checklists. Xiaomi remains locked/off; iPhone is
paired over local network with passcode/developer-image readiness pending. Production data untouched.

Accepted only with current checks/review and explicit physical product approval. Android/iPhone statuses
are separate; missing target or stalled execution remains precisely unconfirmed. If the user accepts
a narrower target boundary, preserve it in final feature/backlog documentation. Deferred Search tasks
are not History closure requirements, and no full MVP claim is made.
