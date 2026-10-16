package com.kurobello.tallybook.core.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
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
    val catalog = history.openCatalog()
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
    val original = PrimaryNavigationState().select(History).openCatalog()
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
    assertEquals(search, search.openCatalog().back())
  }

  @Test
  fun restoredSearchPreservesVisitedEntriesAndStableContentKeys() {
    val original = PrimaryNavigationState().select(History).select(Search).openCatalog()
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
    assertEquals(profile, profile.openCatalog().back())
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
}
