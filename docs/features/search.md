# Search and filters

Search is a shared Android/iOS primary destination over persisted movimientos and transfers in one
selected currency, the primary one by default.
The user accepted the Android flow and follow-ups; the reviewed implementation is integrated into `main`.
[Business logic](../business-logic.md#search-and-filters) owns matching, date and filter rules;
[UI architecture](../ui/architecture.md#search-presentation) owns presentation and lifecycle contracts.
The [screen](references/search-screen.png) and [filters](references/search-filters.png) are composition
references; their sample data and quantity-bearing Apply button are not requirements.

## Integration

- SQLDelight structural predicates produce movimiento and transfer rows and currency years in one
  transaction-backed `MovimientoSearchSnapshot` from `ActividadRepository.observeFiltered`.
  Categories, tags and accounts come from their repositories. Text matching uses
  the shared normalization utility and injected computation dispatcher, without resubscribing for
  each character. Cancellation and generation/revision guards prevent stale publication.
- SearchViewModel owns raw input, applied criteria, transactional editing/closing sessions and
  `RetainedLoadState<SearchResultsUi, SearchFailure>`. Results own the exact visible count; controls
  stay live while delayed feedback applies only to the result region. Pending status shows Loading.
- Decorated Navigation 3 entries retain the actual SavedStateHandle and ViewModel owner. Retained
  entry content reads the current provider to keep presentation/focus inputs fresh. Home entry
  reveals/focuses the field once; tab return retains position. Creation remains shell-owned, with
  modal exclusion and origin-checked callbacks.
- The filter sheet uses 72% of available height, a scrolling body and fixed footer. Clear commits
  default filters, closes and preserves raw text. Custom shows labeled From/To controls with inclusive
  opposite-endpoint bounds and today's cap; legacy reversed saved input keeps normalization.
- Shared `TallybookChoicePill` and `TallybookFilterTag` preserve 48dp interaction targets with smaller
  visuals and no press shading. Shared movement-row amounts and skeletons follow the
  [global card typography](../ui/architecture.md#movement-card-amounts); monetary formatting is unchanged. Reusable APIs live in the [catalog](../ui/components.md).
- Tag filters reach SQL through `MovimientoReadCriteria.etiquetaIds`. SQLDelight `IN :list` cannot
  express "no filter", so `ActividadRepositoryImpl` picks one of four named queries (none, category,
  tag, both); account and medio filters use a flag argument next to their list, and transfers are
  read only while `MovimientoReadCriteria.includesTransferencias` holds. The filter sheet shows a Tags section of `EtiquetaChip`s after the
  categories while the user has tags; applied tags render as `TallybookFilterTag`s with a `#` icon.
- Results are grouped under one full-date header per day with results; rows show title (category
  fallback) and description, without date or tags, and transfers use `ActividadTransferenciaRow`,
  which opens transfer management. Accounts' "View movements" arrives as a `SearchFilterRequest` with
  the account and its currency.
- Real result rows open [movimiento management](manage-movimientos.md); its mutations refresh the
  existing repository projection while retaining Search input and criteria.
- No chart, category authoring, sync, FTS, pager or dependency upgrade is included.
  The unified Custom range selector is deferred to [the backlog](../backlog.md).

## Verification boundaries

The baseline passed 337 shared host tests. The final reviewed tree passed 121 focused Search,
navigation/modal lifecycle, calendar geometry, History and movement-row host tests with zero failures,
errors or skips, plus Detekt, ktfmtCheck, Android production/test APK builds and Native production
compilation. Two independent final reviews covered the data/state and UI/component boundaries.
Review fixes align Home's Search press feedback with the shared surface and bound calendar width to
safe viewport margins while preserving day targets through horizontal grid scrolling.

The current production APK was installed and launched on one authorized Xiaomi M2012K11AG, API 33.
Physical checks cover search/clear/debounce feedback, independent filter application/removal, draft
cancellation, Clear-and-close, Home-entry focus/tab retention, partial sheet/footer reachability,
landscape calendar scrolling, compact tags, choice-pill press shading and smaller movement amounts.
The final calendar fixture ran on the Xiaomi with local 320dp and 360dp window constraints: popup
widths were 288dp and 328dp, and horizontal grid scrolling exposed full 48dp Saturday/Sunday targets
with pointer selection. These are isolated component constraints, not a physical screen-size change.
The final production smoke repeated text/clear, draft cancellation, type application/removal,
Clear-and-close and Home-focus/tab-return checks against the existing 16 movimientos. Home press
feedback and the shared History balance were visually checked; no production records were changed.
Custom checks cover visible endpoint labels, rejected inverted/future selections, same-day apply and
reopening unchanged. Device rotation and keep-awake settings were restored.

Device tests: the full instrumented suite passes on the Xiaomi (1228/1228), including the isolated 2,000-row scale fixture, which asserts
complete counts, scrolling and subscription lifecycle. Its latency and memory are recorded, not held
to a budget, so no Search performance claim is made; the budgets are tracked in the backlog.
Native compilation does not establish physical iPhone runtime behavior; no authorized iPhone was
available. No production data was seeded. Future checks follow
[proportional verification](../README.md#proportional-verification).

## API evidence

- [SQLDelight 2.3.2 arguments](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/query_arguments/)
  supports bound scalars and collection IN; repository fixtures cover empty/many-ID predicates.
- [SQLDelight 2.3.2 transactions](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/transactions/)
  provides transactionWithResult for coherent row/metadata reads, covered with real-driver fixtures.
- [SQLDelight 2.3.2 observable queries](https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/coroutines/)
  provides query Flow updates; repository fixtures cover invalidation and cancellation cleanup.
- [Navigation3 decorated entries source](https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-runtime/src/commonMain/kotlin/androidx/navigation3/runtime/DecoratedNavEntries.kt)
  caches provider entries by back-stack keys. Resolved runtime 1.1.1 bytecode confirms this behavior;
  retained content reads the current provider through a copied entry, preserving decorated owners.
  The host lifecycle integration fixture exercises changing scalar presentation inputs with retained
  keys; Home-entry focus has additional physical keyboard evidence.
- [NavEntry copy constructor](https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-runtime/src/commonMain/kotlin/androidx/navigation3/runtime/NavEntry.kt)
  preserves key, content key and metadata while supplying current content.
- [Compose focus](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior)
  requires the attached focus target before requesting focus. Home-entry intents wait for resumed
  content and a frame; ordinary Search-tab selection has no focus intent.
- [Compose window information source](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/WindowInfo.kt)
  provides container size for a safe-height, scrollable calendar without reducing day targets.
- [Unstyled 2.9.0 BottomSheet source](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/BottomSheet.kt)
  clamps measurement to caller constraints and anchors the measured sheet at the bottom. Search
  supplies a 72% fill modifier through the existing modal layout, retaining its bounded body/footer.
  Physical checks cover height and footer reachability for this caller policy.
- [Compose selectable source](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/selection/Selectable.kt)
  supports an explicit null indication. Resolved foundation 1.11.2 exposes the source/indication
  overload; the shared choice pill disables inherited press shading without changing selection.

Resolved execution versions include Kotlin 2.4.10, Compose Multiplatform 1.11.1, Android Compose
foundation/UI 1.11.2, Android Material3 1.5.0-alpha17, Navigation3 1.1.1, lifecycle 2.11.0-beta01,
Koin 4.2.2 and SQLDelight 2.3.2. No dependency upgrade is part of this change.

## Custom range-picker feasibility

The existing `TallybookDateChipPicker` selects one day; its calendar/year helpers do not implement a
range transaction. The installed Material3 offers
[DateRangePicker](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-date-range-picker.html)
and [rememberDateRangePickerState](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/remember-date-range-picker-state.html)
with both UTC-midnight endpoints, initial displayed month, configurable years and `SelectableDates`.
An isolated commonMain compile probe with both endpoints, year range and future-date restriction
passed Android and Native compilation against the resolved versions, without dependency changes.

A reusable range field could wrap this existing calendar in one popup/dialog. Keep the existing
domain validation and editing session; translate civil dates through UTC midnight, extend the years
to persisted/restored dates, disable future days, and update both draft endpoints only after a complete
selection. Popup cancellation must discard the provisional range. Styles, compact-screen Back/focus
and physical iPhone behavior still need integration evidence before replacing Custom. No calendar
engine or new dependency is needed; this analysis does not assert that replacement has shipped.
