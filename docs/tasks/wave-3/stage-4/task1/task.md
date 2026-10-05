# Wave 3 / Stage 4 / Task 1 — Pure History screen and shared row migration

## Objective and context

Implement the smallest readable History surface for the [wave contract](../../README.md), using the
existing picker/search/theme components. Requires reviewed Stage 3. This task does not own navigation.

Skills: `android-dev`, `compose`, `compose-state-and-effects`, `compose-component-design`, `android-ux`,
`compose-ui-testing-patterns`, `android-testing`, `test-driven-development`, `gradle-run`.

## Files and responsibility

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryScreen.kt`:
  pure UiState plus Actions and a small separate add callback; no Koin/ViewModel/Flow dependencies.
- Create adjacent `HistoryPeriodControls.kt`, `HistorySummary.kt` and `HistoryScreenPreviews.kt` only
  where they own independent composition roles; avoid one component per trivial text node.
- Modify `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/movimiento/MovimientoRow.kt`
  to use Tallybook tokens/components while preserving its existing public Home API.
- Extract its shared visual renderer into `core/ui/movimiento/MovimientoRowContent.kt` only as needed
  for Search's different title/subtitle. Prefer typed content parameters over an `isSearch` flag;
  retain Home's existing category-first wrapper. Both wrappers reuse one avatar/amount/layout renderer.
- Modify theme token files only for measured missing roles; reuse exact existing tokens first.
- Add strings in `shared/src/commonMain/composeResources/values/strings.xml` (Spanish baseline),
  `values-en/strings.xml`, `values-es-rAR/strings.xml` and `values-pt-rBR/strings.xml` under the same
  resource root. Do not invent `values-es` or change the existing fallback-language structure.
- Create `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryScreenTest.kt`;
  extend `core/ui/movimiento/MovimientoComponentsTest.kt` and existing resource tests as needed.

## Composition and interaction contract

- Header: History title and explicitly labelled overall balance/currency. Keep the overall summary
  visible while the list scrolls; if it fails, show its localized unavailable/retry state.
- Period area: contextual title/year menu, neutral scope control and selected-period chip strip.
  Keep controls reachable during loading/empty/error states so changing a filter can recover.
- Search: `TallybookSearchField`, visible query and accessible clear action. It does not auto-focus
  on ordinary History entry, restoration or period changes. IME search/done hides the keyboard.
- Content: labelled period balance/income/expense, result context, then one lazy list with date headings
  and read-only rows. Avoid nested vertical lists and eager composition of all rows.
- Empty period explains that period has no data and allows using the existing add action. Do not
  claim the entire database is empty without a separate query proving it.
- No-results state keeps period totals and offers clear search; it does not replace them with zeros.
- Loading/matching/error state follows request identity from UiState. Retry preserves user input;
  stale content, if retained, has explicit status and never an unmarked success appearance.
- The screen emits callbacks; it does not calculate ranges/totals, catch storage exceptions, mutate
  the sheet, read Clock or instantiate a ViewModel.

## Visual, accessibility and localization approach

1. Follow `docs/ui/architecture.md` for portrait-first layout, exact tokens, motion, safe areas and
   component ownership. Do not independently redesign Home or port Moneta's chart/breakdown.
2. Produce deterministic empty/content/no-results/loading/error previews with fixed dates/amounts.
   Use these to review the History composition before polishing it on device. Preview availability is
   not evidence of acceptance; record any user-facing composition decision requiring review.
3. Migrate the shared row's Material Surface/Text/spacing/border to established Tallybook components.
   Preserve signed amounts, category fallback and non-interactive semantics for both consumers.
   Add semantic currency text where a symbol is ambiguous, without converting monetary values.
4. Render localized dates and month/year labels from civil dates. Use the existing localized strings
   and compatible datetime formatting; no Java date formatter or copied English month names in
   commonMain. Long labels and localized number formatting must wrap without covering the amount.
5. Minimum targets are 48 dp. Announce scope/year selection, date headers, matching count and failure
   meaningfully; avoid announcing every keystroke twice. Do not rely only on red/green for money type.
6. Keep enlarged text, long descriptions, tablet widths and compact landscape usable. Test TalkBack/
   VoiceOver order, row grouping, search clear and year popup dismissal/back behavior.
7. Screen content consumes the shell's published bar clearance. Final inset ownership moves in Stage 5;
   do not add a permanent second bar or double-safe-area workaround to make isolated previews fit.

## Test-first slices

- [ ] Pure screen tests: exact visible states, retained controls, callback arguments, empty period versus
      no results, clear/retry, selection and read-only rows. Expected RED is the missing UI contract.
- [ ] Implement content from state and callbacks; add fixed previews and localized resources.
- [ ] Row regression tests verify Home API, signed Money/currency, fallback and meaningful semantics
   after the targeted token migration; Search's separate title/subtitle must not replace Home's copy.
- [ ] Verify focus/IME actions and year popup using semantics; tags only for genuinely ambiguous nodes.
- [ ] Compile device tests with the exact discovered AGP task; execute only on a confirmed physical
      device. A stalled/unavailable runner is unconfirmed execution, not a pass.
- [ ] Run host regression, Android build, Native production compilation, Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`.
Then `./gradlew :androidApp:assembleDebug`; discover instrumentation compile/run tasks before use.

## Completion / blockers

Complete when pure rendering contracts and resources are implemented with build/test evidence.
Blocks task2. Final keyboard/inset/visual acceptance belongs to Stage 6 on physical targets, not a claim
made from previews or host compilation.
