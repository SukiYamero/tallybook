package com.kurobello.tallybook.core.ui.navigation

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_statistics_open
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_statistics
import tallybook.shared.generated.resources.search_count
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.statistics_detail_view
import tallybook.shared.generated.resources.statistics_scope_year
import tallybook.shared.generated.resources.statistics_title

internal class StatisticsNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun aCategorySheetShowsTheSameMovimientosInSearchAndBackGoesHome() {
    withShell {
      statisticsTab().performClick()
      statisticsTab().assertIsSelected()
      statisticsList().performScrollToNode(hasText(CATEGORY) and hasClickAction())
      composeRule.onNode(hasText(CATEGORY) and hasClickAction()).performClick()
      composeRule
          .onNode(hasText(plural(Res.plurals.statistics_detail_view, ROWS)) and hasClickAction())
          .performScrollTo()
          .performClick()

      composeRule.assertSearchShownWithNoTabSelected()
      composeRule.onNodeWithText(plural(Res.plurals.search_count, ROWS)).assertIsDisplayed()
      composeRule
          .onNodeWithContentDescription(
              runBlocking { getString(Res.string.search_remove_filter, CATEGORY) }
          )
          .assertIsDisplayed()
      systemBack()
      homeTab().assertIsSelected()
    }
  }

  @Test
  fun theHomeCardOpensStatisticsWithItsRetainedSelection() {
    withShell {
      statisticsTab().performClick()
      composeRule
          .onNode(hasText(text(Res.string.statistics_scope_year)) and hasClickAction())
          .performClick()
      homeTab().performClick()
      homeTab().assertIsSelected()

      composeRule.onNode(hasClickLabel(text(Res.string.home_statistics_open))).performClick()

      statisticsTab().assertIsSelected()
      composeRule
          .onNode(hasText(text(Res.string.statistics_scope_year)) and hasClickAction())
          .assertIsSelected()
      systemBack()
      homeTab().assertIsSelected()
    }
  }

  private fun statisticsTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_statistics)) and hasClickAction()
      )

  private fun statisticsList() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.statistics_title)) and
              SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
      )

  private fun homeTab() = composeRule.onNodeWithContentDescription(text(Res.string.navigation_home))

  private fun hasClickLabel(label: String) =
      SemanticsMatcher("onClick label is $label") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
      }

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun plural(resource: PluralStringResource, count: Int): String = runBlocking {
    getPluralString(resource, count, count)
  }

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun withShell(block: (PrimaryDeviceShell) -> Unit) {
    val fixture = PrimaryDeviceShell()
    try {
      composeRule.setContent {
        if (fixture.mounted.value) {
          val owner = fixture.owner.value
          KoinIsolatedContext(fixture.context) {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalViewModelStoreOwner provides owner.viewModelOwner,
                LocalSavedStateRegistryOwner provides owner,
                LocalSaveableStateRegistry provides fixture.registry.value,
            ) {
              TallybookTheme { TallybookViewportOverlayHost { TallybookNavHost() } }
            }
          }
        }
      }
      composeRule.waitForIdle()
      block(fixture)
    } finally {
      composeRule.runOnIdle { fixture.close() }
    }
  }

  private companion object {
    /** Every fixture row is a USD gasto in Food on the fixture's today. */
    const val CATEGORY = "Food"
    const val ROWS = 60
  }
}
