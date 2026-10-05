# Wave 3 / Stage 4 / Task 1 — Pure History screen and minimal shared row renderer

## Objective and context

Implement the smallest readable [History reference](../../references/history-screen.png) composition
under the [wave contract](../../README.md), after reviewed Stage 3. This task owns no navigation.
Reference counts/data/colors do not become production defaults; no seeding or excluded breakdown.

Skills: `android-dev`, `compose`, `compose-state-and-effects`, `compose-component-design`, `android-ux`,
`compose-ui-testing-patterns`, `android-testing`, `test-driven-development`, `gradle-run`.

## Files and responsibilities

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryScreen.kt`:
  pure immutable UiState/Actions plus add callback/root modifier, no Koin/ViewModel/Flow dependency.
- Create nearby `HistoryPeriodControls.kt`, `HistorySummary.kt`, `HistoryScreenPreviews.kt` only for
  independent composition roles, not one file for each text node.
- Modify `core/ui/movimiento/MovimientoRow.kt` under the same commonMain package root to use Tallybook
  tokens/components, preserving public `MovimientoRow(movimiento, categoria, modifier)` Home API.
- Extract `core/ui/movimiento/MovimientoRowContent.kt` only as needed by the real Home/History
  consumers. Typed primary/secondary text roles share avatar/amount/layout; no isHistory flag or
  generic mapper. Small History wrapper supplies description/category-fallback title and date subtitle.
- Reuse existing period header/year menu/neutral segmented control/period row. Modify these components
  only for demonstrated missing semantics/context-change visibility, preserving existing callers.
- Theme changes only for a measured missing role; 14/12 row spacing, gap/border/avatar roles already
  have exact reusable tokens. Preserve CategoryAvatar shape; no new History token family.
- Add strings in `shared/src/commonMain/composeResources/values/strings.xml` (Spanish baseline),
  `values-en/strings.xml`, `values-es-rAR/strings.xml`, `values-pt-rBR/strings.xml`.
- Create `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryScreenTest.kt`;
  extend `core/ui/movimiento/MovimientoComponentsTest.kt` and resource regressions as relevant.

## Composition, state and callbacks

- Keep compact explicitly labelled overall balance/currency outside list scrolling, with honest
  loading/unavailable/retry. Selected-period totals have a separate label and status.
- Use one vertical LazyColumn for contextual header/year menu, scope/month supporting text, period
  count, neutral three-scope control, horizontal chip strip, compact period summary, date headings
  and keyed read-only rows. Controls remain reachable in compact height, large text and errors.
  Count is the full selected-period row count, not a settled-match/predicted filter count.
- Visible title is localized day, week block dates or month; a static History label may supply accessible
  screen context but does not replace the reference hierarchy. Reuse TallybookPeriodPickerHeader,
  TallybookYearMenu, TallybookSegmentedControl(style = Neutral), TallybookPeriodPickerRow.
- History-owned compact TallybookSurface/Text summary shows period balance/income/expense with
  responsive wrap/stack. Do not reuse the tall Home BalanceCard or build a generic balance widget.
- Each row uses nonblank description as primary text, otherwise category/fallback; date subtitle,
  category avatar/context and exact signed Money/currency. Keep stable IDs/date groups and storage
  order. No click/chevron/edit semantics until detail exists. Home stays category-first.
- Empty state refers to this period and uses the existing add callback; do not assert globally no data.
  Loading/error/stale status follows UiState request identity; retry retains selection. Unavailable
  totals are not zero. No search/no-results/matching/filter/clear-query/IME input belongs here.
- UI emits actions and formats civil-date presentation; it never reads Clock, calculates totals/ranges,
  catches storage exceptions, owns creation session or instantiates a ViewModel.

## Accessibility, layout and locales

1. Follow canonical architecture/tokens/safe areas. Reference saturated colors/blue outline/rounded
   avatar do not justify palette or shape changes. No chart/category breakdown/analysis tabs/eye action.
2. Fixed-date/amount light/dark previews cover content/empty/loading/read/calculation/global-summary
   error and large text. Preview compilation is not physical acceptance.
3. Shared row migration uses Tallybook components/tokens; preserve Home copy/signed currency and
   accessible type meaning. Long title/large amount must not overlap; allow growth/wrap/stack with
   deliberate visual truncation while retaining useful semantic text.
4. Localized month/year/civil dates use supported common APIs/resources. No Java formatter/copied
   English month names. Date headings/context title are headings; period/year selected semantics
   describe full date ranges/month/year and all targets remain at least 48 dp.
5. Merge read-only row semantics with title/date/type/currency/category meaning; decorative avatar
   does not become a separate action. Do not rely on colors for income/expense or selected periods.
6. Test same-index option changes across month/year so selected chip reveal remains correct. Preserve
   scroll unless accepted selection change requires revealing the selected chip; no blanket reset.
7. Consume shell-published bar clearance; no permanent second bar or doubled bottom safe inset.
   Test popup dismissal/focus/Back, tablet width, compact landscape and enlarged text on real targets.

## Test-first slices and checks

- [ ] RED/GREEN exact loading/content/empty/error/global-error states, retained controls, callback
      arguments/year/scope selection, period count, stable date/row keys and read-only semantics.
- [ ] Add pure content, deterministic previews and all supported localized resources.
- [ ] Home row API/copy regression plus History description/date/fallback/sign/currency/long-text
      semantics; one shared renderer with no cloned amount formatter.
- [ ] Year popup semantics/dismissal and selected-chip context change visibility; use tags only where
      nodes are genuinely ambiguous. No search focus/IME tests are added to History.
- [ ] Compile device tests with `./gradlew :shared:compileAndroidDeviceTest`;
      `:shared:assembleAndroidDeviceTest` assembles the test APK. Execute
      `:shared:connectedAndroidDeviceTest` only against one confirmed authorized physical device.
- [ ] Host regression, Android build, Native production compilation, Detekt/ktfmt and fresh review.

Validate: `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`.
Then `./gradlew :androidApp:assembleDebug :shared:compileAndroidDeviceTest`.
Unavailable/stalled device execution remains explicitly unconfirmed; no emulator/simulator.

## Completion and dependencies

Pure rendering/resources/callbacks have current build/test evidence and Home remains compatible.
Reviewed Stage 4 enables full S5.T1 shell integration. Physical visual/accessibility/keyboard-in-sheet/
inset acceptance remains Stage 6; deferred Search UI does not block this stage.
