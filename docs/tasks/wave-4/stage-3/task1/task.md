# Stage 3 / Task 1 — Pure Search screen, skeletons and filters

Read [Wave 4](../../README.md), canonical presentation/loading rules, wording and component catalog.
Planning only. Depends on Stage 2; coverage A7/A8/A12 and UI portions of A4/A5/A11.

## Goal and ownership

Implement reference composition using shared primitives; content consumes state/Actions only.
Proposed files under `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/ui/`:

- `SearchScreen.kt`: header/input/chips/list/empty/error composition with root Modifier.
- `SearchFilterSheet.kt`: sheet shell with independently scrolling body and reachable footer.
- `SearchCategoryFilters.kt`: real parent-grouped category selection/fallback chips.
- `SearchResultRow.kt`: thin date/title/category adapter over MovimientoRowContent.
- `SearchScreenPreviews.kt`: deterministic ready/empty/error/loading and light/dark variants.

Own additive shared changes only where needed:

- `core/ui/component/TallybookDateChipPicker.kt`: optional minimum year, bounded lazy/scrollable year
  selector and caller value-label support for full year display; defaults preserve existing callers.
  Cap visible year rows with a theme dimension, reveal the selected year once and keep older years
  reachable without an unbounded grid. Validate focusable popup exclusion before
  deciding whether it needs a small visibility coordination callback. No second calendar system.
- `core/ui/category/CategoryChip.kt`: optional enabled parameter, default true; Closing cannot toggle.
  Reuse existing selected/tint semantics. Do not redesign the general category picker.
- `core/ui/theme/{Dimensions,Spacing,Motion,...}.kt`: new semantic tokens only when existing ones fail.
- All four current `composeResources/values*/strings.xml`: ES, ES-AR, EN, PT-BR copy/plurals and semantics.
  Preset/type labels map typed values to resources at the UI boundary.

Proposed device tests: `feature/search/ui/SearchScreenTest.kt`, `SearchFilterSheetTest.kt`; extend
`core/ui/component/TallybookDateChipPickerTest.kt` for earlier-year/date-label defaults and popup layering,
and CategoryChip test coverage at its existing seam. Use one grouped test fixture per meaningful seam.
Task 2 owns route/navigation wiring; this task does not add a destination or another creation route.

## Rendering contract

SearchScreen receives SearchUiState, SearchActions, creation/filter requests, bottomBarClearance and
root Modifier. It owns LazyListState in composition; preserve one saveable content scroll owner.
Result-feedback adapter observes result status; it cannot delay live input/chips/actions. Static
first-read skeleton uses four existing MovimientoRowSkeleton rows and localized count-line loading text;
sample data/amounts have no semantics. Use shared progress indicator for sustained local work only.

Header/input/chips and rows share the lazy screen composition. Pending/failed current count is absent
with reserved line height; settled zero is real zero. Applied chips have date/type/category stable keys,
explicit remove descriptions and deterministic category ordering. Filter button conveys active and
expanded status, not only a colored dot. No-data offers the shell creation callback; no-results offers
clear text/open filters without implicitly clearing all inputs. Query-dependent empty copy uses the
settled query, never newer pending text. Rows are read-only with no click/chevron/edit semantics.

Known categories are ordered parent groups with children following persisted order; orphan categories
remain visible. All 58 real catalog entries are reachable, no screenshot list or type-based pruning.
Missing selected IDs have localized fallback/remove controls. Exact parent-ID selection is visible;
labels must not imply a whole category subtree is selected. Names are rendered/searched using the same
localized source generation. Unknown-category fallback does not show raw IDs.

Editing sheet renders draft controls; Closing renders the captured read-only draft until onHidden.
All category/preset/type/date/clear/apply controls disable during Closing, including open popups.
Apply has exact localized count-free label, remains enabled for valid unchanged/zero-result drafts,
and only validation prevents an Apply commit. Limpiar commits defaults and closes; no discard-confirmation dialog.

Use 72% of available height and compact centered preset chips with minimum 48 dp touch targets.
Keep Limpiar/Aplicar outside the scrolling body with the modal's existing safe/IME policy. Compose the
fixed footer within the current modal content slot; extend the shared modal API only if a measured
layout constraint requires it. Scroll/body weight and content-detent measurement must be tested with
small catalogs and all 58 categories; an unbounded weighted Column is not a valid fix. Changing shared
modal measurement is a material boundary requiring regressions for existing creation/category sheets.

Custom uses two existing single-date popups with maxDate=today. Minimum reachable year is the minimum
of shared 2019, relevant persisted nonfuture years and valid applied/draft endpoint years, preserving
access if earlier rows disappear during an editing session. Show full year labels/cross-year range,
and full accessible civil dates. Switching to another preset or Closing dismisses an open popup.
One focusable popup blocks opening the other; confirm actual layering/Back/focus with Unstyled modal.
Only introduce narrowly scoped coordination if actual integration shows both can be active.

If the selected date becomes future after a backward clock change, keep the field's invalid date label
but clamp its initially visible popup month to maxDate for correction. Preserve existing default
behavior for valid dates; this UI reveal does not mutate or silently normalize applied filters.

## Scroll, focus and accessibility

Temporary loading/error content uses unsaved list state and cannot overwrite/clamp saved content scroll.
When current criteria/text settles as a new product query, reset to top once; tab return, revalidation,
category refresh and saveable restoration keep position. The identity/reset acknowledgment belongs
in the route/composition seam; no ViewModel holds LazyListState or runs frame-clock scroll animation.

48 dp targets, selected semantics and predictable reading order are mandatory. Long descriptions,
labels and high-value/currency amounts wrap with the shared row layout rather than fixed screenshot
widths. Light/dark theme and enlarged text use tokens/font measurement. Footer remains reachable in
compact landscape and with IME; shell bottom clearance is applied once on the screen and independently
through existing modal safe-area policy. Announce one settled count politely, avoid keystroke chatter.

## Test-first slices / exit

- [ ] Pure screen renders current count, pending no-count, header/input/chips, exact row labels and
      distinct no-data/no-results/error; clear/removal/retry/creation callbacks remain isolated.
- [ ] Fresh slow-read skeleton semantics appear at 200 ms; fast read/error cancels them; first read
      loading text replaces numeric count; same-request cached empty return has no skeleton.
- [ ] Live query/chips update during delayed feedback; old result identity cannot leak after criteria
      change; text debounce has no spinner or old empty assertion; sustained match feedback settles.
- [ ] Filter exact presets/type/multiple IDs, unchanged/zero-result Apply, Clear/cancel, quantity-free
      visible and accessible Apply label; Closing controls reject interaction until hidden.
- [ ] All 58 categories/missing-ID chips, real category tints, large text/compact height/footer scroll.
- [ ] Two custom popups, max today, earlier-year floor including persisted 1900 and a compact-screen
      scroll/select of that year, future-invalid value with valid correction month, year labels,
      Back to popup then sheet, focus
      return and switching away from Custom. Regression proves existing picker defaults are unchanged.
- [ ] Deterministic previews using fixed dates/money/labels and light/dark theme; physical comparison
      follows Task 2. Preview compilation does not constitute visual acceptance.

Through gradle-run: focused host presentation/layout regressions and `./gradlew detekt ktfmtCheck`,
then `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosArm64`. Discover actual device-test
build/runner tasks in Stage 3 Task 2; do not reuse guessed old commands. Device UI tests run only on a
confirmed physical device. Exit requires pure rendering/callback contracts plus tested popup/footer
integration; final navigation/creation/lifecycle evidence is owned by Task 2.
