# Stage 2 / Task 1 — Search state, matching and loading

Read [Wave 4](../../README.md), the canonical loading matrix and both Stage 1 contracts.
Planning only. Coverage A4–A9 and state/restore portions of A10/A12/A14.

## Goal, files and responsibility

Own the single Search input/session/result owner and pure result projection, not shell/UI rendering.
Proposed files under `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/`:

- `viewmodel/SearchUiState.kt`: state contract and derived current-count/readiness.
- `viewmodel/SearchActions.kt`: typed user/lifecycle/session callbacks.
- `viewmodel/SearchViewModel.kt`: authoritative inputs, subscriptions, cancellation and frame acceptance.
- `viewmodel/SearchProjection.kt`: candidates and literal matching/projection; cancellable work.
- `viewmodel/SearchFailure.kt`: typed source/date failures; no raw throwable messages in UI.
- `viewmodel/SearchSavedState.kt`: actual entry SavedStateHandle checkpoint registration/recovery.
- `ui/SearchResultsUi.kt`: settled result aggregate with rows, request identity and currency metadata;
  constituent SearchResultRowUi shares this aggregate's file, not SearchUiState.kt.
- `ui/SearchPresentation.kt`: pure result-feedback adapter and composable helper; reuse shared delay.
- `viewmodel/SearchDateProvider.kt`: Search-local injected Clock/TimeZoneProvider capture seam.

Own the additive Koin edit to existing `di/ViewModelModule.kt`. No Search import of History types:
reuse its pattern, not its feature package. Keep date capture Search-local rather than moving working
History classes into core solely for symmetry.

Tests under matching commonTest feature directories: `SearchViewModelTest.kt`, `SearchProjectionTest.kt`,
`SearchSavedStateTest.kt`, `ui/SearchPresentationTest.kt`. Include existing Koin graph verification at
the project's current test seam; do not add a new DI framework or dispatcher module.

## Produced screen contract

`SearchUiState` exposes raw query, applied SearchFilters, civil today, initialized currency, authoritative
SearchFilterSession, category source readiness and `RetainedLoadState<SearchResultsUi, SearchFailure>`.
Relevant pending-work/request identities distinguish waiting for text, structural reading and matching;
do not flatten these into duplicate isLoading/hasResults flags or store another count. SearchResultsUi
stores one settled result identity and row list plus coherent currency years; count is a getter.
Retain small currency-tagged last-read year metadata for filter correction independently of result
rows. This is one immutable metadata owner, shared by reference with the result that came from that
read; do not copy years into a second independently mutable state. Only a current coherent Ready
result can establish current no-data. Clearing invalid/unrelated rows need not delete same-currency
metadata; currency changes must clear it.

`SearchActions` covers query change/clear, applied-chip removal, retry, resume/presentation changes,
open/edit/clear/apply/cancel/hidden for filters. Every session action carries SearchFilterSessionId.
It exposes synchronous authoritative `hasActiveFilterSession` for shell guards. ViewModel callbacks
commit session/input invalidation synchronously; an asynchronously combined UiState must never be the
only authority for whether a session can open. Composition receives Actions, not ViewModel types.

Filter content reads `Editing.draft` or `Closing.renderSnapshot` and real category/years state.
Normal filter opening requires initialized currency plus an available coherent read/category snapshot,
even if revalidating. Read failure without those inputs leaves retry available and opening disabled;
existing draft can still cancel. Invalid applied Custom is an explicit correction exception: with
initialized currency and ready categories, filters can open using retained same-currency years, or
the shared floor plus applied/draft endpoint years if no currency metadata exists. It starts no invalid
read and cannot declare no-data from cached metadata. Known empty categories are ready with clear copy.

## State and concurrency strategy

1. Use one mutable authoritative input owner for raw text/applied filters/today/session identity.
   StateFlow holds the renderable state; update it synchronously for accepted user actions. Candidate
   snapshots and matching outputs are derived pipeline values with versioned acceptance, not a second
   editable filter owner. SavedStateProvider reads current applied input lazily from this owner.
2. Follow History's subscription-aware five-second sharing and before-collection revalidation pattern.
   Production owns jobs in viewModelScope; tests inject one scope/dispatcher/scheduler. WhileSubscribed
   may stop data work but must not stop synchronous session actions or save the current state too late.
   Scope cancellation/onCleared releases only owned jobs/checkpoint; no repository-held CoroutineScope.
3. Preference initialization/failure, category readiness/failure and repository emissions are explicit
   SourceLoad values. Failure from one source must surface while another source is silent; a combine
   awaiting a first value is not allowed to hide known failure. Do not use fake seeded categories or
   default currency to satisfy initialization. Missing category IDs are distinct from catalog failure.
4. Structural key is currency + resolved range + type + canonical immutable category IDs. Session edits
   never change it. Resolve valid Apply atomically; unchanged criteria require no new subscription.
   Removing a chip changes only its dimension. Currency/criteria changes clear unrelated snapshots
   before starting/cancelling work; invalid dates expose correction state with no read subscription.
5. Text identity is normalizeSearchText(raw). Nonblank changed identity settles 250 ms after the last
   edit; blank/clear settles immediately; identical normalized edits need no delay/new projection.
   Keep raw text and settled request text distinct by temporal role, not as independently editable
   copies. Clear cancels the pending timer and preserves applied filters.
6. A raw edit immediately marks count/empty semantics unavailable if its identity is unsettled.
   During text-only pending work, one same-structural settled row snapshot may remain as a visibly
   updating display, with immediate plain searching status in the count line. Its own text identity
   stays attached. Never present its old count or empty-query
   message as the new query. New criteria cannot use that display exception. Once new text fails,
   discard a previous different-text snapshot rather than retain it as current error fallback.
7. Apply/remove-chip during pending text keeps the original last-edit deadline. New structural reads
   need not wait for debounce, but their result cannot be declared current for an unsettled raw query.
   Snapshot arrival, draft edits, category metadata and revalidation do not restart that text timer.
8. Build normalized candidates once per read/category/display-label generation, referencing the domain
   Movimiento rather than cloning it. Match on injected DEFAULT computation dispatcher with mapLatest
   or equivalent structured latest-only ownership. Check cancellation in long loops. Gate publication
   on structural key, normalized text, candidate generation, reload and observation epoch. Atomically
   accept rows/count/failure for the current request, never separate row/count emissions.
9. Resume captures Clock/timezone once, re-resolves relative presets and refreshes sources. Custom is
   absolute; if a backward clock makes it future, expose correction without querying that range.
   Retain same-currency metadata for correction and allow its filter-open exception, including a
   simultaneous currency change with no new read metadata. Existing draft edits remain intact on
   resume; refresh its relative date context and validate again
   on Apply. No background midnight ticker. Same-request revalidation retains valid/empty snapshots;
   current count returns only once Ready. Retry keeps query/filters and resubscribes failed sources.
10. Category rename/locale/fallback change invalidates candidates and localized projection without a
     database movement emission. Displayed labels and searchable labels use the same generation.
     Real creation/delete updates via SQL observation; no manual merge/reset/refresh signal.

## Result presentation boundary

Implement the canonical loading matrix using the existing 200 ms token and pure adapters. Initial
structural pending with no snapshot gets delayed skeleton feedback. Metadata chatter must preserve
its deadline. Keep input/applied chips/session live independently of result-only feedback. Reset the
presentation boundary at structural change to avoid deferLoading holding an unrelated rendered frame.

Text debounce alone shows no spinner/skeleton; sustained matching starts a separate 200 ms feedback
deadline when computation actually begins. Same-request revalidation shows a discreet indicator only
after sustained work. This is display timing, not a fake delay on matching/SQL. Fast Ready/Failed values
cancel feedback immediately. Tests use an injected monotonic source/shared scheduler, not real sleeps.

## Saved owner, rows and edge fixtures

Checkpoint schema/input with encodeToSavedState/decodeFromSavedState and the real entry handle, matching
HistorySavedState. New owner rebuilds rows/categories and starts Hidden. A retained owner keeps its draft;
ordinary recomposition cannot cancel it. Raw query never enters logs/preferences. No serialized job,
failure, scroll object, session phase or candidates. Existing platform restoration scope applies.

Projection uses description when nonblank and category fallback otherwise; null/missing/empty labels
never become raw IDs. Results keep repository order and exact Money values; no totals/Double conversion.
Raw quotes, percent, underscore, emoji and regex-like text are literal. Internal spaces are not tokenized.
Preserve ñ/n behavior from existing SearchTextTest. Long queries/labels produce correct bounded matching
without rebuilding normalized candidates for every character.

## Test-first slices / exit

- [ ] One-scheduler 249/250 ms debounce, clear/whitespace immediate, normalized equivalent input,
      abandoned timer, Apply/remove-chip with pending text, slow cancelled projection and late frames.
- [ ] Count/rows currentness with partial source arrival and true currency-empty/nonmatching metadata;
      category/metadata/movement failures surface before silent peers; retry preserves inputs.
- [ ] Subscription counters prove no draft/keystroke SQL reads; unchanged Apply does not resubscribe.
      Preference/currency changes and actual criteria changes do; listeners disappear after stop/clear.
- [ ] Every session transition including old callback during newer Closing, disabled render retention,
      initial read failure open guard and zero-result Apply independent of count.
- [ ] Same-query success/empty return has no skeleton; <200 ms fresh success, ≥200 ms slow read,
      metadata deadline, error/cancel before grace, changed criteria during old delayed presentation.
- [ ] Text pending retains only same-structural rows without old count/no-results; no debounce spinner;
      sustained computation/revalidation feedback ends at Ready/error/disposal.
- [ ] Candidate generation on rename/fallback/locale without SQL emission; null/blank description,
      missing selected ID, literals, accents/ñ, exact order/Money and matching/nonmatching insert/delete.
- [ ] Saved-state round-trip/recovery through actual handle, new owner Hidden, retained owner draft,
      relative day/month/year rollover and backward-clock Custom correction. Assert filters open to
      fix it, same-currency metadata survives row invalidation, currency change clears metadata, and
      correction still opens with ready categories without issuing an invalid query.
- [ ] Run targeted common/host tests, Native test compile and mandatory Kotlin checks.

Through gradle-run during execution:

`./gradlew :shared:testAndroidHostTest --tests '*SearchViewModelTest' --tests '*SearchProjectionTest' --tests '*SearchSavedStateTest' --tests '*SearchPresentationTest'`

`./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`

Exit: current results and UI session/read contracts are deterministic and tested. No UI task starts
against ambiguous synchronous modal availability, result identity or pending-count behavior.
