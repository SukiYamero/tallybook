# Wave 3 / Stage 1 / Task 3 — Search filter contracts

## Objective and context

Define Search's typed non-text filters and transactional edit rules from the
[wave contract](../../README.md), the supplied visual guide and Moneta's actual reference. The user
confirmed Apply commits and dismissal discards; Moneta's live-filter implementation is not this contract.
Follows task1 for shared date-range and canonical-document ownership.

Skills: `android-dev`, `kotlin-api-design`, `kotlin-control-flow`, `android-testing`,
`test-driven-development`; `gradle-run` for commands.

## Files and interfaces

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/viewmodel/SearchFilters.kt`:
  internal presets, typed filter values, range resolution and immutable draft/apply transitions.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/MovimientoReadCriteria.kt`:
  framework-free structural criteria consumed by the repository: currency, optional range,
  optional movement type and category ID set. Empty IDs means all, not zero results.
- Create `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/search/viewmodel/SearchFiltersTest.kt`.
- Update `docs/business-logic.md` for the accepted filter/domain contract; do not create a second
  durable search-rules document. Coordinate visual balance policy with `docs/ui/architecture.md`.

Presets are `All`, `Last7Days`, `Last30Days`, `ThisMonth`, `ThisYear`, `Custom`.
Filters store preset, custom endpoints, optional type and immutable category IDs. Resolved range,
active-filter status and labels are derived. No `isAll` flag beside an optional type or a date sentinel.
The raw text query is separate and is not changed by clearing filter dimensions.

## Specific approach

1. Publish confirmed Apply/dismiss behavior and validate proposed date/category/currency defaults
   against canonical requirements. Settle overall-balance visibility in Search before Stage 1 closes.
2. One pure resolver consumes filters plus injected today. No system Clock reads, platform formatter,
   Compose annotation or translation keys inside repository criteria.
3. Preset intervals are inclusive; fixed-date tests cover month/year rollover and leap dates. Custom
   endpoints normalize reversed order before application, stay absolute on resume and cannot exceed
   the agreed maximum date. Never silently run a malformed saved custom interval.
4. Category selection is exact ID matching, OR across IDs and AND with date/type. Parent/descendant
   expansion requires a new product decision. Retain children, unrelated category/type combinations
   and selected missing IDs with visible fallback; do not silently narrow the existing catalog.
5. Session phases are Hidden, Editing(draft) and Closing(render snapshot). Apply/cancel ends editing
   once, disables controls and retains read-only render data until `onHidden` releases it. Reject
   reopen while Closing. Clear edits only an Editing draft; Apply is atomic/idempotent at zero results.
   Late hidden callbacks cannot close a newer session. No phase is saved as persistent input.
6. Saved Search input restores only applied filters and raw query. Unknown version/preset/type or
   invalid custom dates recover deterministically to default applied filters; never restore an open
   draft, predicted count or live result list. Retain valid stable IDs even if their labels are missing.
7. Define preset and type copy through typed resource mappings at the UI boundary. Use existing
   English identifier/Spanish domain conventions and no stringly typed SQL keys in screen state.

## Test-first slices

- [ ] RED/GREEN exact All/7/30/month/year/custom ranges, leap/year transitions and inclusive edges.
- [ ] RED/GREEN reversed custom normalization, future/invalid restored bounds and default recovery.
- [ ] RED/GREEN type/date/category combination and deterministic immutable ID selection.
- [ ] RED/GREEN opening copies applied values, edits stay isolated, cancel leaves applied unchanged,
      Apply commits all dimensions once and Clear preserves query until/after Apply as specified.
- [ ] RED/GREEN Closing retains disabled render data, duplicate actions do nothing, onHidden releases
      it once and reopen is allowed only after hidden; late callbacks cannot cancel a new session.
- [ ] Saved-input round trip excludes draft and results; metadata/category changes do not reset IDs.
- [ ] Publish accepted canonical rules and close pending Stage 1 decisions with explicit evidence.
- [ ] Run focused tests, Native test compilation, Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*SearchFiltersTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Blocks Stage 2 structural Search reads and Stage 3 Search state. Product decisions remain visible gates;
do not claim a fully execution-ready contract while period/global-balance requirements disagree.
