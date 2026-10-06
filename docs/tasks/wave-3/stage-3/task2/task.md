# Wave 3 / Stage 3 / Task 2 — History state, cancellation and retained selection

## Objective and context

Own History state and connect accepted selection to real repository observations. Read the
[wave contract](../../README.md), task1 projection and current Home/form state patterns. Requires
reviewed Stage 2, task1, and the real [early S5.T1 probe](../../stage-5/task1/task.md#early-gate-before-stage-3)
before choosing saved-input transport.

Skills: `android-dev`, `using-chrisbanes-skills`, `kotlin-concurrency-and-flow`,
`compose-state-and-effects`, `koin`, `android-testing`, `test-driven-development`, `gradle-run`.

## Files and responsibilities

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryUiState.kt`,
  `HistoryActions.kt`, `HistoryViewModel.kt`: immutable screen state, typed actions, observations.
- Keep saved input beside `HistorySelection.kt` or separate `HistorySavedInput.kt` if warranted;
  small version/scope/ISO anchor only, no storage repository/preferences/query field.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt` for History only.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistoryViewModelTest.kt`.

## Consumed and produced contracts

Consumes observeRange/observeYears/observeSummary, categories, initialized preferences, pure
projection, injected Clock/TimeZoneProvider/computation dispatcher and the verified entry saved-owner
transport. Test scope injection follows existing precedent without leaking a test type into production.
Produces StateFlow<HistoryUiState> and HistoryActions for scope/period/year, retry, activation/resume
and explicit localized fallback-label presentation updates. Route supplies the same label rows use;
label changes rebuild presentation without requiring SQL invalidation.

UiState derives full picker options and selected-period identity separately from its today-clamped
read range. Selection remains their sole authority; Compose does not calculate ranges.

UiState owns selection/today/known years, current range request, content/empty/loading/read/calculation
failure and projected groups/period count/totals. History has no global-summary read or state. Derived values
cannot be independently copied into disagreement. No query, filter, debounce, matching status/count,
normalized candidates, predicted count or effect stream is created.

## Approach

1. Observe preferences first; only Ready chooses currency. Uninitialized/error startup is a real
   failure, not invented currency or empty content.
2. Build immutable request identity from currency and resolved range. Latest-only switching cancels
   previous collectors; tag loading/current/stale content by that identity. Use injected computation
   dispatcher where real volume warrants it.
3. Category/year metadata cannot reset scope/anchor. Each initial upstream failure is visible even
   when another source has not emitted. Retain known options/current year on metadata error and
   provide retry; never claim a false empty menu or empty currency from failed metadata.
4. Period totals/count/groups use one range snapshot. Home owns the global summary independently;
   History never reads it or substitutes it for unavailable period data.
5. Durable failures preserve selection. Retain stale content only for the same request with explicit
   status; new request labels never show old-date rows as current. Retry resubscribes affected reads,
   without reset/infinite retries/snackbar-only error.
6. One readonly UiState StateFlow projects private typed inputs, their sole editable authority.
   Use the official WhileSubscribed command policy with five-second timeout and one owned producer;
   do not observe output as an input or retain subscriptions through a self-subscribing pipeline.
   STOP invalidates cached freshness/request epoch before waiting for read cancellation, preserving
   current selection and known years. START revalidates; prove the first observed re-entry state after
   hidden writes/timeout. A fixed initial replay value must not reset a historical selection to Day/today.
   Dispose/cancel the owned producer/provider with the entry, preserving any injected caller scope;
   no independent eager collectors or generic derived-StateFlow framework.
7. Resume captures Clock/timezone together and uses S1's transition; no background midnight timer.
   SQL invalidation handles creation: never append/refresh manually or reset period.
8. Restore validated small input into new entry owners using early verified transport. Android supported
   saved-owner/Activity recreation differs from iOS retained-controller/saveable scope; a new iOS
   process starts normally. Repository observations restart; no saved rows, draft, focus or jobs.

## Test-first slices

- [x] RED/GREEN loading/content/empty, upstream failure before peers, startup error and partial
      summary failure; explicit fallback change updates row text without SQL emission.
- [x] RED/GREEN rapid A→B period/currency changes, late A emissions rejected and old collectors canceled.
- [x] RED/GREEN retry preserves selection, overflow/recovery, years invalidation and selected-year
      retention after last row deletion.
- [x] RED/GREEN first re-entry state after timeout/hidden write, shutdown counts and reactivation reads;
      use explicit active collectors rather than weakening fresh `.value` assertions.
- [x] RED/GREEN date/timezone/year rollover and validated restored input reload repository data.
- [x] Inside/outside range inserts update History period and Home global summaries appropriately without manual refresh;
      controls/selection remain unchanged. No matching/nonmatching distinction exists in History.
- [x] One TestCoroutineScheduler for scope/dispatchers/runTest; no wall-clock sleeps.
- [x] Focused/full host regression, Native test compilation, Detekt/ktfmt and real Koin wiring evidence.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*HistoryViewModelTest' --tests '*HistoryProjectionTest'`.
Then `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion and blockers

Latest state, failures, selection/restoration and cancellation are proven; reviewed Stage 3 enables
Stage 4. A failed actual saved-owner integration requires evidence-based re-planning, not app-wide
screen state or unrequested persistence to promise broader iOS restoration.
