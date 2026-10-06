# Wave 4 — Search and filters implementation plan

**Status:** Planning only. Product scope is agreed; implementation, runtime acceptance, commits and
integration are not authorized by this document. The application currently has no Search destination.

**Goal:** Deliver shared text search and transactional filters with exact results/count, first-read
skeletons, retained same-request revalidation and correct navigation/modal lifetime.

**Architecture:** SQLDelight owns persisted data. Extend MovimientoRepository with a coherent
structural snapshot. SearchViewModel owns input, filter sessions and results. Pure Compose content
renders immutable state/Actions; the shell coordinates navigation and modal exclusion.

**Planning role:** task-planner, as defined in the global skill and referenced by repository methodology.
The execution role is operator-orchestrator only after implementation is separately authorized.

**Spec owners:** [Business rules](../../business-logic.md#search-and-filters-planned-wave-4),
[Search presentation](../../ui/architecture.md#planned-search-presentation-wave-4),
[components](../../ui/components.md), [wording](../../wording.md),
[methodology/API gates](../../README.md), [delivery registry](../../features/README.md).
Read these with the assigned task; keep each durable rule in its canonical owner.

After separate implementation authorization, use operator-orchestrator and the repository's subagent
execution workflow. Every worker has bounded file ownership, accommodates others' changes and never
reverts them. One final independent review is required. An intermediate review is reserved for the
coherent repository snapshot boundary if it blocks dependent state implementation.

## Scope and defaults

Includes Search tab and Home search entry point, field, exact count, removable chips, read-only rows,
filter sheet/calendars, empty/error/retry states, loading and supported restoration. Exact behavior
lives in the canonical sections. No balance subscription, detail editing, chart, currency selector,
category authoring, sync, dependency upgrade, FTS or pager is added.

Home opens retained Search without clearing input, reveals its header and focuses the field once.
This explicit Home-entry intent takes precedence over retained scroll; ordinary tab return retains it.
Selecting the Search tab does not automatically open the keyboard. Search root Back returns Home;
IME and popup/sheet dismissal take precedence. Creation remains shell-owned and reusable from Search.

## Baseline and reference evidence

Relevant existing assets: `core/ui/component/TallybookSearchField.kt`, `core/ui/text/SearchText.kt`
and their tests. `core/ui/navigation/Destinations.kt` has Home, History and UiCatalog only. Search is
inert in TallybookBottomNavigation; `feature/home/HomeOuterShell.kt:HomeSearch` is presentational.

Declared versions in the catalog/build: Kotlin 2.4.10, Compose Multiplatform 1.11.1, Material3
1.11.0-alpha07, Navigation3 1.1.1, lifecycle 2.11.0-beta01, Koin 4.2.2, SQLDelight 2.3.2, coroutines
1.9.0, datetime 0.8.0, Compose Unstyled 2.9.0 and AGP 9.0.1. Wrapper: Gradle 9.1.0; daemon: Azul JDK
21; shared JVM target: 11; Android minSdk 31 / compile-targetSdk 36. Confirm resolved versions at
execution; catalog declarations are not a new dependency-resolution result.

Moneta is a read-only reference in the sibling `web/moneta` repository. On this host it is
`/Users/sukiyamero/Desktop/programacion/web/moneta`; rediscover the path on another host.

| Moneta source under `src/features/search/` | Reuse | Tallybook adaptation |
| --- | --- | --- |
| SearchScreen.tsx | Header, field, chips, rows, empty states | Currency-bound reactive SQL; lazy stable-ID rows; no editing |
| useSearchFilters.ts and tests | Defaults, independent clearing, exact IDs | Typed applied/draft/closing transaction |
| dateRangePresets.ts and tests | Inclusive 7/30-day offsets, reversed ranges | Month/year end at today; injected civil dates |
| useDebouncedQuery.ts and tests | 250 ms debounce, immediate clear | Request/generation guards and cancellation |
| searchMatch.ts and tests | Contiguous substring of description/category | SearchText intentionally preserves ñ |
| FilterSheet.tsx and tests | Presets/type/categories and tints | Count-free Apply; reachable fixed footer |
| SearchLoadingState.tsx and screen tests | Four rows and delayed first feedback | Measured MovimientoRowSkeleton; existing 200 ms token |

The [screen](references/search-screen.png) and [filters](references/search-filters.png) guide
composition. Screenshot data/categories and its quantity-bearing button are not requirements.
Moneta's whole-store filtering, live filters, seed fallback, console logging and edit action are not
the selected implementation. Moneta's custom calendar permits future dates; this contract caps today.

## Ownership and patterns

| Concern | Owner / chosen pattern | Existing seam |
| --- | --- | --- |
| Persisted data | Existing repositories, typed DataOutcome, SQL predicates | MovimientoRepositoryImpl / CategoriaRepository / preferences |
| Structural rows and currency presence/years | One MovimientoSearchSnapshot read transaction | Existing range mapper, year query, IO dispatcher, asLocalDataOutcome |
| Raw query/applied filters/civil context/session | SearchViewModel authoritative input/state | History synchronous invalidation and saved-input convention |
| Candidates/results | One active source snapshot, normalized fields, pure matching | SearchText / MovimientoRowContent |
| Loading/error | RetainedLoadState<SearchResultsUi, SearchFailure> plus request identity | RetainedLoadState; derive flags/count |
| Filter transaction | Hidden / Editing(id,draft) / Closing(id,render) | Creation origin-token/hide-lifetime discipline |
| Route | Koin/lifecycle/localized presentation inputs | HistoryRoute / decorated entry SavedStateHandle |
| Scroll/focus/IME/animation | Composition | ScreenFrame / viewport overlays / date and modal primitives |
| Rendering | Plain SearchScreen and SearchFilterSheet with state/Actions | Previewable content without app graph |
| Modal exclusion | Shell consults authoritative session availability | AddMovimientoSheetSession remains creation owner |
| First-read feedback | Result-only delayed presentation; live controls remain current | LoadingPresentation / deferLoading / theme token |

No Search repository wrapper, generic SQL builder, copied History ViewModel, framework-bearing criteria
or cache of previous queries is required. Domain types exposed by the existing public repository are
public; feature types remain internal. Use the established explicit-backing-field idiom. Add a buffered
Channel only for a real one-shot effect; modal visibility is state, and shell navigation uses callbacks.

## Data and matching pipeline

1. Resolve initialized primary currency and applied date/type/category criteria; text is excluded.
   Invalid Custom input starts no repository read.
2. A cold `observeFiltered(criteria)` returns `DataOutcome<MovimientoSearchSnapshot>`: structural rows
   and all persisted currency years from one read transaction. Years determine currency presence and
   earlier date availability; independently timed reads must not produce a false no-data screen.
3. Observe real categories separately; failures never become seeded success. Normalize candidate
   description/displayed category once per structural/category/label change, on injected computation
   dispatcher. Missing IDs use the same localized fallback in rows, chips and matching.
4. Settle normalized text with 250 ms latest-only debounce; normalized blank clears immediately.
   Equivalent normalized edits need no matching job; raw input is still kept/checkpointed.
5. Match without per-character database subscriptions. Publish one immutable SearchResultsUi when
   criteria, text and candidate generation are current; count derives from that result list.
6. Insert/delete invalidation naturally updates matches. Never append creation results, getAll/cache
   history, manually refresh after creation or reset inputs. Cancellation releases listeners.

Structural identity controls subscriptions; normalized text controls matching; category/locale/fallback
changes regenerate candidates. Reload/subscription epochs guard late frames without changing product
query identity. Invalidate synchronously before asynchronous work. CPU loops check cancellation; a
cancelled synchronous SQL call may finish but cannot publish into a newer request.

## Dependency stages and file ownership

| Stage | Task / owner | Depends on | Observable exit |
| --- | --- | --- | --- |
| 1 | [Task 1: filters and saved input](stage-1/task1/task.md) | Canonical rules | Pure dates/session/restore contracts proven |
| 1 | [Task 2: coherent SQL observation](stage-1/task2/task.md) | Task 1 criteria | Structural reads/metadata/reactivity proven |
| 2 | [Task 1: state, matching, loading](stage-2/task1/task.md) | Stage 1 | Current results/session isolation/loading matrix proven |
| 3 | [Task 1: screen/filter content](stage-3/task1/task.md) | Stage 2 | Pure UI, skeletons, popup/footer integration proven |
| 3 | [Task 2: navigation/acceptance](stage-3/task2/task.md) | Stage 3 Task 1 | Entry/modal/lifecycle integration and review proven |

Task 1 owns criteria first; Task 2 subsequently owns repository/SQL and affected fakes. Stage 2 alone
owns ViewModelModule. Stage 3 Task 1 owns component extensions/theme/resources and pure UI; Task 2 owns
navigation/shell/Home wiring and runtime integration tests. The orchestrator serializes canonical docs.
Stages are dependency boundaries, not repeated full builds/reviews or artificial parallelism.

## Acceptance coverage

| ID | Boundary | Implementation / evidence |
| --- | --- | --- |
| A1 | Defaults, inclusive/reversed dates, leap/year/clock changes | S1T1 fixed-date filter tests |
| A2 | Exact IDs, AND/OR composition, mixed currencies/types | S1T2 real SQLite fixtures |
| A3 | Empty filtered rows with currency data; out-of-filter mutations update years | S1T2 coherent observation/transaction tests |
| A4 | Null/blank/long descriptions, labels, accents/ñ/literal text | S2T1 projection tests with SearchText |
| A5 | Draft isolation, zero-result Apply, clear/discard, old callbacks | S1T1 transitions + S2T1 subscription assertions |
| A6 | 249/250 ms, immediate clear, Apply while typing, cancellation | S2T1 one-scheduler concurrency tests |
| A7 | <200/≥200 ms first read, metadata chatter, cached empty return | S2T1 presentation + S3T1 semantics |
| A8 | Different request never claims old results/count; text pending explicit | S2T1 identity + S3T1 state transitions |
| A9 | Preference/category/read failure with silent peers; retry keeps inputs | S2T1 source-order/failure tests |
| A10 | Retained tabs/scroll, Home focus, saved owner, discarded restored draft | S3T2 real navigation/restoration |
| A11 | Filters/creation exclude each other through hide; ordered Back/popup | S3T2 modal integration + physical smoke |
| A12 | 58 categories, missing IDs, older-year scroll, large font, dark theme, money, safe/IME | S3T1 semantics/previews + physical layout |
| A13 | Shared Native compile and actual popup/keyboard target behavior | S3T2 separate Android/iPhone evidence |
| A14 | Complete history, exact count, bounded work/memory/cancellation | S1T2 query plan + S3T2 agreed volume probe |

## Version-sensitive gates

Primary contracts reviewed during planning:

- [SQLDelight 2.3.2 arguments](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/query_arguments/)
  supports bound scalars and collection IN. Empty/many-ID generated behavior requires actual tests.
- [SQLDelight 2.3.2 transactions](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/transactions/)
  provides transactionWithResult. Prove combined reads/invalidation with real drivers.
- [SQLDelight 2.3.2 observable queries](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/coroutines/)
  provides query Flow updates. Verify generated listener dependencies and cancellation cleanup.

Execution also verifies resolved Navigation3/lifecycle/Koin/Unstyled versions and official source for
changed restoration/modal behavior. New Search restoration, coordinated pickers, minimum-year support
and footer placement need complete integration checks. Documentation/compilation is not runtime proof.

## Verification and readiness

Planning evidence consists of source/test/reference inspection, primary contract review, document
link/path/coherence checks and a final plan review. No new app build/test/device result is asserted.

Authorized execution runs task-specific checks through gradle-run's wrapper. Kotlin always requires
`./gradlew detekt ktfmtCheck`; broaden once for repository/navigation regressions, not every task.
Android builds and Native compile are separate from runtime. No emulator/simulator runs; Native
simulator test compilation is compilation only.

Before physical steps identify one authorized Xiaomi, Android tablet or iPhone, discover actual
toolchain/serial, and follow AGENTS.md. Missing hardware gates only runtime acceptance; documentation
planning needs no device. Android evidence cannot confirm iPhone behavior.

All-history search needs one bounded scale probe. Agree representative volume before executing it:
2,000 mixed-currency/type/category rows is a proposed starting point, not an approved run/capacity
claim. Cover All, This year and sparse category + text. Record correctness, read/match latency, UI
responsiveness, retained allocations and subscription counts. No production seeding or repeated/default
10,000-row probe; no brittle wall-clock unit assertions. If materialization/normalization blocks UI
or retains unbounded snapshots, re-plan normalized storage/paging/exact count together before delivery.

Implementation authorization, resolved integration evidence and physical setup are execution gates.
There is no unresolved product choice for the agreed scope. A plan cannot guarantee error-free code;
tests and final review remain mandatory.
