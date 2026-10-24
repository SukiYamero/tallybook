# Wave 5 / Stage 5 / Task 1 — History transfer rows and Search account/medio/transfer filters

## Objective

Show transfers in History and Search, add Account and Medio filter dimensions plus a Transfer type to
Search, and open Search filtered by an account from the Accounts sheet.

## Context and skills

- Read [the wave README](../../README.md) → Display surfaces (History, Search) and Balances;
  `docs/business-logic.md` → Tracking and History, Search (draft/apply/clear, AND/OR, count from one
  snapshot, missing selections); `docs/ui/architecture.md` → History presentation, Search presentation
  (Loading and result currency); `docs/ui/components.md` → `TallybookFilterTag`.
- Skills: `android-skills:android-dev`, `android-skills:compose`,
  `chrisbanes-skills:kotlin-concurrency-and-flow`, `chrisbanes-skills:compose-state-and-effects`,
  `chrisbanes-skills:compose-ui-testing-patterns`, `android-skills:android-testing`,
  `superpowers:test-driven-development`.
- Current state: `HistoryViewModel` observes `observeRange(moneda, range)` and `projectHistory`
  requires every row to be a `Movimiento` in the requested currency; `SearchFilters.tipo` is a
  `TipoMovimiento?`; `MovimientoReadCriteria` drives four SQL variants in `Movimiento.sq`;
  `SearchFilterRequest(token, categoryIds, etiquetaIds)` is issued by `PrimaryNavigator.showSearchFilter`.

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/{Actividad,ActividadSnapshot,SearchTipo}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/{ActividadRepository,ActividadRepositoryImpl}.kt`
  (movimientos + transfers in one snapshot for History and Search)
- Modify: `core/model/{MovimientoReadCriteria,MovimientoSearchSnapshot}.kt`, `core/data/{MovimientoRepository,MovimientoRepositoryImpl}.kt` (move/retire the movimiento-only filtered read once Search consumes `ActividadRepository`), `sqldelight/.../Movimiento.sq` (filter queries), `Transferencia.sq` (range, year and filter reads — Stage 2 deliberately left them to this task), `di/RepositoryModule.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/FakeActividadRepository.kt`
- Modify: `feature/history/viewmodel/{HistoryViewModel,HistoryProjection}.kt`, `feature/history/ui/{HistoryDayGroupUi,HistoryScreen,HistoryScreenPreviews}.kt`
- Modify: `feature/search/viewmodel/{SearchFilters,SearchFilterSession,SearchObservation,SearchProjection,SearchSavedInput,SearchSavedState,SearchUiState,SearchViewModel}.kt`, `feature/search/ui/{SearchFilterSheet,SearchFilterRequest,SearchResultsUi,SearchResultRow,SearchDayGroupUi,SearchPresentation}.kt`; create `feature/search/ui/{SearchAccountFilters,SearchMedioFilters}.kt`
- Modify: `core/ui/navigation/{PrimaryNavigator,TallybookNavHost}.kt`, `feature/accounts/ui/AccountSheetHost.kt` (View movements)
- Modify: `feature/history/viewmodel/HistorySavedState.kt`, `feature/search/viewmodel/SearchSavedInput.kt` and both screens for the currency selector
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/history/viewmodel/{HistoryProjectionTest,HistoryViewModelTest,HistorySavedStateTest}.kt`, `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/search/viewmodel/{SearchFiltersTest,SearchFilterSessionTest,SearchSavedInputTest,SearchViewModelTest,SearchProjectionTest}.kt`, `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/feature/search/viewmodel/SearchSavedStateTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/{ActividadRepositoryTest,HistoryRepositoryTest,SearchRepositoryTest}.kt` (new + extend; keep the `EXPLAIN QUERY PLAN` assertions)
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/search/ui/SearchFilterSheetTest.kt`, `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/history/ui/HistoryScreenTest.kt` (extend); the scale fixture builders behind `{History,Search}VolumeDeviceTest`

## Contracts

```kotlin
sealed interface Actividad {
  val fecha: LocalDate; val createdAt: Instant; val id: String
  data class DeMovimiento(val movimiento: Movimiento) : Actividad
  /** [monto] is this transfer's side in the requested currency; both sides when both match it. */
  data class DeTransferencia(val transferencia: Transferencia, val monto: Money, val direccion: Direccion) : Actividad
}

/** Relative to the requested currency: money enters it, leaves it, or both sides are in it. */
enum class Direccion { ENTRADA, SALIDA, INTERNA }

enum class SearchTipo { GASTO, INGRESO, TRANSFERENCIA } // core.model; TipoMovimiento stays two-valued

class MovimientoReadCriteria(/* existing */, cuentaIds: Set<String> = emptySet(), medios: Set<MedioPago> = emptySet(), tipo: SearchTipo? = null)

interface ActividadRepository {
  fun observeActividadRange(moneda: Moneda, range: MovimientoDateRange): Flow<DataOutcome<List<Actividad>>>
  fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> // movimiento and transfer years
  fun observeFiltered(criteria: MovimientoReadCriteria): Flow<DataOutcome<MovimientoSearchSnapshot>> // rows become Actividad
  /** Currencies with at least one live account; drives the currency selector. */
  fun observeMonedas(): Flow<DataOutcome<List<Moneda>>>
}
```

Both reads run movimiento and transfer selects inside **one** `transactionWithResult`, as
`observeFiltered` already does for rows and years, and invalidate on both tables.

## Exact rules

- History: rows are ordered `fecha DESC, created_at DESC, id DESC` across both kinds; a same-currency
  transfer between two accounts of the viewed currency is one row; a cross-currency transfer appears
  in each currency with that side's amount. `projectHistory` summary counts only movimientos; the
  period count keeps "N movements" for movimientos and appends "· M transfers between accounts"
  (`history_transfer_count`, omitted when M = 0), per `docs/wording.md` §7.
- Transfer rows reuse Stage 3's `TransferenciaRowContent` and open transfer management.
- Search: README rules decide when transfers appear; the account filter matches either transfer side;
  the medio dimension never matches transfers. Text search matches transfer descripcion and both
  account names.
- New dimensions follow the existing draft/apply/clear and restoration rules; missing selected
  accounts render with a localized fallback and remove action, like missing categories; archived
  accounts stay filterable.
- Saved state: `SearchSavedInput` stores account IDs, medio names and `SearchTipo` with a
  compatibility path for the existing saved `TipoMovimiento` value.
- Currency selector: README [Display surfaces](../../README.md#display-surfaces) — visible only with
  live accounts in more than one currency, default primary, saved with each screen's existing
  restoration rules; changing it keeps the other filters and History's period selection.
- View movements: `showSearchFilter(moneda = cuenta.moneda, cuentaIds = setOf(id))`, same one-shot
  token semantics; Search switches to that currency before applying the filter.
- Year metadata (`observeYears`) includes years that only have transfers in that currency.

## TDD

- [ ] RED repository: one-snapshot activity read, cross-currency sides, tombstones excluded, AND/OR across new dimensions, transfer visibility conditions.
- [ ] RED History projection/VM: ordering, totals unchanged, cross-currency presence, failures.
- [ ] RED Search filters/session/saved state, including migration of the saved tipo.
- [ ] RED currency selector: hidden with one currency, default primary, restoration, View movements of
      a USD account lands on USD with the account filter.
- [ ] GREEN; filter sheet UI, rows, View movements wiring; previews.
- [ ] Add transfers to the scale fixture; compile device tests.
- [ ] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.

## Blocks

Blocked by Stages 3 and 4. Blocks Stage 6.
