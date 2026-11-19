package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.mutableStateOf
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.feature.search.ui.SearchFilterRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

internal class PrimaryNavigationTest {
  @Test
  fun visitsHistoryOnceAndSelectedTabIsNoOp() {
    var state = PrimaryNavigationState()
    assertEquals(listOf(Home), state.retainedKeys)
    assertSame(state, state.select(Home))
    repeat(10) {
      state = state.select(History)
      assertEquals(listOf(Home, History), state.retainedKeys)
      assertSame(state, state.select(History))
      state = state.select(Home)
      assertEquals(listOf(Home, History), state.retainedKeys)
    }
  }

  @Test
  fun catalogReturnsToSelectedPrimaryAndRootCannotBecomeEmpty() {
    val history = PrimaryNavigationState().select(History)
    val catalog = history.openSecondary(UiCatalog)
    assertEquals(listOf(Home, History, UiCatalog), catalog.displayedKeys)
    assertEquals(history, catalog.back())
    val home = history.back()
    assertEquals(Home, home.selected)
    assertEquals(listOf(Home, History), home.retainedKeys)
    assertEquals(listOf(Home), home.displayedKeys)
    assertSame(home, home.back())
  }

  @Test
  fun completeNavigationSnapshotRestoresVisitedAndSelectedKeys() {
    val original = PrimaryNavigationState().select(History).openSecondary(UiCatalog)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(listOf(Home, History, UiCatalog), restored.retainedKeys)
    assertEquals(History, restored.back().selected)
  }

  @Test
  fun searchRetainsItsOwnerAndBackReturnsHome() {
    val search = PrimaryNavigationState().select(Search)
    assertEquals(Search, search.selected)
    assertEquals(listOf(Home, Search), search.retainedKeys)
    assertEquals(listOf(Home, Search), search.displayedKeys)
    assertSame(search, search.select(Search))
    val history = search.select(History)
    assertEquals(listOf(Home, History, Search), history.retainedKeys)
    assertEquals(listOf(Home, History), history.displayedKeys)
    assertEquals(listOf(Home, History, Search), history.select(Search).back().retainedKeys)
    assertEquals(Home, history.select(Search).back().selected)
    assertEquals(search, search.openSecondary(UiCatalog).back())
  }

  @Test
  fun restoredSearchPreservesVisitedEntriesAndStableContentKeys() {
    val original = PrimaryNavigationState().select(History).select(Search).openSecondary(UiCatalog)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(listOf(Home, History, Search, UiCatalog), restored.retainedKeys)
    assertEquals(Search, restored.back().selected)
    assertEquals("primary-search", primaryContentKey(Search))
  }

  @Test
  fun profileRetainsItsOwnerRestoresAndBackReturnsHome() {
    val profile = PrimaryNavigationState().select(Search).select(Profile)
    assertEquals(listOf(Home, Search, Profile), profile.retainedKeys)
    assertEquals(listOf(Home, Profile), profile.displayedKeys)
    assertSame(profile, profile.select(Profile))
    assertEquals(profile, profile.openSecondary(UiCatalog).back())
    val home = profile.back()
    assertEquals(Home, home.selected)
    assertEquals(listOf(Home, Search, Profile), home.retainedKeys)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), profile)
        )
    assertEquals(profile, restored)
    assertEquals(profile, initialPrimaryNavigation(Profile).select(Search).select(Profile))
    assertEquals("primary-profile", primaryContentKey(Profile))
  }

  @Test
  fun statisticsRetainsItsOwnerAndBackReturnsHome() {
    val statistics = PrimaryNavigationState().select(Statistics)
    assertEquals(Statistics, statistics.selected)
    assertEquals(listOf(Home, Statistics), statistics.retainedKeys)
    assertEquals(listOf(Home, Statistics), statistics.displayedKeys)
    assertSame(statistics, statistics.select(Statistics))
    val search = statistics.select(Search)
    assertEquals(listOf(Home, Search, Statistics), search.retainedKeys)
    assertEquals(listOf(Home, Search), search.displayedKeys)
    assertEquals(statistics.copy(searchVisited = true), search.select(Statistics))
    val home = statistics.back()
    assertEquals(Home, home.selected)
    assertEquals(listOf(Home, Statistics), home.retainedKeys)
    assertEquals(listOf(Home), home.displayedKeys)
    assertEquals(statistics, statistics.openSecondary(UiCatalog).back())
    assertEquals(statistics, initialPrimaryNavigation(Statistics))
    assertEquals("primary-statistics", primaryContentKey(Statistics))
  }

  @Test
  fun statisticsIsNeverASecondaryAndOpensOnlyTheCatalog() {
    val statistics = PrimaryNavigationState().select(Statistics)
    listOf(Categories, Tags, Accounts, Home, Search, Profile, Statistics).forEach {
      assertSame(statistics, statistics.openSecondary(it))
    }
    val profile = PrimaryNavigationState().select(Profile)
    assertSame(profile, profile.openSecondary(Statistics))
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Profile, profileVisited = true, secondary = Statistics)
    }
    assertFailsWith<IllegalArgumentException> { PrimaryNavigationState(Statistics) }
  }

  @Test
  fun restoredStatisticsPreservesVisitedEntriesAndItsSelection() {
    val original =
        PrimaryNavigationState().select(History).select(Statistics).openSecondary(UiCatalog)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(listOf(Home, History, Statistics, UiCatalog), restored.retainedKeys)
    assertEquals(Statistics, restored.back().selected)
  }

  @Test
  fun aSnapshotSavedBeforeStatisticsExistedRestoresUnvisited() {
    val saved =
        """{"selected":{"type":"com.kurobello.tallybook.core.ui.navigation.Search"},""" +
            """"historyVisited":true,"secondary":null,"searchVisited":true,""" +
            """"profileVisited":true}"""

    val restored = Json.decodeFromString<PrimaryNavigationState>(saved)

    assertEquals(Search, restored.selected)
    assertEquals(false, restored.statisticsVisited)
    assertEquals(listOf(Home, History, Search, Profile), restored.retainedKeys)
    assertEquals(
        listOf(Home, History, Search, Statistics, Profile),
        restored.select(Statistics).retainedKeys,
    )
  }

  @Test
  fun categoriesOpensOnlyOverProfileAndBackReturnsToProfile() {
    val home = PrimaryNavigationState()
    assertSame(home, home.openSecondary(Categories))
    val search = home.select(Search)
    assertSame(search, search.openSecondary(Categories))
    assertSame(search, search.openSecondary(Home))

    val profile = search.select(Profile)
    val categories = profile.openSecondary(Categories)
    assertEquals(Categories, categories.secondary)
    assertEquals(listOf(Home, Search, Profile, Categories), categories.retainedKeys)
    assertEquals(listOf(Home, Profile, Categories), categories.displayedKeys)
    assertSame(categories, categories.openSecondary(UiCatalog))
    assertSame(categories, categories.openSecondary(Categories))
    assertEquals(profile, categories.back())
    assertEquals(profile, categories.select(Profile))
    assertEquals(
        initialPrimaryNavigation(Categories),
        initialPrimaryNavigation(Profile).openSecondary(Categories),
    )
    assertEquals("secondary-categories", primaryContentKey(Categories))
  }

  @Test
  fun tagsOpensOnlyOverProfileAndBackReturnsToProfile() {
    val home = PrimaryNavigationState()
    assertSame(home, home.openSecondary(Tags))
    val search = home.select(Search)
    assertSame(search, search.openSecondary(Tags))

    val profile = search.select(Profile)
    val tags = profile.openSecondary(Tags)
    assertEquals(Tags, tags.secondary)
    assertEquals(listOf(Home, Search, Profile, Tags), tags.retainedKeys)
    assertEquals(listOf(Home, Profile, Tags), tags.displayedKeys)
    assertSame(tags, tags.openSecondary(Categories))
    assertSame(tags, tags.openSecondary(Tags))
    assertEquals(profile, tags.back())
    assertEquals(profile, tags.select(Profile))
    assertEquals(
        initialPrimaryNavigation(Tags),
        initialPrimaryNavigation(Profile).openSecondary(Tags),
    )
    assertEquals("secondary-tags", primaryContentKey(Tags))
  }

  @Test
  fun viewingATagsMovimientosLeavesTagsForSearchWithAPendingTagFilter() {
    val navigation = mutableStateOf(PrimaryNavigationState())
    val modals = PrimaryModalCoordinator(AddMovimientoSheetSession()) { false }
    val navigator = PrimaryNavigator(navigation, modals, catalogEnabled = false)
    navigator.select(Profile)
    navigator.openSecondary(Tags)
    assertEquals(Tags, navigator.state.secondary)

    navigator.showSearchFilter(etiquetaIds = setOf("t_empresa"))

    assertEquals(Search, navigator.state.selected)
    assertEquals(null, navigator.state.secondary)
    val request = assertNotNull(navigator.searchFilterRequest)
    assertEquals(SearchFilterRequest(request.token, etiquetaIds = setOf("t_empresa")), request)
    assertEquals(Home, navigator.state.back().selected)
  }

  @Test
  fun aFilterRequestCarriesItsRangeAndTipoToSearch() {
    val navigation = mutableStateOf(PrimaryNavigationState())
    val modals = PrimaryModalCoordinator(AddMovimientoSheetSession()) { false }
    val navigator = PrimaryNavigator(navigation, modals, catalogEnabled = false)
    val range = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))

    navigator.showSearchFilter(
        categoryIds = setOf("food"),
        moneda = Moneda.COP,
        range = range,
        tipo = SearchTipo.GASTO,
    )

    assertEquals(Search, navigator.state.selected)
    val request = assertNotNull(navigator.searchFilterRequest)
    assertEquals(
        SearchFilterRequest(
            request.token,
            categoryIds = setOf("food"),
            moneda = Moneda.COP,
            range = range,
            tipo = SearchTipo.GASTO,
        ),
        request,
    )
  }

  @Test
  fun statisticsTakesSearchsTabSoSearchSelectsNoTab() {
    assertEquals(listOf(Home, History, Statistics, Profile), primaryTabDestinations)
    assertFalse(Search in primaryTabDestinations)
    assertTrue(PrimaryNavigationState().select(Search).selected !in primaryTabDestinations)
  }

  @Test
  fun aStatisticsGroupOpensSearchOverItsGastosWhileItsSelectionStays() {
    val navigation = mutableStateOf(PrimaryNavigationState())
    val modals = PrimaryModalCoordinator(AddMovimientoSheetSession()) { false }
    val navigator = PrimaryNavigator(navigation, modals, catalogEnabled = false)
    val range = MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 24))
    navigator.select(Statistics)

    navigator.showStatisticsFilter(StatisticsKey.Categoria("food"), Moneda.COP, range)
    val category = assertNotNull(navigator.searchFilterRequest)
    navigator.showStatisticsFilter(StatisticsKey.Etiqueta("trip"), Moneda.COP, range)
    val tag = assertNotNull(navigator.searchFilterRequest)
    navigator.showStatisticsFilter(StatisticsKey.Cuenta("cash"), Moneda.USD, range)
    val cuenta = assertNotNull(navigator.searchFilterRequest)

    assertEquals(
        SearchFilterRequest(
            category.token,
            categoryIds = setOf("food"),
            moneda = Moneda.COP,
            range = range,
            tipo = SearchTipo.GASTO,
        ),
        category,
    )
    assertEquals(
        SearchFilterRequest(
            tag.token,
            etiquetaIds = setOf("trip"),
            moneda = Moneda.COP,
            range = range,
            tipo = SearchTipo.GASTO,
        ),
        tag,
    )
    assertEquals(
        SearchFilterRequest(
            cuenta.token,
            cuentaIds = setOf("cash"),
            moneda = Moneda.USD,
            range = range,
            tipo = SearchTipo.GASTO,
        ),
        cuenta,
    )
    assertEquals(Search, navigator.state.selected)
    assertEquals(listOf(Home, Search, Statistics), navigator.state.retainedKeys)
    navigator.back()
    assertEquals(Home, navigator.state.selected)
  }

  @Test
  fun aMedioOpensNoSearch() {
    val navigation = mutableStateOf(PrimaryNavigationState().select(Statistics))
    val modals = PrimaryModalCoordinator(AddMovimientoSheetSession()) { false }
    val navigator = PrimaryNavigator(navigation, modals, catalogEnabled = false)
    val range = MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 24))

    navigator.showStatisticsFilter(StatisticsKey.Medio(MedioPago.EFECTIVO), Moneda.COP, range)

    assertEquals(Statistics, navigator.state.selected)
    assertNull(navigator.searchFilterRequest)
  }

  @Test
  fun snapshotWithTagsOpenRestoresTheSecondarySlot() {
    val original = PrimaryNavigationState().select(Profile).openSecondary(Tags)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(Profile, restored.back().selected)
  }

  @Test
  fun showingACategoryLeavesCategoriesForSearchWhoseBackSelectsHome() {
    val categories = PrimaryNavigationState().select(Profile).openSecondary(Categories)
    val search = categories.select(Search)
    assertEquals(Search, search.selected)
    assertEquals(null, search.secondary)
    assertEquals(listOf(Home, Search, Profile), search.retainedKeys)
    assertEquals(listOf(Home, Search), search.displayedKeys)
    assertEquals(Home, search.back().selected)
  }

  @Test
  fun snapshotWithCategoriesOpenRestoresTheSecondarySlot() {
    val original =
        PrimaryNavigationState().select(History).select(Profile).openSecondary(Categories)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(listOf(Home, History, Profile, Categories), restored.retainedKeys)
    assertEquals(Profile, restored.back().selected)
    assertEquals(null, restored.back().secondary)
  }

  @Test
  fun accountsOpensOverHomeOrProfileAndBackReturnsToThatPrimary() {
    val home = PrimaryNavigationState()
    val fromHome = home.openSecondary(Accounts)
    assertEquals(Accounts, fromHome.secondary)
    assertEquals(listOf(Home, Accounts), fromHome.displayedKeys)
    assertEquals(home, fromHome.back())

    val history = home.select(History)
    assertSame(history, history.openSecondary(Accounts))
    val search = home.select(Search)
    assertSame(search, search.openSecondary(Accounts))

    val profile = home.select(Profile)
    val fromProfile = profile.openSecondary(Accounts)
    assertEquals(listOf(Home, Profile, Accounts), fromProfile.retainedKeys)
    assertSame(fromProfile, fromProfile.openSecondary(Categories))
    assertEquals(profile, fromProfile.back())
    assertEquals(listOf(Home), fromProfile.select(Home).displayedKeys)
    assertEquals(
        initialPrimaryNavigation(Profile).openSecondary(Accounts),
        initialPrimaryNavigation(Accounts),
    )
    assertEquals("secondary-accounts", primaryContentKey(Accounts))
  }

  @Test
  fun recurrentesOpensOverHomeOrProfileAndBackReturnsToThatPrimary() {
    val home = PrimaryNavigationState()
    val fromHome = home.openSecondary(Recurrentes)
    assertEquals(listOf(Home, Recurrentes), fromHome.displayedKeys)
    assertEquals(home, fromHome.back())

    val history = home.select(History)
    assertSame(history, history.openSecondary(Recurrentes))

    val profile = home.select(Profile)
    val fromProfile = profile.openSecondary(Recurrentes)
    assertEquals(listOf(Home, Profile, Recurrentes), fromProfile.retainedKeys)
    assertEquals(profile, fromProfile.back())
    assertEquals(
        initialPrimaryNavigation(Profile).openSecondary(Recurrentes),
        initialPrimaryNavigation(Recurrentes),
    )
    assertEquals("secondary-recurrentes", primaryContentKey(Recurrentes))
  }

  @Test
  fun presupuestosOpensOverHomeOrProfileAndBackReturnsToThatPrimary() {
    val home = PrimaryNavigationState()
    val fromHome = home.openSecondary(Presupuestos)
    assertEquals(listOf(Home, Presupuestos), fromHome.displayedKeys)
    assertEquals(home, fromHome.back())

    val history = home.select(History)
    assertSame(history, history.openSecondary(Presupuestos))

    val profile = home.select(Profile)
    val fromProfile = profile.openSecondary(Presupuestos)
    assertEquals(listOf(Home, Profile, Presupuestos), fromProfile.retainedKeys)
    assertEquals(profile, fromProfile.back())
    assertEquals(
        initialPrimaryNavigation(Profile).openSecondary(Presupuestos),
        initialPrimaryNavigation(Presupuestos),
    )
    assertEquals("secondary-presupuestos", primaryContentKey(Presupuestos))
  }

  @Test
  fun snapshotWithAccountsOpenOverHomeRestoresTheSecondarySlot() {
    val original = PrimaryNavigationState().openSecondary(Accounts)
    val restored =
        Json.decodeFromString<PrimaryNavigationState>(
            Json.encodeToString(PrimaryNavigationState.serializer(), original)
        )
    assertEquals(original, restored)
    assertEquals(Home, restored.back().selected)
    assertEquals(null, restored.back().secondary)
  }

  @Test
  fun aSecondaryThatCannotOpenOverItsPrimaryIsRejected() {
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Home, secondary = Categories)
    }
    assertFailsWith<IllegalArgumentException> { PrimaryNavigationState(Home, secondary = Tags) }
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Search, searchVisited = true, secondary = Accounts)
    }
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Profile, profileVisited = true, secondary = Search)
    }
  }
}
