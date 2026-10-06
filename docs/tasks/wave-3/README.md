# Wave 3 — History

> **For implementing agents:** use `subagent-driven-development`, one task per fresh context.
> Read this contract and the active task together. A fresh reviewer must review and correct each
> stage before its successor starts. Unchecked items are planned work, not implementation evidence.

**Goal:** browse persisted movimientos by Day/Week/Month and year context in History, with exact
period totals, retained selection/scroll and the accepted creation flow.
Search is a separate later destination; History has no text search or non-date filters.

**Architecture:** SQLDelight owns persisted data; the History ViewModel owns scope/anchor selection;
a pure projection builds groups and period totals from one bounded snapshot; pure Compose content
emits actions. The app shell owns Home/History navigation and the single creation sheet.

**Tech stack:** reuse repository versions; no upgrade is authorized. The catalog declares Kotlin
2.4.10, Compose Multiplatform 1.11.1, Material3 1.11.0-alpha07, Navigation 3 1.1.1, Lifecycle
2.11.0-beta01, SQLDelight 2.3.2, Koin 4.2.2, DataStore 1.2.1, Compose Unstyled 2.9.0, coroutines
1.9.0 and datetime 0.8.0. Resolve affected versions again when integrating them.

**Canonical owners:** [business rules](../../business-logic.md),
[UI architecture](../../ui/architecture.md), [components](../../ui/components.md),
[wording](../../wording.md), [methodology](../../README.md). This is the active execution plan;
Stage 1 updates canonical business/picker wording before implementation diverges from those documents.

**Execution tree:** `/Users/sukiyamero/.codex/worktrees/history-search/tallybook`, branch
`feat/history-search`, scope-revision baseline `cfae0954d5749b00138c27999d92ceb1cae3d761`.
Use the existing worktree. Planning checks do not establish a passing runtime baseline.

## Accepted scope and reference

The supplied [History image](references/history-screen.png) is the period/header/totals/read-only-list
reference. It confirms three scopes and a year selector. Its data, currencies, pixel dimensions,
category breakdown and eye control do not authorize production defaults or added functionality.
No demo data is seeded. Moneta at
`/Users/sukiyamero/Desktop/programacion/web/moneta/src/features/history/` remains read-only context;
its calendar weeks and fourth Year scope are not copied.

Included:

- Home and History as real retained primary destinations; Search/Profile stay inert reservations.
- Day/Week/Month, year context, exact date-grouped rows and exact selected-period totals.
- Selected-period balance/income/expense/count in the scrollable content; no fixed global balance
  or History-wide global-summary subscription. Home retains the global balance.
- Description-first History rows with date subtitle, category fallback/avatar and shared signed Money
  layout. Preserve Home's category-first public row API and visual behavior.
- Reactive SQL invalidation, explicit loading/empty/error/retry, retained selection/scroll and supported
  restoration; creation through the same shell-owned accepted sheet/session.
- Targeted shared row/tokens, shell/insets, localization/accessibility and separate physical targets.

Excluded: text search in History; any query/debounce/matching-count/index state; type/category filters;
Search UI, filter/session contracts, criteria, structural SQL, projection, DI or navigation; charts/
category breakdown; a fourth Year scope; balance visibility toggle; detail/edit/delete/undo; currency
conversion; auth/Drive/sync/export; persisted screen preferences; new modules/generic abstractions;
Paging/FTS or schema migration without demonstrated need. Auth + Drive remain required for the MVP.
The reviewed normalization at `e0d667b` stays intact for the existing category picker and future Search;
it does not add a History consumer or become a dependency of active History projection.

## Accepted temporal and presentation contract

S1.T1 publishes the durable rules in the canonical business document and aligns period-picker wording.
No period or Search-balance decision remains pending for this wave.

- Fresh History entry starts Day at today. Scope and anchor are the only editable selection inputs;
  range, month/year context, selected chips and options are derived.
- Week means month-bound blocks 1–7, 8–14, 15–21, 22–28 and 29–month end. Blocks never cross months.
  Current-month Day/Week options stop at today; historical months are complete. Current-month totals
  end at today; full calendar-month picker representation remains distinct from the History read range.
- Scope changes preserve anchor. Year/month transitions preserve a valid day, clamp month-end/leap
  dates and future anchors; a future month in the current year clamps to the current month.
- Allowed years are persisted distinct years for the primary currency plus current year, descending.
  Selected/restored past years remain selectable during this entry even if their last row disappears;
  metadata invalidation never resets selection. Fresh entries derive the menu again from persisted years.
- Capture Clock/timezone together on date refresh. If the anchor was previous today, follow new today;
  historical selection stays fixed unless it becomes future. No background midnight timer is needed.
- Period rows/count/totals describe all rows in the inclusive civil-date/currency range. Global balance
  describes all dates in that currency and is labelled separately, with its own loading/error/retry.
- Group by movement `fecha`; preserve `fecha DESC, created_at DESC, id DESC`. Rows are read-only.
  Description is the title when nonblank, category label/fallback otherwise; date is the subtitle.
  Date headers and row subtitles use localized civil dates. Category remains meaningful in avatar/
  accessibility content; no Search-style date-plus-category subtitle is required.

## Ownership and lifecycle

| Value | Single owner / lifetime |
| --- | --- |
| Movimientos/categories | SQLDelight via cold repositories |
| Primary currency | Existing preferences repository |
| History scope/anchor | History ViewModel for its retained navigation entry |
| Today | Injected Clock/TimeZoneProvider captured together on operation/resume |
| Groups, period count/totals, row text | Pure projection of the current range/category/presentation snapshot |
| Global summary | Existing all-date repository summary; separate labelled status |
| Scroll/year popup | Entry saveable composition state / transient local popup |
| Navigation and creation sheet/store | Shared app shell; typed Home/History routes and one creation session |
| Read/calculation failures | Durable UiState; retry preserves selection |

SQL filters currency/date before mapping. List and period totals use one bounded snapshot and checked
Money arithmetic; overflow is a typed failure, never a partial/zero/wrapped total. No separate period
summary query, all-history cache, normalized text index or business calculation in Compose is needed.
One immutable History UiState/Actions contract uses latest-only request switching; no effect stream is
needed for this read-only screen. Use the established injected dispatchers and cancellation boundary.

Lifecycle collectors plus `WhileSubscribed` use the agreed five-second stop timeout. Re-entry after
inactive timeout must mark/revalidate cached content before presenting it as current. Editable
selection survives independently of upstream replay; hidden writes are re-read on activation.

Restoration has an explicit platform boundary: Android supported Activity/saved-owner recreation;
iOS retained-controller/saveable scene recreation supported by the actual host. A newly launched iOS
process has no persisted host restoration bridge here and starts with normal cold-launch defaults.
A fresh app without saved navigation starts Home; first History entry starts Day/today. Save only typed
navigation, small validated scope/anchor input and scroll, never rows/jobs/focus/submission/sheet drafts.
No new DataStore/preferences/host persistence is authorized to extend iOS cross-process restoration.

## Active tasks and dependency order

| Stage | Active deliverable | Deferred work |
| --- | --- | --- |
| [1 — Contracts](stage-1/tasks.md) | S1.T1 accepted periods/canonical alignment; S1.T2 completed normalization retained | S1.T3 Search filters |
| [2 — Reads](stage-2/tasks.md) | S2.T1 bounded range/years/read validation | S2.T2 structural Search SQL |
| Early navigation gate | S5.T1 early adapter/resolution/decorated-entry/Koin/saved-owner probe before S3 transport | No Search route or model in probe |
| [3 — State](stage-3/tasks.md) | S3.T1 History projection, then S3.T2 History ViewModel | S3.T3 Search projection/state |
| [4 — UI](stage-4/tasks.md) | S4.T1 pure History screen and minimal shared row wrapper | S4.T2 Search/filter sheet |
| [5 — Shell](stage-5/tasks.md) | Remaining S5.T1 Home/History retention, Back, one sheet/bar | Search enablement/focus/modal handshake |
| [6 — Acceptance](stage-6/tasks.md) | S6.T1 Home/History graph, physical verification and History closure | Search integration/closure |

Each reviewed stage gates its successor. The early S5.T1 slice is explicitly executable before Stage 3
and reviewed before transport selection; the remaining full-shell slice follows reviewed Stage 4.
Deferred task files retain useful future proposals, but their checklists/dependencies are inactive and
must be rebased into a separately authorized Search plan. They do not gate History completion.

## External API gate and verification

Use [repository methodology](../../README.md) for version-sensitive APIs: actual resolved version,
primary documentation/source, complete integration check and target-specific execution. The scratch
navigation audit identifies the matching official `lifecycle-viewmodel-navigation3` adapter as absent;
S5.T1 owns adding it under the existing Lifecycle version, resolving the graph again and exercising
real decorators → entry owners → Koin → SavedStateHandle with fresh restored registry/store owners.
Source/API feasibility and serializer-only tests do not close that gate. No Koin navigation framework
or general app store is needed. Full NavDisplay/scroll/Back/device evidence remains in Stage 5/6.

Primary research anchors: [KMP Navigation 3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html),
[KMP ViewModel ownership](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html),
[SQLDelight observation](https://sqldelight.github.io/sqldelight/latest/2.x/extensions/coroutines-extensions/index.html),
[KMP lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html).
Revalidate actual resolved artifacts before integration; these links authorize no upgrades.

| Requirement | Task owner | Meaningful evidence |
| --- | --- | --- |
| Three scopes, blocks, year/today/restored boundaries | S1.T1 | Fixed civil-date ranges/transitions, picker regressions |
| Category search normalization preserved | Completed S1.T2 | Existing reviewed `e0d667b` evidence; current regression suite at closure |
| Currency/date/order/year/invalidation/read corruption | S2.T1 | Real SQLite collectors before writes, positive amount/year validation |
| Exact same-snapshot rows/count/totals and overflow | S3.T1 | Pure projection and valid extreme/subtotal failure tests |
| Latest state, retry, presentation updates, resume | S3.T2 | Shared virtual scheduler, late request emissions, first re-entry state |
| Global balance separate/visible; History rows/Home preserved | S4.T1 | Screen semantics/previews/row regressions, physical reference comparison |
| Actual entry/Koin/saved-input ownership | Early S5.T1 | Real adapter + fresh-owner registry restoration probe |
| Back/tab/scroll/store lifetime, one bar/sheet | Remaining S5.T1 | Full-shell/physical lifecycle and creation-session tests |
| Creation → persisted data → Home/History | S6.T1 | File-backed production graph and separate confirmed physical targets |
| Locales/accessibility/insets/selected-month scale | S6.T1 | Reproducible target/build/fixture observations and physical review |

Execution checks (not run by this planning revision):

- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64`
  (Native test compilation only; no simulator runtime).
- `./gradlew detekt ktfmtCheck`
- `./gradlew :androidApp:assembleDebug :shared:compileAndroidDeviceTest`
- `:shared:assembleAndroidDeviceTest` assembles the test APK; `:shared:connectedAndroidDeviceTest`
  executes it only against a confirmed authorized physical Android target.

Follow `gradle-run`; discover exact device/SQLDelight tasks when needed. Confirm one authorized physical
Xiaomi/tablet/iPhone before device work, resolve tool paths and use its serial without modifying
local.properties/signing/project.pbxproj. Never run a simulator/emulator. Missing hardware gates only
that phase; report execution/compile/host evidence separately. Preserve the instrumented-runner stall
as unconfirmed until diagnosed. User physical acceptance precedes merge.

## Focused risks and re-plan triggers

S2.T1 owns positive stored amount validation (Money stays signed) and scalar year metadata validation;
year metadata is not a full row corruption scan. Preserve current created_at TEXT secondary order
without claiming normalized instant sorting. S3.T2/S4.T1 explicitly update the localized unknown-category
presentation input so visible fallback stays current without requiring a database emission.
S5.T1 keeps originating creation-session identity on delayed hidden callbacks and protects re-entry
readiness. Search filter token/availability rulings remain in deferred files only.

Profile a reproducible 10,000-row selected-month fixture with excluded dates/currencies, query plan,
read/projection timing, retained-memory observation and physical scroll responsiveness; no brittle
unit timing assertion or advertised cap. All-history Search typing/count profiling is deferred.
Re-plan bounded list/totals together if volume is inadequate; no silent LIMIT/partial financial total.
Home's post-LIMIT currency filtering is an existing seam; S6 checks compatibility and owns only a
necessary focused fix/regression if it violates the accepted behavior. Do not redesign Home.

Re-plan on changed scope/period policy, incompatible actual retained-entry APIs, needed schema changes
or demonstrated scale failure. Backlog simulator/signing wording is stale; AGENTS physical-only and
shared TEAM_ID constraints control execution. Auth/Drive, charts and Search remain later work.

## Closure

- [ ] Canonical business/picker/UI owners agree with accepted History behavior.
- [ ] Active tasks and current-tree checks/reviews are complete; deferred Search is not a closure gate.
- [ ] Separate Android and iPhone runtime/restoration limitations are explicit.
- [ ] User accepts History and creation on a physical device before merge.
- [ ] Compact History into `docs/features/history.md`; preserve future Search specs for its later plan,
      update canonical owners/backlog as applicable and archive the worktree through the repo workflow.
