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
}
