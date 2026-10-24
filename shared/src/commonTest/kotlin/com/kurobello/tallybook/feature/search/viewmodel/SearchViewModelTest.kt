package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.ViewModelStore
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
  @Test
  fun rapidTextRoundtripCannotLeaveSettledResultsPending() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    for (query in listOf("c", "ca", "caf", "cafe")) vm.onQueryChanged(query)
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun rapidRoundtripToPendingTextResetsItsLastEditDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(200)
    vm.onQueryChanged("other")
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun textTimerBecomesOnlyPendingWorkAfterFirstCandidatesArePrepared() = runTest {
    val fixture = SearchFixture(this)
    fixture.movimientos.source.value = null
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(150)
    runCurrent()
    assertFalse(vm.uiState.value.work == SearchWork.WaitingText)
    assertNull(vm.uiState.value.displayResults)
    fixture.movimientos.restore()
    runCurrent()
    assertEquals(SearchWork.WaitingText, vm.uiState.value.work)
    assertNull(vm.uiState.value.currentCount)
    assertNull(vm.uiState.value.displayResults)
    advanceTimeBy(99)
    runCurrent()
    assertEquals(SearchWork.WaitingText, vm.uiState.value.work)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun debouncesAt250MillisecondsAndClearIsImmediateWithoutSqlRestarts() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged("café")
    assertNull(vm.uiState.value.currentCount)
    assertEquals(2, vm.uiState.value.displayResults?.count)
    runCurrent()
    advanceTimeBy(249)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onQueryChanged(" CAFE ")
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onQueryChanged("not found")
    runCurrent()
    vm.onClearQuery()
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    advanceTimeBy(500)
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun draftEditsAreIsolatedAndApplyingPreservesPendingTextDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(100)
    val id = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(id, vm.uiState.value.filters.copy(tipo = TipoMovimiento.GASTO))
    runCurrent()
    assertEquals(1, fixture.movimientos.starts)
    assertNull(vm.uiState.value.filters.tipo)
    vm.onApplyFilters(id)
    assertNull(vm.uiState.value.displayResults)
    runCurrent()
    assertEquals(2, fixture.movimientos.starts)
    advanceTimeBy(149)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    assertTrue(vm.hasActiveFilterSession)
    vm.onFiltersHidden(id)
    val nextId = requireNotNull(vm.onOpenFilters())
    vm.onFiltersHidden(id)
    assertTrue(vm.hasActiveFilterSession)
    vm.onApplyFilters(nextId)
    runCurrent()
    assertEquals(2, fixture.movimientos.starts)
  }

  @Test
  fun clearFiltersCommitsDefaultsClosesAndKeepsTheTextDeadline() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val first = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(
        first,
        vm.uiState.value.filters.copy(
            preset = SearchDatePreset.Last7Days,
            tipo = TipoMovimiento.INGRESO,
            categoryIds = setOf("selected"),
        ),
    )
    vm.onApplyFilters(first)
    vm.onFiltersHidden(first)
    runCurrent()
    val id = requireNotNull(vm.onOpenFilters())
    vm.onQueryChanged("cafe")
    runCurrent()
    advanceTimeBy(100)
    fixture.clock.instant = Instant.parse("2026-10-07T12:00:00Z")
    vm.onClearFilters(id)
    val defaults = SearchFilters.initial(LocalDate(2026, 10, 7))
    assertEquals(defaults, vm.uiState.value.filters)
    assertEquals(
        defaults,
        assertIs<SearchFilterSession.Closing>(vm.uiState.value.filterSession).renderSnapshot,
    )
    assertEquals("cafe", vm.uiState.value.rawQuery)
    assertTrue(vm.hasActiveFilterSession)
    vm.onApplyFilters(id)
    vm.onClearFilters(id)
    runCurrent()
    assertEquals(3, fixture.movimientos.starts)
    advanceTimeBy(149)
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    vm.onFiltersHidden(id)
    val nextId = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(nextId, defaults.copy(tipo = TipoMovimiento.GASTO))
    val currentSession = vm.uiState.value.filterSession
    vm.onClearFilters(id)
    assertEquals(currentSession, vm.uiState.value.filterSession)
    assertEquals(defaults, vm.uiState.value.filters)
  }

  @Test
  fun showFiltersResetsQueryAndFiltersToThatCategoryAndClosesTheOpenSession() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val applied = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(
        applied,
        vm.uiState.value.filters.copy(
            preset = SearchDatePreset.Last7Days,
            tipo = TipoMovimiento.INGRESO,
            categoryIds = setOf("other"),
        ),
    )
    vm.onApplyFilters(applied)
    vm.onFiltersHidden(applied)
    vm.onQueryChanged("cafe")
    val editing = requireNotNull(vm.onOpenFilters())
    fixture.clock.instant = Instant.parse("2026-10-07T12:00:00Z")

    vm.onShowFilters(setOf("cat_comida"), emptySet())
    runCurrent()

    assertEquals("", vm.uiState.value.rawQuery)
    assertEquals(
        SearchFilters.initial(LocalDate(2026, 10, 7)).copy(categoryIds = setOf("cat_comida")),
        vm.uiState.value.filters,
    )
    assertIs<SearchFilterSession.Closing>(vm.uiState.value.filterSession)
    vm.onApplyFilters(editing)
    assertEquals(setOf("cat_comida"), vm.uiState.value.filters.categoryIds)
    vm.onFiltersHidden(editing)
    assertFalse(vm.hasActiveFilterSession)
  }

  @Test
  fun showFiltersReplacesEveryFilterWithExactlyTheRequestedTags() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onShowFilters(setOf("food"), setOf("t_viaje"))
    vm.onQueryChanged("lunch")
    val editing = requireNotNull(vm.onOpenFilters())

    vm.onShowFilters(emptySet(), setOf("t_empresa"))
    runCurrent()

    assertEquals("", vm.uiState.value.rawQuery)
    assertEquals(
        SearchFilters.initial(TODAY).copy(etiquetaIds = setOf("t_empresa")),
        vm.uiState.value.filters,
    )
    assertEquals(setOf("t_empresa"), vm.uiState.value.criteria?.etiquetaIds)
    assertEquals(emptySet(), vm.uiState.value.criteria?.categoryIds)
    assertIs<SearchFilterSession.Closing>(vm.uiState.value.filterSession)
    vm.onApplyFilters(editing)
    assertEquals(setOf("t_empresa"), vm.uiState.value.filters.etiquetaIds)
    assertEquals(listOf("0"), vm.uiState.value.displayResults?.rows?.map { it.id })
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun showFiltersWithoutIdsKeepsTheCurrentInput() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onShowFilters(emptySet(), setOf("t_empresa"))
    vm.onQueryChanged("cafe")

    vm.onShowFilters(setOf(" "), setOf(""))

    assertEquals("cafe", vm.uiState.value.rawQuery)
    assertEquals(setOf("t_empresa"), vm.uiState.value.filters.etiquetaIds)
  }

  @Test
  fun removingATagFilterCommitsOnlyThatTag() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onShowFilters(setOf("food"), setOf("t_empresa", "t_viaje"))

    vm.onRemoveTagFilter("t_viaje")

    assertEquals(
        SearchFilters.initial(TODAY)
            .copy(categoryIds = setOf("food"), etiquetaIds = setOf("t_empresa")),
        vm.uiState.value.filters,
    )
  }

  @Test
  fun deletedTagLeavesAppliedFiltersCriteriaAndDraftWithoutAFailure() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onShowFilters(emptySet(), setOf("t_empresa", "t_viaje"))
    runCurrent()
    assertEquals(1, vm.uiState.value.currentCount)
    val editing = requireNotNull(vm.onOpenFilters())

    fixture.etiquetas.delete("t_empresa")
    runCurrent()

    assertEquals(setOf("t_viaje"), vm.uiState.value.filters.etiquetaIds)
    assertEquals(setOf("t_viaje"), vm.uiState.value.criteria?.etiquetaIds)
    assertEquals(listOf(VIAJE), vm.uiState.value.etiquetas.values)
    val session = assertIs<SearchFilterSession.Editing>(vm.uiState.value.filterSession)
    assertEquals(setOf("t_viaje"), session.draft.etiquetaIds)
    assertNull(vm.uiState.value.results.failure)
    assertEquals(0, vm.uiState.value.currentCount)
    vm.onApplyFilters(editing)
    assertEquals(setOf("t_viaje"), vm.uiState.value.filters.etiquetaIds)
  }

  @Test
  fun tagFiltersAppliedBeforeTheFirstCatalogDropTagsMissingFromIt() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    vm.onShowFilters(emptySet(), setOf("t_gone", "t_empresa"))
    assertEquals(setOf("t_gone", "t_empresa"), vm.uiState.value.filters.etiquetaIds)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    assertEquals(setOf("t_empresa"), vm.uiState.value.filters.etiquetaIds)
    assertNull(vm.uiState.value.results.failure)
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun textMatchesTagNamesWithoutRestartingTheRead() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    vm.onQueryChanged("EMPRESA")
    advanceTimeBy(250)
    runCurrent()

    assertEquals(listOf("0"), vm.uiState.value.displayResults?.rows?.map { it.id })
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun tagCatalogFailureSurfacesWithoutOpeningFilters() = runTest {
    val fixture = SearchFixture(this)
    fixture.etiquetas.failure = DataError.Local(IllegalStateException())
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    val failure = assertIs<SearchFailure.Repository>(vm.uiState.value.results.failure)
    assertEquals(SearchSource.Etiquetas, failure.source)
    assertFalse(vm.uiState.value.canOpenFilters)
  }

  @Test
  fun categoryFailureSurfacesWhilePreferencesAndMovementsAreSilent() = runTest {
    val fixture = SearchFixture(this)
    fixture.preferences.source.value = null
    fixture.categorias.source.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.results)
    assertEquals(0, fixture.movimientos.starts)
    assertFalse(vm.uiState.value.canOpenFilters)
  }

  @Test
  fun distinguishesNoCurrencyDataFromFilteredEmptyAndRetainsMetadataForCorrection() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val id = requireNotNull(vm.onOpenFilters())
    vm.onFilterDraftChanged(
        id,
        vm.uiState.value.filters.copy(
            preset = SearchDatePreset.Custom,
            customStart = TODAY,
            customEnd = TODAY,
        ),
    )
    vm.onApplyFilters(id)
    vm.onFiltersHidden(id)
    runCurrent()
    fixture.clock.instant = Instant.parse("2026-10-05T12:00:00Z")
    val starts = fixture.movimientos.starts
    vm.onResume()
    runCurrent()
    assertIs<SearchFailure.InvalidDates>(vm.uiState.value.results.failure)
    assertNull(vm.uiState.value.displayResults)
    assertEquals(starts, fixture.movimientos.starts)
    assertEquals(listOf(2026), vm.uiState.value.years?.persistedYears)
    assertTrue(vm.uiState.value.canOpenFilters)
    assertNotNull(vm.onOpenFilters())
  }

  @Test
  fun categoryRenameRebuildsMatchesWithoutSqlAndWhitespaceTextSettlesImmediately() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    vm.onQueryChanged("snacks")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals(0, vm.uiState.value.currentCount)
    fixture.categorias.source.value =
        DataOutcome.Success(
            listOf(Categoria("food", "Snacks", null, CategoryIcon.UTENSILS, CategoryTint.AMBER))
        )
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged(" \t ")
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    assertEquals(1, fixture.movimientos.starts)
  }

  @Test
  fun sourceChangesInvalidateHeldMatchingAndDoNotPublishOldLabels() = runTest {
    val fixture = SearchFixture(this)
    val computation = HoldingDispatcher()
    val vm = fixture.create(computation)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    repeat(4) {
      computation.releaseAll()
      runCurrent()
    }
    assertEquals(2, vm.uiState.value.currentCount)
    vm.onQueryChanged("snacks")
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    fixture.categorias.source.value =
        DataOutcome.Success(
            listOf(Categoria("food", "Snacks", null, CategoryIcon.UTENSILS, CategoryTint.AMBER))
        )
    runCurrent()
    assertNull(vm.uiState.value.currentCount)
    repeat(4) {
      computation.releaseAll()
      runCurrent()
    }
    assertEquals(2, vm.uiState.value.currentCount)
    assertTrue(vm.uiState.value.displayResults!!.rows.all { it.categoryLabel == "Snacks" })
  }

  @Test
  fun readFailureKeepsOnlySameTextAndRetryPreservesAppliedInput() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    fixture.movimientos.source.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    runCurrent()
    assertIs<RetainedLoadState.Failed<*, *>>(vm.uiState.value.results)
    assertEquals(2, vm.uiState.value.displayResults?.count)
    vm.onQueryChanged("cafe")
    assertNull(vm.uiState.value.displayResults)
    runCurrent()
    fixture.movimientos.restore()
    vm.onRetry()
    runCurrent()
    advanceTimeBy(250)
    runCurrent()
    assertEquals("cafe", vm.uiState.value.rawQuery)
    assertEquals(1, vm.uiState.value.currentCount)
  }

  @Test
  fun coherentYearsDetermineNoDataAndCurrencyChangesDiscardOldMetadata() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), listOf(1900)))
    runCurrent()
    assertEquals(0, vm.uiState.value.currentCount)
    assertTrue(vm.uiState.value.displayResults!!.years.hasCurrencyData)
    assertTrue(1900 in vm.uiState.value.availableYears)
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList()))
    runCurrent()
    assertFalse(vm.uiState.value.displayResults!!.years.hasCurrencyData)
    fixture.preferences.source.value =
        DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP)))
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.years?.moneda)
    assertFalse(vm.uiState.value.availableYears.contains(1900))
  }

  @Test
  fun editingSessionKeepsEarliestYearUntilItsMatchingHiddenCallback() = runTest {
    val fixture = SearchFixture(this)
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), listOf(1900)))
    val vm = fixture.create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val session = requireNotNull(vm.onOpenFilters())
    fixture.movimientos.source.value =
        DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList()))
    runCurrent()
    assertTrue(1900 in vm.uiState.value.availableYears)
    vm.onCancelFilters(session)
    assertTrue(1900 in vm.uiState.value.availableYears)
    vm.onFiltersHidden(session)
    assertFalse(1900 in vm.uiState.value.availableYears)
  }

  @Test
  fun listenersStopAtFiveSecondsAndOwnerClearKeepsInjectedSiblingAlive() = runTest {
    val fixture = SearchFixture(this)
    val vm = fixture.create()
    runCurrent()
    assertEquals(0, fixture.movimientos.starts)
    val collector = backgroundScope.launch { vm.uiState.collect {} }
    val sibling = backgroundScope.launch { awaitCancellation() }
    runCurrent()
    collector.cancel()
    runCurrent()
    advanceTimeBy(4999)
    runCurrent()
    assertEquals(1, fixture.movimientos.active)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(0, fixture.movimientos.active)
    assertNull(vm.uiState.value.currentCount)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    assertEquals(2, vm.uiState.value.currentCount)
    ViewModelStore().apply {
      put("search", vm)
      clear()
    }
    runCurrent()
    assertEquals(0, fixture.movimientos.active)
    assertTrue(sibling.isActive)
  }
}

private class HoldingDispatcher : CoroutineDispatcher() {
  private val pending = mutableListOf<Runnable>()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    pending.add(block)
  }

  fun releaseAll() {
    while (pending.isNotEmpty()) pending.removeAt(0).run()
  }
}
