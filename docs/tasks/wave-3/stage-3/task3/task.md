# Wave 3 / Stage 3 / Task 3 — Search state and filter edit sessions

## Objective and context

Implement independent Search state for the [wave contract](../../README.md), especially confirmed
draft/apply/dismiss behavior. Requires reviewed Stage 2 and shared text/projection conventions.
May run beside History task2; serialize the shared Koin edit through the orchestrator.

Skills: `android-dev`, `kotlin-concurrency-and-flow`, `compose-state-and-effects`, `koin`,
`android-testing`, `test-driven-development`, `gradle-run`.

## Files and interfaces

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/viewmodel/SearchUiState.kt`,
  `SearchActions.kt`, `SearchViewModel.kt` and `SearchProjection.kt`, separated by responsibility.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/ui/SearchResultUi.kt`:
  one row aggregate with description-first title, date/category subtitle inputs and exact Money.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt` once task2's edit
  is coordinated. Do not add a Search repository wrapper around MovimientoRepository.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/search/viewmodel/SearchViewModelTest.kt`
  and `SearchProjectionTest.kt`.

Consumes initialized preferences, observeFiltered/observeYears, categories, Clock/timezone, injected
computation dispatcher, shared normalizer and Stage 1 filter transitions.
Produces one `StateFlow<SearchUiState>`/Actions contract for text, applied-chip removal, filter-session
open/edit/clear/apply/cancel, retry and resume. UiState distinguishes applied filters, optional draft,
current request/matching status, result rows/count, currency-data status and durable failure. Its
filter session is Hidden/Editing/Closing; optional editable draft derives only from Editing.

## Specific approach

1. Applied filters plus currency produce one structural request. Only applying/removing an applied
   chip changes that request; draft changes must never invoke observeFiltered or alter live results.
2. Draft is a temporary working copy, not second applied state. Apply validates/commits once and
   changes Editing to Closing; cancel changes to Closing without applying. Closing retains disabled
   immutable render data until onHidden, then Hidden releases it. Reject reopen during Closing and
   ignore duplicate/late actions. No per-field intermediate commits or count-dependent enablement.
   Bind every session action/hidden callback to its session identity; an old callback cannot release
   a newer Closing session. Expose synchronous authoritative session availability for shell requests,
   independent of asynchronously collected UiState; the shell keeps no copy of filter values.
3. Clear draft preserves raw query; dismiss discards draft. Clear input preserves applied filters.
   Reopening always starts from the latest applied values, including screen chip removals.
4. Use latest-only cancellation/request identity for structural reads and 250 ms text matching,
   immediate clear, one shared scheduler in tests. Normalize candidates once per data/category update;
   no full-history copy besides the one active repository snapshot or database read per character.
   Use the shared pre-normalized matcher. The route updates an explicit fallback-label input; a label
   change rebuilds candidates without requiring a database emission and rows display that same label.
5. Count is exactly the current settled matching list size. While matching/loading/error is current,
   mark count unavailable/updating; never report cached or preview count as current. Filter draft
   changes do not cause a count preview or result query.
6. Observe currency years metadata to distinguish “no movements in this currency” from “no results”.
   Wait for verified metadata before asserting global emptiness. Category/metadata failures are
   explicit; fallback labels remain visible/searchable, not a hidden filter reset.
7. Search and History share text normalization and core presentation seams, not each other's ViewModel
   or feature package. Search does not import History selection or period-summary calculations.
8. Saved input contains applied filters/raw query only. Tab switching retains entry/input/scroll;
   restoring into a new entry owner closes/discards an open draft and rebuilds data. Rotation that
   retains the existing owner preserves its draft; ordinary recomposition is not cancellation. Raw
   query is never logged or persisted in preferences/analytics.
9. On resume capture date/timezone once and re-resolve relative applied presets. Custom stays absolute.
   If a sheet remains open across pause, refresh draft relative context without applying it; retain
   draft edits, then validate against refreshed today on Apply. No background midnight ticker.
10. Recoverable read error preserves applied filters/query, with stale content only for the same
    request and explicit status. Retry resubscribes without resetting filters. Creation invalidation
    updates matches naturally without append/refresh or query reset.

## Test-first slices

- [ ] RED/GREEN default all-history state, each upstream initial failure, honest currency-data emptiness
      and settled count. Test metadata ordering so “no data” does not flash before readiness.
- [ ] RED/GREEN draft isolation, Clear/Apply/cancel/reopen, duplicate actions, zero matches and
      missing-category IDs. Assert draft edits create no structural read subscriptions.
- [ ] RED/GREEN Closing render lifetime, onHidden cleanup, rejected reopen before hidden and stale
      callbacks; applying/canceling once cannot mutate results again during the hide transition.
- [ ] RED/GREEN debounce 249/250 ms, immediate clear, pending match plus Apply/remove chip, late
      structural results and cancellation. Count/rows always match active applied request/query.
- [ ] RED/GREEN retry retaining all inputs, entry saved-input recreation, discarded draft restoration
      and relative-preset rollover versus stable custom dates.
- [ ] Active collector plus matching/nonmatching inserts updates results/count without reset; foreign
      currency cannot appear. Projection titles/subtitles/fallback reflect the supplied guide.
- [ ] Run focused/common host tests, Native test compilation, Detekt and ktfmt; verify Koin graph.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*SearchViewModelTest' --tests '*SearchProjectionTest' --tests '*SearchFiltersTest'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when applied state, draft lifetime and exact current results are proven. Blocks Stage 4.
If volume requires a pager or new normalized storage, re-plan count/search semantics before claiming
that the all-history contract is supported.
