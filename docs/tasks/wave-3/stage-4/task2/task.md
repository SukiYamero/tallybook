# Wave 3 / Stage 4 / Task 2 — Search screen and transactional filter sheet

> **DEFERRED — separate later Search destination.** This file preserves useful future proposals,
> not an active Wave 3 task. Its original checklist, dependencies, shared-file permissions and
> unresolved Search decisions are inactive. Rebase them against delivered History/canonical rules
> in a separately authorized Search plan before execution. It neither blocks nor enables History;
> no Search UI, filter/criteria, structural SQL, projection, DI or navigation work is authorized now.
> Use the [deferred Search index](../../README.md); re-plan dependencies before execution.


## Objective and context

Implement the user-supplied Search composition and confirmed Apply semantics from the
[deferred Search proposals](../../README.md). Requires Stage 3 and task1 shared-row rendering. The reference images
are [Search](../../references/search-screen.png) and [filters](../../references/search-filters.png).
Moneta provides behavior/data context but its live filtering and count-bearing CTA are overridden.

Skills: `android-dev`, `compose`, `compose-component-design`, `compose-state-and-effects`,
`android-ux`, `compose-ui-testing-patterns`, `android-testing`, `test-driven-development`, `gradle-run`.

## Files and interfaces

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/ui/SearchScreen.kt`,
  `SearchFilterSheet.kt`, `SearchCategoryFilters.kt` and `SearchScreenPreviews.kt` by composition role.
- Reuse the shared row renderer from task1; a small Search row wrapper supplies description-first
  title and date/category subtitle. No Home row API/behavior change and no cloned amount formatter.
- Extend existing theme tokens and locale resource files only for new semantic roles/copy.
- Create `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/search/ui/SearchScreenTest.kt`
  and `SearchFilterSheetTest.kt`; extend current date/sheet/component regressions when relevant.

Pure SearchScreen consumes `SearchUiState`, `SearchActions`, open-filter/add callbacks and a root
Modifier. Filter content consumes the Editing draft or disabled Closing render snapshot, categories
and callbacks, never a ViewModel, SQL query or predicted count. Visibility derives from phase;
composition owns motion/focus and forwards onHidden; state actions own the session values.

## Specific composition

1. Header: localized Buscar movimientos title, exact settled count below, accessible filter icon
   with derived active indicator. Reconcile compact global-balance policy from Stage 1 without
   guessing missing results or changing the approved count-free footer.
2. Search input: description/category intent, leading icon, accessible clear. Text input remains
   stable in size; query/IME behavior follows shared components. No focus loop on recomposition.
3. Applied chip strip: date/type/category chips reflect committed filters, scroll horizontally if
   needed and expose explicit remove semantics. It never renders tentative draft values as applied.
4. Results: lazy stable-ID rows, category avatar, nonblank description title (category fallback),
   date plus category subtitle, signed exact amount and unambiguous currency. No chevron/click until
   movement detail is implemented. Loading/error/no-data/no-results follow real state readiness.
5. Sheet sections follow the guide: date presets, conditional custom endpoints, Todos/Gastos/Ingresos,
   category chips and footer. Layout wraps based on actual width/text; it does not fix image dimensions.
6. Category chips come from all real persisted categories, including children; compact avatar/tint
   presentation reuses category semantics. Group/order meaningfully without changing exact-ID selection.
   Do not restyle the entire existing category picker or introduce hardcoded screenshot categories.
7. Custom date labels include year when needed and complete accessible civil dates. Two calendar
   popups open one at a time above the sheet, with correct scrim/Back/focus return and maxDate. Verify
   actual popup layering for Compose Unstyled 2.9.0 rather than adding a duplicate calendar system.
8. Footer: secondary **Limpiar**, primary **Aplicar**. Primary contains no quantity, result preview,
   dynamic label, `Aplicar (N)` or quantity-bearing accessibility name. Use localized resources in all
   supported locales. Apply is enabled for valid unchanged filters and zero matches.
9. Filter body scrolls while footer stays reachable above safe area/IME, with 58 categories, enlarged
   text and compact landscape. The app theme owns every static dimension/color/motion value.
10. Back/scrim/drag cancellation invokes cancel before hidden cleanup; Apply invokes commit once then
    closes; Clear edits only the draft. Keep disabled render data through hide, release onHidden and
    reject reopen/creation before hidden. No financial-data discard dialog for temporary filters.

## Accessibility and state boundaries

- All targets are at least 48 dp, selected categories/types have meaningful semantics, chip removal
  and filter-button active/expanded status are announced without relying on dots/colors.
- Keep underlying screen results/count unchanged while editing; announce committed changes after
  Apply settles. Pending text matching never announces an old header count as current.
- Keep rows available through scrolling with large text, long labels, safe areas and dark theme.
  Contrast/type meaning must not rely solely on amount color.
- Route/modal owner receives lifecycle restoration events. Saved applied inputs/scroll survive;
  an open draft/keyboard does not automatically reappear after process recreation.
- Creation is shell-owned and exclusive with filter modal. This task emits requests, not a second
  AddMovimientoSheetRoute. Shell coordination is tested in Stage 5.

## Test-first slices

- [ ] Pure Search screen RED/GREEN: title/count readiness, input/clear, applied chip removals,
      distinct empty/error/matching states, row title/subtitle and no detail click semantics.
- [ ] Filter content RED/GREEN: all presets/types/categories, custom visibility, selected state,
      Clear/Apply/cancel callbacks and zero-result/unchanged-draft Apply availability.
- [ ] Assert exact Spanish CTA Aplicar and no count in any CTA semantics; header retains result count.
- [ ] Wire draft edits and verify applied chips/count behind sheet stay unchanged until Apply.
- [ ] Drive hide animation and verify retained disabled content, exactly-once commit/cancel, no early
      reopen, onHidden cleanup and no filter/create modal overlap.
- [ ] Test custom calendar layering, cross-year labels, popup dismissal and focus return via semantics.
- [ ] Verify scroll/CTA accessibility with 58 categories, large text, keyboard and compact height.
- [ ] Produce deterministic light/dark previews; compare with supplied composition, accounting for
      native safe areas and accessibility. Do not claim acceptance from preview compilation.
- [ ] Compile device tests with discovered tasks, run on authorized physical device if available,
      then host regression, Android build, Native compile, Detekt and ktfmt.

Validate: `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosArm64 detekt ktfmtCheck`.
Then `./gradlew :androidApp:assembleDebug`; discover exact device-test tasks before using them.

## Completion / blockers

Complete when pure rendering and callback contracts match the supplied reference/approved editing
behavior. Blocks Stage 5. Physical acceptance and modal/lifecycle integration remain explicit later gates.
