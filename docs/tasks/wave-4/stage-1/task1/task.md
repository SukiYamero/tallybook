# Stage 1 / Task 1 — Typed filters, sessions and saved input

Read [Wave 4](../../README.md) and its canonical spec owners. This is a planning contract; execute only
after implementation authorization. Coverage: A1 and the pure portion of A5/A10.

## Goal and ownership

Define framework-free repository criteria and pure Search input/session rules. No SQL, UI or navigation
is owned by this task. Stage 1 Task 2 consumes criteria; Stage 2 consumes filters/session/saved input.

Proposed files under `shared/src/commonMain/kotlin/com/kurobello/tallybook/`:

- `core/model/MovimientoReadCriteria.kt`: public criteria for the existing public repository API.
- `feature/search/viewmodel/SearchFilters.kt`: internal preset/typed filters and pure resolution.
- `feature/search/viewmodel/SearchFilterSession.kt`: session phases and origin-checked transitions.
- `feature/search/viewmodel/SearchSavedInput.kt`: serializable schema/input recovery; no handle wiring.

Tests in matching `shared/src/commonTest/kotlin/.../feature/search/viewmodel/`:
`SearchFiltersTest.kt`, `SearchFilterSessionTest.kt`, `SearchSavedInputTest.kt`.
The orchestrator updates canonical rules only for a confirmed inconsistency, not a second spec copy.

## Consumed / produced contracts

Consumes existing Moneda, TipoMovimiento, MovimientoDateRange, FIRST_SELECTABLE_YEAR and injected
`today: LocalDate`; the resolver never reads Clock, Compose, resources or a platform formatter.

- `MovimientoReadCriteria(moneda, range: MovimientoDateRange?, tipo: TipoMovimiento?, categoryIds:
  Set<String>)`. Empty IDs disable the category predicate; null type/range disable those dimensions.
  Values are immutable by contract and defensively copied at the boundary; no mutable set can change
  a captured request. IDs are nonblank. Validation failures become DataError.InvalidInput in Task 2.
- `SearchDatePreset`: All, Last7Days, Last30Days, ThisMonth, ThisYear, Custom.
- `SearchFilters(preset, customStart, customEnd, tipo, categoryIds)`. Query is separate. Default factory
  takes today; custom endpoints exist even while another preset is selected.
- `resolveSearchFilters(filters, today)` returns typed `SearchFilterResolution.Valid` with normalized
  filters and optional range, or `Invalid` with a typed date failure. A future custom date is invalid;
  a reversed valid range normalizes before constructing MovimientoDateRange. No partially committed
  fields, date sentinels or parallel isAll/isActive constructor flags.
- `SearchFilterSession`: Hidden; Editing(sessionId,draft); Closing(sessionId,renderSnapshot).
  `SearchFilterSessionId` is a monotonic local identity; session actions carry it. Pure transitions
  produce a commit only on Editing → Closing via valid Apply. The ViewModel owns ID allocation.
- `SearchSavedInput`: schema version 1, raw query, preset, ISO custom endpoints, optional movement type,
  deterministic category-ID list. Serialize tokens explicitly; do not depend on enum ordinals.

Active chips/filter status are derived from applied values. An old custom endpoint does not make All
active. Relative-preset range equality does not change the selected preset label; criteria equality
still prevents needless database resubscription.

## Transition and restore details

Opening copies applied filters once into Editing. Edits/Clear require the matching Editing ID. Apply
captures refreshed today once, validates and atomically returns committed normalized filters + Closing.
Invalid Apply keeps Editing with correction feedback; it creates no read. Cancel returns Closing with
the render snapshot and no commit. Closing is immutable/disabled; matching hidden releases it once.
Reopen during Closing and actions for older IDs are no-ops, including an old hidden callback when a
newer session is also Closing. Opening after Hidden allocates a new ID.

Clear replaces just the Editing draft with defaults at current today. Dismiss discards those defaults;
Apply commits them. Preset changes preserve custom endpoints. Categories use immutable exact IDs, with
deterministic serialized/chip order; no parent expansion, catalog lookup or type pruning in the reducer.

Absent/undecodable/unknown-version saved input recovers to fresh filters/raw empty query. With a valid
version and decodable raw query, unknown preset/type, malformed dates or future Custom recovers only
filters to defaults, preserving raw query. Reversed valid Custom normalizes. Duplicate IDs coalesce;
blank IDs are removed during recovery; missing nonblank IDs remain. Never restore session phases,
predicted count, result rows or a partially valid custom interval. Active relative presets recompute
against current today. Existing backward-clock Custom input is invalid pending correction, distinct
from a newly restored invalid Custom input that deterministically recovers.

## Test-first slices and exit

- [ ] Fixed today 2028-03-01: Last7 = Feb 24–Mar 1; Last30 = Feb 1–Mar 1. Include both boundaries and
      exclude adjacent dates; test Jan 1, Dec 31 and leap-day/month-year boundaries.
- [ ] All resolves null; month/year begin at day 1/Jan 1 and end today; Custom one-day/cross-year,
      reversed, future and backward-clock invalid cases; switching presets retains custom endpoints.
- [ ] Category toggling twice returns the original set; caller mutation cannot alter captured criteria;
      All/custom status derives correctly and type/category combinations remain available.
- [ ] Open/edit/clear/cancel/reopen leaves applied values unchanged; valid Apply emits one commit even
      with no changes or zero matches; no result count is required by this pure contract.
- [ ] Duplicate Apply/cancel/hidden, rejected reopen, old actions during newer Editing/Closing; retained
      closing render data exists until exactly its matching hidden cleanup.
- [ ] Saved schema/default recovery, normalized reversed range, malformed/future values, unknown tokens,
      missing category IDs, literal Unicode raw text; serialized fields exclude session/results.
- [ ] Run focused tests and Kotlin checks; compile common tests for Native without running a simulator.

Nested Gradle checks, through the installed gradle-run wrapper during execution:

`./gradlew :shared:testAndroidHostTest --tests '*SearchFiltersTest' --tests '*SearchFilterSessionTest' --tests '*SearchSavedInputTest'`

`./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`

Use camelCase common test names, fixed civil dates and no wall-clock sleeps. Exit requires coherent
typed contracts and passing pure behavior evidence. Task 2 must not start against an unresolved
criteria shape; this is not an independent full-suite or mandatory intermediate review checkpoint.
