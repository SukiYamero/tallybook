# Wave 3 — Movement consultation: History and Search

> **For implementing agents:** use `subagent-driven-development`, one task per fresh context.
> Read this contract and the task file together. A fresh reviewer must review and correct each stage
> before the next stage starts. Unchecked items are planned work, not completed implementation.

**Goal:** browse persisted movimientos in History and find them in Search with text, date, type and
category filters, retaining screen state and preserving the accepted creation flow.

**Architecture:** SQLDelight owns persisted data; separate shared History/Search ViewModels own
screen inputs; pure projections build display data; stateless Compose content emits actions. The
app shell owns primary navigation and the single creation sheet. Search uses a temporary filter
draft committed atomically through Apply.

**Tech stack:** reuse declared/resolved repository versions. The planning baseline declares Kotlin
2.4.10, Compose Multiplatform 1.11.1, Material3 1.11.0-alpha07, Navigation 3 1.1.1, Lifecycle
2.11.0-beta01, SQLDelight 2.3.2, Koin 4.2.2, DataStore 1.2.1, Compose Unstyled 2.9.0, coroutines
1.9.0 and datetime 0.8.0. No upgrade is authorized by this wave; resolve versions at execution.

**Spec:** this execution contract, [business rules](../../business-logic.md),
[UI architecture](../../ui/architecture.md), [component inventory](../../ui/components.md),
[wording](../../wording.md) and [methodology](../../README.md).

**Planning baseline:** clean `main` at `e32ac3c1593139882b63321a3524fdf0708138f5`, ahead of its remote.
This documentation task does not push existing commits or establish a fresh passing runtime baseline.

## Approved scope and reference

The user confirmed both History and Search in this wave. The supplied Search images are approximate
visual references, preserved as [Search screen](references/search-screen.png) and
[filter sheet](references/search-filters.png). Their illustrative data/categories/device dimensions
are not production defaults.

The user's explicit CTA instruction overrides the image: **Aplicar**, without a result count in
visible text or its accessibility label. The count stays below the Search screen title.
The user also confirmed transactional filter editing: **Apply commits; dismissal discards changes**.

The actual Moneta reference is at `/Users/sukiyamero/Desktop/programacion/web/moneta/src/features/`.
Read `history/` and `search/`, especially their README, screens, filter hooks, date builders and tests.
Moneta is read-only context. Its live-filter behavior is deliberately not copied; its row layout and
calendar-week behavior also differ from the approved Tallybook guides in ways described below.

Included:

- Real History and Search primary destinations alongside Home.
- History Day/Week/Month context, year menu, date-grouped rows and exact period totals.
- Search text plus All history / Last 7 days / Last 30 days / This month / This year / Custom dates.
- Search Todos/Gastos/Ingresos and multi-select categories from the real persisted catalog.
- Active filter chips, applied/draft state, clear/apply/dismiss behavior and result counts.
- Reactive updates, explicit error/retry states, retained inputs/scroll and restoration.
- Creation from any enabled primary destination through the same accepted sheet/session.
- Targeted shared-row/token and shell/inset changes required by these actual consumers.
- Supported locale resources, accessibility and separate Android/iPhone verification evidence.

Excluded: detail/edit/delete/undo; category management; charts/category breakdown; amount/date/ID
text search; currency conversion; login/Drive/sync; export; persisting filters/query in DataStore;
new Paging/FTS libraries or migrations without measured need. This remains a local increment, not a
completed MVP: Google Sign-In + Drive are required by the canonical business rules.

## Decisions pending before dependent implementation

The user will provide more detail about History periods. Do not treat the following proposals as
approval or infer calendar-week behavior from the Search screenshots.

| Decision | Proposed default | Required resolution |
| --- | --- | --- |
| History Week | Preserve current month-bound blocks: 1–7, 8–14, etc.; show explicit dates | User-supplied detail determines blocks versus calendar weeks, cross-month behavior and Monday/Sunday policy |
| History Year scope | Day/Week/Month now; year menu changes their context | Decide whether Year becomes a fourth scope; Search This year does not cover arbitrary past annual browsing |
| Overall balance in Search | Follow the supplied header/count composition | Reconcile the durable “overall balance always visible” rule with this screen; no large unsolicited card or silent claim of compliance |

`docs/tasks/period-picker.md` says calendar week, but its implementation and tests assert blocks kept
inside a month. Stage 1 must align canonical wording and tests with the accepted rule. Moneta uses
calendar weeks and has four scopes; do not claim full behavioral parity.
The stage may finish independent work while a decision is pending; it cannot close its gate or start
dependent period/UI behavior until that decision is settled.

## Proposed History contract

Stage 1 publishes accepted domain rules in `docs/business-logic.md`; task files reference that owner
instead of creating permanent duplicate rules. These are planning inputs pending period confirmation.

- First entry: Day at today, empty query. One scope and anchor are editable; selected period,
  focused month/year and available options are derived.
- Scope changes preserve anchor. Month/year changes preserve a valid day and clamp month-end/leap
  dates and future anchors; a current-year future month clamps to the current month.
- Allowed years: distinct persisted years for the primary currency plus current year, descending.
  A selected/restored year stays available during the entry lifetime even if its last row disappears.
  A fresh entry uses persisted metadata again; years never reset selection during invalidation.
- Proposed current-month day/week options end at today; historical months are complete. Current
  month totals end at today. Exact week and annual behavior follows the pending decision.
- Changing period preserves query; clear query restores the full period list.
- Period totals describe all rows in the selected date/currency range, regardless of text search.
  Overall balance describes all dates in that currency; label it separately from period totals.
- Group by the civil movement `fecha`. Preserve storage order `fecha DESC, created_at DESC, id DESC`.
  No rows are clickable until detail exists.
- Search descriptions and the category label actually rendered, including supplied unknown-category
  fallback. Matching is shared with Search; it is not amount/date/ID/type matching.

## Confirmed Search editing and proposed filter rules

The Apply/dismiss contract and count-free CTA are confirmed. Stage 1 validates the remaining defaults
against canonical product requirements and publishes their accepted form before implementation.

- Fresh Search: All history, all types, no selected category IDs, empty text. Search and History have
  independent inputs; changing one never silently constrains the other.
- Opening filters copies applied non-text filters into a draft. Controls change only that draft;
  chips/results behind the sheet keep the applied state.
- Aplicar validates, atomically commits all draft dimensions and closes. Zero results and an unchanged
  valid draft still allow Apply. No count-preview query or live-results CTA is necessary.
- Back/scrim/drag dismissal drops the draft. Limpiar resets draft date/type/categories and custom
  bounds to today, preserving text query; the reset commits only through Apply.
- Apply/cancel ends editing once, then enters Closing with disabled render data until `onHidden`.
  Reject reopen during Closing; only Hidden allows global creation. Render data retained for animation
  is neither an applied filter copy nor an editable draft.
- Clear input removes only text. Removing a screen chip immediately removes only its applied
  dimension. The next draft copies that latest applied state.
- Inclusive presets: Last 7 = today minus 6 through today; Last 30 = today minus 29 through today;
  This month/year use full calendar boundaries as in Moneta. All history has no date predicate,
  represented by an explicit optional range, not fabricated minimum/maximum dates.
- Custom uses required valid civil dates. Proposed Moneta-compatible policy swaps reversed bounds
  before display/apply; proposed max custom date is today, matching creation. Cross-year custom
  labels include the year, even though the current compact date chip omits it.
- Date, type and category dimensions combine with AND; multiple categories combine with OR. No
  selected categories means all. Match exact stable category IDs; a parent does not automatically
  include descendants. Categories do not determine the movement type.
- Render the real 58-category catalog, including retained children, with deterministic ordering and
  a scrollable/filter-group presentation. Do not copy the image's older flat category list. Missing
  selected IDs use the same visible fallback and are not invisibly cleared during a live refresh.
- Text matches description or currently displayed category name as a literal normalized substring.
  Keep raw input intact; trim outer whitespace, lowercase and fold supported Latin accents, including
  decomposed forms. Preserve ñ versus n to retain current Tallybook category-search semantics; this
  is an explicit difference from Moneta's broader Unicode fold.
- Empty text matches all rows; category-picker empty text still means no suggestions. `%` and `_`
  are literal; no token language, ranking, parent expansion or SQL wildcard matching.
- Input updates immediately; nonempty matching settles after 250 ms; clear is immediate. A pending
  query cannot show/announce an older count as current. Only settled current results publish count.
- Search row: description-first title when nonblank, category fallback otherwise; subtitle has date
  and category; signed amount and unambiguous currency; read-only semantics. Preserve Home's row
  presentation rather than silently changing it to the Search variant.
- Filter active dot/chips derive from applied non-text filters. Each removable chip has an accessible
  removal label; active state does not rely only on color or a dot.

## Ownership and strategy

| Value | Single owner / lifetime |
| --- | --- |
| Movimientos/categories | SQLDelight through cold repositories; persisted and shared across consumers |
| Primary currency | Existing preferences repository; no copied screen/navigation preference |
| History selection and query | History ViewModel for its retained navigation entry |
| Applied Search filters and query | Search ViewModel for its separate retained entry |
| Search filter session | Hidden/Editing/Closing owned by Search state; closing render snapshot until onHidden |
| Today | ViewModel, injected Clock + TimeZoneProvider captured together on operation/resume |
| Row data, groups, matching/counts and period totals | Pure projection of the current request snapshot; never persisted/optimistically appended |
| Scroll and focus | Saved composition state by entry; focus/keyboard are transient |
| Navigation and creation sheet/store | Shared app shell; typed routes and exactly one creation session |
| Read/calculation failures | Durable UiState; retry preserves inputs |

SQL filters currency/date/type/category before domain mapping. Kotlin matches supported-locale text
on the resulting snapshot using the shared normalizer, with normalized fields built once per data/
category update. No query-per-keystroke, duplicated all-history cache or Compose business calculations.

History derives list and exact totals from the same bounded snapshot already required for display,
using checked Money arithmetic. This avoids independently emitted list/total revisions. The existing
all-date SQL summary continues to own overall balance. Overflow has an explicit typed projection
failure; it never becomes a wrapped number, partial total or zero.

Search All history is not date-bounded. LazyColumn limits composition, not database materialization.
Stage 2/6 profile both selected-month and All history 10,000-row stress fixtures plus exclusions.
These are stress cases, not an advertised data limit. If inadequate, re-plan paging, accent matching
and exact global result count together before closure. Never add a silent LIMIT or guess the count.

One immutable UiState/Actions contract per screen; latest-only request switching and deliberate
StateFlow sharing. Repositories use injected dispatchers and preserve cancellation. Add buffered
Channel effects only for an actual one-shot need, not a speculative generic event layer.

Navigation retains actual per-primary entry stores/saveable state. Switching tabs does not destroy
History/Search, duplicate routes or accidentally share Activity-owned ViewModels. Root Back selects
Home from either screen; transient surfaces get first dismissal; Home delegates platform exit.
The filter sheet and global create sheet are coordinated and cannot overlap. The accepted creation
session survives hide animation, releases after hidden and starts a fresh draft on a new session.
Retained Home currently shares eagerly; Stage 5 adjusts sharing after checking synchronous consumers
and proves inactive screen subscriptions stop after the agreed timeout.

## Stages and dependencies

| Stage | Deliverable and order |
| --- | --- |
| [1 — Contracts and pure rules](stage-1/tasks.md) | task1 periods + task2 normalization may run in parallel; task3 filters follows task1 for shared range/document ownership |
| [2 — Reactive consultation reads](stage-2/tasks.md) | task1 range/years then task2 structural Search reads; one repository/schema owner |
| [3 — State and projections](stage-3/tasks.md) | task1 projection then task2 History / task3 Search with separate feature ownership |
| [4 — Pure screen composition](stage-4/tasks.md) | task1 History/shared row migration then task2 Search/filter sheet |
| [5 — Shared shell/navigation](stage-5/tasks.md) | task1 connects Home/History/Search and one creation flow |
| [6 — Integration/physical acceptance](stage-6/tasks.md) | task1 real graph, target evidence, full-wave review and closure |

## Execution methodology

1. Re-read baseline/AGENTS at execution; create one feature worktree `feat/history-search`. No
   implementation or worktree creation is performed by this documentation task.
2. Use the global `task-planner` and `operator-orchestrator` roles documented in `docs/README.md`.
   Orchestrator owns cross-task decisions; fresh implementer context per task and fresh reviewer per
   stage, with bounded read-only audits when independent discovery is useful.
3. RED -> GREEN -> REFACTOR: fail for the actual missing behavior, implement the smallest contract,
   then simplify. Tests are grouped by meaningful seams, not one file per assertion.
4. Reuse error boundaries, money, injected clock/timezone, components and session lifecycle. No
   use-case layer for a single consumer, new module, generic mapper framework or redundant flags.
5. Shared rules stay in commonMain; domain has no Compose/platform imports. English identifiers/docs
   retain existing Spanish domain vocabulary. Do not migrate all Home packages for symmetry.
6. Follow gradle-run before wrapper invocation. Run narrow checks before full regression and retain
   failure fingerprints; do not suppress warnings/skip tests or repeat unchanged failed checks.
7. Review contracts, simplification, concurrency, accessibility and ownership before physical product
   acceptance. A detailed plan does not replace current-tree checks or guarantee flawless code.

## External API evidence

Before version-sensitive integration, resolve the dependency version, verify primary docs/source for
that version, test the complete connection and execute affected targets. Current references guide
research; their sample dependency versions do not authorize upgrades:

- [KMP Navigation 3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [KMP ViewModel/entry ownership](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html)
- [SQLDelight coroutine queries](https://sqldelight.github.io/sqldelight/latest/2.x/extensions/coroutines-extensions/index.html)
- [KMP lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html)

Gate entry saveable-state/ViewModel-store decorators, typed back-stack serializer and Koin ownership
as one integration. Add matching KMP lifecycle-navigation coordinates only if resolved artifacts
require them; no Android-only artifacts in commonMain. Also verify variable-arity category query
bindings, coroutine debounce/latest operators on 1.9.0, custom calendar max-date and popup nesting.

## Acceptance coverage

| Criterion | Task owner | Evidence |
| --- | --- | --- |
| Period/year/today/restoration boundaries | Stage 1 / task1 | Fixed-date common tests with exact ranges |
| Shared literal accent/case matching | Stage 1 / task2 | Normalizer and existing category-search regressions |
| Presets/type/category and filter edit transactions | Stage 1 / task3 | Pure filter-transition tests |
| SQL range/currency/order/years/invalidation | Stage 2 / task1 | Real SQLite collectors active before writes |
| Structural Search predicates/count eligibility | Stage 2 / task2 | Mixed-filter real SQLite tests + query-plan/volume evidence |
| Exact History totals/search-independent summary | Stage 3 / task1 | Pure projection, subtotal overflow and valid extreme-balance tests |
| Latest History state/retry/resume | Stage 3 / task2 | One virtual scheduler and request identity tests |
| Search Apply/cancel/clear/debounce/count | Stage 3 / task3 | State machine/real observation tests |
| History states and shared row compatibility | Stage 4 / task1 | Pure screen semantics, fixed previews and Home regressions |
| User visual guide and count-free Aplicar | Stage 4 / task2 | Search/sheet semantics + reference comparison |
| Entry retention, Back, one bar/sheet | Stage 5 / task1 | Real shell lifecycle/restoration integration |
| Create -> database -> all three consumers | Stage 6 / task1 | Real graph, file-backed persistence and physical scenarios |
| Keyboard, insets, accessibility, performance | Stage 6 / task1 | Separate confirmed physical Android/iPhone evidence |

Execution checks (not run by this documentation task):

- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64`
  (the latter compiles common tests, without running a simulator).
- `./gradlew detekt ktfmtCheck`
- `./gradlew :androidApp:assembleDebug`
- Discover exact device compile/run and SQLDelight migration tasks from the actual AGP/source-set
  configuration. Query-only additions do not require a fabricated migration.

Confirm one authorized physical Xiaomi/tablet/iPhone before installing or running device checks;
resolve local tool paths without editing local.properties/signing files. Verify targets sequentially.
AGENTS prohibits emulator/simulator runtime even where older docs suggest it. Missing device/stalled
runner means explicitly unconfirmed execution, never an inferred pass from compilation.

## Gaps and re-plan boundaries

| Gap/risk | Required treatment |
| --- | --- |
| Week wording/annual History | Pending user detail; no calendar-week claim or silent annual omission |
| Search versus always-visible global balance | Resolve in Stage 1; document explicit screen policy before visual implementation |
| 58 categories versus screenshot | Use real catalog with exact-ID matching and reachable scrolling; parent expansion is a separate product decision |
| Persisted category language | Search what rows display; current bootstrap names do not auto-localize after system-language change |
| Material row/category chip literals | Targeted token migration/compact presentation; preserve existing Home/category picker behavior |
| All-history volume | Profile real reads/projection/typing; re-plan paging/count/normalization if required |
| Home currency filtering after LIMIT | Do not copy into History/Search; integration identifies any needed focused consistency fix |
| createdAt variable-precision TEXT order | Preserve current stable SQL order; do not claim universal chronological instant sorting. Normalization/migration is separate work |
| Instrumented runner stall | Diagnose execution before relying on automatic device results; preserve manual target evidence separately |
| iPhone/tooling/backlog inconsistencies | Use existing backlog; reconcile stale simulator/TEAM_ID instructions without changing signing configuration |
| Auth/Drive, annual History, charts and detail/edit/delete | Later MVP/features, not implied complete by this wave |

Re-plan if period scope changes, category parent semantics expand, All history needs paging, declared
KMP APIs cannot meet saved-entry lifetime, or a schema/data migration becomes necessary. Helpers/file
names may adapt; approved behavior, single ownership and verification may not silently change.

## Closure

- [ ] Pending period/global-balance decisions are settled and canonical owners agree with behavior.
- [ ] All task/stage evidence belongs to the current feature tree; no open correctness findings.
- [ ] Android acceptance and iPhone execution status are explicit and separate.
- [ ] User accepts the physical feature before merge.
- [ ] Compact into `docs/features/history.md` and `docs/features/search.md`, update canonical owners,
      remove resolved backlog entries and archive the feature worktree through the repo workflow.
