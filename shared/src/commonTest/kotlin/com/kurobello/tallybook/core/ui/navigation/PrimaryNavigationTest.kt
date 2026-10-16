package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.mutableStateOf
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.feature.search.ui.SearchFilterRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
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
  fun aSecondaryThatCannotOpenOverItsPrimaryIsRejected() {
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Home, secondary = Categories)
    }
    assertFailsWith<IllegalArgumentException> { PrimaryNavigationState(Home, secondary = Tags) }
    assertFailsWith<IllegalArgumentException> {
      PrimaryNavigationState(Profile, profileVisited = true, secondary = Search)
    }
  }
}
