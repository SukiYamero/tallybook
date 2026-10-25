package com.kurobello.tallybook.ui.test

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.kurobello.tallybook.core.ui.navigation.Destination
import com.kurobello.tallybook.core.ui.navigation.History
import com.kurobello.tallybook.core.ui.navigation.Home
import com.kurobello.tallybook.core.ui.navigation.Profile
import com.kurobello.tallybook.core.ui.navigation.Search
import com.kurobello.tallybook.core.ui.navigation.Statistics
import com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TallybookBottomNavigationTest {

  @Test
  fun searchHasNoTabSoNoTabIsSelectedWhileItIsOpen() = runComposeUiTest {
    setContent {
      TallybookTheme {
        TallybookBottomNavigation(
            onAddMovement = {},
            selectedDestination = Search,
            onSelectDestination = {},
        )
      }
    }

    onAllNodes(isTab).assertCountEquals(TAB_COUNT)
    onAllNodes(isTab and isSelected()).assertCountEquals(0)
  }

  @Test
  fun statisticsIsTheFourthTabAndReportsItsDestination() = runComposeUiTest {
    val selected = mutableListOf<Destination>()
    setContent {
      TallybookTheme {
        TallybookBottomNavigation(
            onAddMovement = {},
            selectedDestination = Statistics,
            onSelectDestination = { selected += it },
        )
      }
    }

    onAllNodes(isTab and isSelected()).assertCountEquals(1)
    onAllNodes(isTab)[STATISTICS_INDEX].assertIsSelected()
    repeat(TAB_COUNT) { index -> onAllNodes(isTab)[index].performClick() }

    waitForIdle()
    assertEquals<List<Destination>>(listOf(Home, History, Statistics, Profile), selected)
  }

  private companion object {
    const val TAB_COUNT = 4
    const val STATISTICS_INDEX = 2
    val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
  }
}
