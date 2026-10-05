# Wave 3 / Stage 3 / Task 2 — ViewModel, cancellation and retained input

## Objective and context

Own History's screen state and reconnect accepted selection/search rules to real repository streams.
Read the [wave contract](../../README.md), task1 projection and current Home/form state patterns.
Requires task1 complete and reviewed Stage 2.

Skills: `android-dev`, `using-chrisbanes-skills`, `kotlin-concurrency-and-flow`,
`compose-state-and-effects`, `koin`, `android-testing`, `test-driven-development`, `gradle-run`.

## Files and responsibility

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryUiState.kt`:
  screen contract only, separate from row/group models.
- Create adjacent `HistoryActions.kt` and `HistoryViewModel.kt`: typed intents, observation and state.
- Keep saved input beside `HistorySelection.kt`, or create `HistorySavedInput.kt` if serialization is
  an independent unit of change. Do not add a new storage repository for screen preferences.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt` for the real consumer.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryViewModelTest.kt`.

## Consumed and produced contracts

- Consumes `MovimientoRepository.observeRange/observeYears/observeSummary`, category observation,
  initialized preferences, the pure projection, injected Clock/TimeZoneProvider and computation
  dispatcher. A test scope follows existing injection precedent without leaking into production API.
- Produces `StateFlow<HistoryUiState>` and `HistoryActions`: scope/period/year selection, raw query
  changes, clear search, retry and resume/date refresh. The UI supplies the resolved fallback label
  through a deliberate presentation input, not a composable resource call inside the ViewModel.
- UI state includes selection inputs, today/available years, data status, matching status, projection,
  overall balance status and typed failure. Derived empty/no-results/readiness values cannot be copied
  into disagreement with their inputs.
- Saved input contains only the Stage 1 validated small fields. Its entry lifetime is integrated and
  proven by Stage 5; a serializer unit test alone does not promise navigation retention.

## Specific approach

1. Observe initialized preferences first. Only `Ready` selects a currency; uninitialized startup state
   is a real failure. Do not manufacture a default currency or empty content during startup errors.
2. Form one immutable request identity from currency and resolved range. Switch observations using
   latest-only cancellation. Tag/loading data by request identity so old results cannot accompany new
   filter labels. Map on the injected computation dispatcher when needed for the measured volume.
3. Observe categories and years without letting metadata emissions reset selection. Initial failures
   render immediately even when another source has not emitted. If year metadata fails, retain known
   options/current selection and expose an actionable failure; do not misreport an empty year menu.
4. Keep typed query visible immediately. Normalize and debounce nonempty matching by 250 ms with the
   shared virtual scheduler; blank/clear bypasses delay. Period changes match the current raw query
   against the new range without briefly showing results for an old query.
5. A pending query marks its result area as updating; stale rows/counts are either withheld or clearly
   represented as belonging to the previous settled query. The UI cannot announce them as current.
6. Summary and groups use one range snapshot. Overall balance is independently labelled and has its
   own loading/failure status; do not substitute the period balance on a global-summary error.
7. A read/calculation failure is durable. Preserve selection/query and the last successful snapshot
   only for the same request; mark stale content and avoid claiming it is current. On a changed request,
   show loading/error instead of rows with the wrong dates. Retry resubscribes the affected reads, not
   resets inputs; no infinite auto-retry or snackbar-only errors.
8. Use one deliberate shared StateFlow. With lifecycle-scoped consumers and no required background
   `.value` freshness, choose `WhileSubscribed` with the repository's justified retention interval
   (proposed 5 seconds). If synchronous fresh reads become necessary, revise sharing explicitly.
   Cancel active jobs on entry disposal; no `GlobalScope`, independent init collectors or lost errors.
9. On resume/entry refresh today through the accepted pure transition; no midnight background ticker.
   A movement insert only invalidates SQL; it never appends a row, clears search or jumps to another day.
10. Prefer direct Actions callbacks for durable input. Add no effect stream unless an actual one-shot
    behavior emerges; a real effect then uses one buffered Channel collected by one lifecycle owner.

## Test-first slices

- [ ] RED: loading -> content/empty, each upstream failing before others emit, initialized preference
      failure and partial/global-summary error. GREEN: model the pipeline and statuses.
- [ ] RED: rapid A -> B period/currency changes and late A emissions never overwrite B. GREEN: use
      cancellation plus request identity; confirm old repository collectors terminate.
- [ ] RED: debounce boundaries at 249/250 ms, immediate clear, query change during read, period change
      during pending match and no SQL request per keystroke. GREEN: implement the matching pipeline.
- [ ] RED: retry preserves selection/query, recoverable failure, arithmetic overflow, years update and
      selected-year retention after data deletion. GREEN: handle explicit recovery.
- [ ] RED: resume after date/zone/year change and saved input recreation restores inputs while
      repository content reloads. GREEN: wire accepted temporal/restoration transitions.
- [ ] Observe insert inside/outside active period and match/nonmatch query. Assert unchanged inputs,
      correct summary and filtered rows without calling refresh.
- [ ] Use one `TestCoroutineScheduler` for scope, injected dispatchers and `runTest`; no wall-clock sleeps.
- [ ] Run full host regression, Native test compilation and lint/format checks; confirm Koin wiring.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*HistoryViewModelTest' --tests '*HistoryProjectionTest'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when latest-state, failure, cancellation and saved-input contracts are proven. Blocks Stage 4.
Unavailable KMP saved-state APIs require an explicit compatible approach under the evidence gate;
do not quietly weaken restoration or move screen state to an ambient app-wide ViewModel.
