# Wave 3 / Stage 1 / Task 1 — Period and product contract

## Objective and context

Implement the accepted deterministic temporal contract and pure rules used by History: three scopes
Day/Week/Month, year context and month-bound week blocks. No text search or fourth Year scope. Read the [wave contract](../../README.md), `docs/business-logic.md`,
`docs/tasks/period-picker.md`, the current picker builders and their tests before editing.

Skills: `android-dev`, `using-chrisbanes-skills`, `kotlin-api-design`, `kotlin-control-flow`,
`android-testing`, `test-driven-development`; use `gradle-run` before wrapper execution.

## Files and responsibility

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/MovimientoDateRange.kt`:
  framework-free validated inclusive `start`/`end` civil-date value.
- Create adjacent `MovimientoPeriodRanges.kt`: the shared framework-free period boundaries consumed
  by both History selection and core UI picker adapters. It must not import a feature or Compose.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistorySelection.kt`:
  internal scope/anchor selection and pure transition/range rules.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookPeriodSelection.kt`
  only to share the accepted range calculation instead of implementing it twice.
- Test in `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/MovimientoDateRangeTest.kt`
  and `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistorySelectionTest.kt`;
  update the existing `TallybookPeriodSelectionTest.kt` when required by the accepted contract.
- Update `docs/business-logic.md` for accepted History periods, separate future Search destination and
  global/period total semantics without implementing or defining Search filters; update
  `docs/tasks/period-picker.md` for the week wording. Keep UI policy in `docs/ui/architecture.md`.

## Consumed and produced contracts

- Consumes injected `today: LocalDate`, caller-provided years and the existing picker behavior.
- Produces `MovimientoDateRange(start: LocalDate, end: LocalDate)` with `start <= end`.
- Produces internal `HistoryScope` and `HistorySelection(scope, anchor)`; range/context are derived.
- Produces pure scope, year, month and period-selection transitions. Other tasks consume these names
  and invariant meanings; local helper names may be chosen during implementation.
- Saved/raw date inputs are validated before constructing a range; invalid restored input follows the
  documented recovery rule. Repository methods consume already-valid ranges and validate remaining
  criteria at their boundary. Do not add raw-range overloads or invalid sentinels. UI builders do not
  silently repair reversed intervals.

## Specific approach

1. Publish the already accepted rules in the canonical business document: Week uses blocks 1–7,
   8–14, 15–21, 22–28 and 29–month end, never crossing months; three scopes plus a year selector,
   no fourth Year scope. Current-month Day/Week options and History totals end at today; historical
   months are complete. Keep full calendar-month picker representation distinct from the today-clamped
   History read range. Align the stale “calendar week” wording in `docs/tasks/period-picker.md`.
   Retain future Search/charts as later capabilities, not History text/filter state or this wave work.
   The compact global balance stays separate and always visible; it is not the period balance.
2. Keep domain ranges independent of Compose. UI picker values adapt to domain ranges; do not move
   `@Immutable` into `core.model` or let repositories import `core.ui.component`.
   Shared calendar calculation lives in `core.model.MovimientoPeriodRanges`; History transitions
   and UI picker builders each adapt its date ranges. `core.ui` must never import `feature.history`
   to reuse range calculation. Keep the internal History scope enum feature-owned.
3. Maintain one selection input. Context month, active year, available periods and selected chip are
   derived. A historical-month scope transition must preserve the intended anchor, not reset to today.
   Scope changes preserve anchor. Selecting a different Week chip moves anchor to that block's start;
   tapping the already-selected chip is a no-op and preserves its interior anchor. Month-chip selection
   uses the existing month transition, preserving/clamping the valid day rather than forcing day 1.
4. A range is inclusive, based on `Movimiento.fecha`; do not convert dates to UTC instants for SQL.
   `createdAt` remains only the existing stable secondary order, not the grouping/filtering date.
5. Capture now and the timezone once per date-refresh operation. On resume, if the anchor represented
   the previous today, follow the new today; historical selection stays fixed unless now in the future.
   New years become available without relying on a midnight timer in the background.
6. Define small serializable saved input: schema version, scope and ISO anchor. Recompute
   derived state against current today/currency. Unknown versions/scopes and malformed/future anchors
   recover deterministically to Day/today; never deserialize rows. A valid past
   year absent from current metadata remains selectable for this entry, following the retention rule.
   Keep raw-date validation independent of restoration transport. The early S5.T1 real saved-owner/
   Koin probe must pass before S3 selects transport. Android supported saved-owner recreation and
   iOS retained-controller/saveable scope are the restoration boundaries; fresh cold launches use
   normal defaults, with no iOS cross-process bridge or extra persistence. Save no query field.

## Test-first slices

- [ ] RED: reversed range is rejected; a one-day range is valid. GREEN: implement the value boundary.
- [ ] RED: exact range expectations for Feb 2024/2025, 30/31-day months, today on day 1 and month end,
      and the last truncated block. GREEN: centralize the accepted range builders.
- [ ] RED: Day -> Week -> Month preserves anchor; year/month changes clamp Jan 31 and Feb 29 correctly.
      GREEN: implement pure selection transitions.
- [ ] RED: different Week-chip selection anchors its block start; selected Week-chip tap preserves an
      interior anchor; Month-chip selection preserves/clamps valid day. GREEN: explicit chip transitions.
- [ ] RED: resuming after midnight/year rollover advances only a live-today selection; backward clock
      and timezone changes never leave a future anchor. GREEN: implement date-refresh policy.
- [ ] RED: valid saved input round trip plus malformed/future/unknown-version recovery. GREEN: implement
      input validation without saving any repository data.
- [ ] Confirm the catalog and existing picker tests use the same boundary rules.
- [ ] Run focused host tests, Native test compilation, Detekt and ktfmt; attach outcomes before review.

Validate: `./gradlew :shared:testAndroidHostTest --tests '*MovimientoDateRangeTest' --tests '*HistorySelectionTest' --tests '*TallybookPeriodSelectionTest'`.
Then `./gradlew :shared:compileTestKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Completion / blockers

Complete when exact temporal behavior and restoration inputs are accepted and tested. Blocks Stage 2
and Stage 3. The product choices are settled; no redundant approval is required. Early S5.T1 transport evidence
remains an engineering gate before S3, not a blocker for this pure task. S1.T3 Search is deferred.
