package com.kurobello.tallybook.core.ui.navigation

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.testing.withKeyboardInputMode
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.search_apply
import tallybook.shared.generated.resources.search_filters
import tallybook.shared.generated.resources.search_income
import tallybook.shared.generated.resources.search_last_7_days
import tallybook.shared.generated.resources.search_no_results_title
import tallybook.shared.generated.resources.search_placeholder
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.search_title

internal class SearchNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun searchRetainsQueryAcrossHomeAndScrollAcrossANewOwner() {
    withShell { fixture ->
      homeField().performClick()
      composeRule.assertSearchShownWithNoTabSelected()
      field().assertIsFocused().performTextInput("Item")
      systemBack()
      searchList().performScrollToNode(hasText("Item 35"))
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      restoreShell(fixture)
      composeRule.assertNoTabSelected()
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      homeTab().performClick()
      homeTab().assertIsSelected()
      homeField().performClick()
      field().assertTextContains("Item")
    }
  }

  @Test
  fun everyHomeEntryRevealsDeeplyScrolledFieldAndFocusesItAndBackReturnsHome() {
    withShell {
      homeField().performClick()
      field().assertIsFocused()
      systemBack()
      field().assertIsNotFocused()
      searchList().performScrollToNode(hasText("Item 35"))
      homeTab().performClick()
      homeField().performClick()
      composeRule.assertNoTabSelected()
      field().assertIsDisplayed().assertIsFocused()
      systemBack()
      systemBack()
      homeTab().assertIsSelected()
    }
  }

  @Test
  fun filterDraftDiscardApplyAndClearKeepUnderlyingNavigationAndModalExclusive() {
    composeRule.withKeyboardInputMode { filterDraftDiscardApplyAndClear() }
  }

  private fun filterDraftDiscardApplyAndClear() {
    withShell {
      homeField().performClick()
      filters().performClick()
      composeRule.onNodeWithText(text(Res.string.search_last_7_days)).performClick()
      homeTab().performClick()
      composeRule.assertNoTabSelected()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule
          .onNodeWithContentDescription(text(Res.string.movimiento_amount_expense_prompt))
          .assertDoesNotExist()
      systemBack()
      composeRule.onNodeWithText(text(Res.string.search_apply)).assertDoesNotExist()
      filters().assertIsFocused()
      composeRule.onNodeWithText(text(Res.string.search_last_7_days)).assertDoesNotExist()
      filters().performClick()
      composeRule.onNodeWithText(text(Res.string.search_income)).performClick()
      composeRule.onNodeWithText(text(Res.string.search_apply)).performClick()
      composeRule.onNodeWithText(text(Res.string.search_no_results_title)).assertIsDisplayed()
      composeRule
          .onNodeWithContentDescription(
              runBlocking {
                getString(Res.string.search_remove_filter, text(Res.string.search_income))
              }
          )
          .performClick()
      composeRule.onNodeWithText("Item 9").assertIsDisplayed()
    }
  }

  private fun homeField() = composeRule.onNodeWithText(text(Res.string.home_search_placeholder))

  private fun homeTab() = composeRule.onNodeWithContentDescription(text(Res.string.navigation_home))

  private fun searchList() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.search_title)) and
              SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
      )

  private fun field() =
      composeRule.onNodeWithContentDescription(text(Res.string.search_placeholder))

  private fun filters() = composeRule.onNodeWithContentDescription(text(Res.string.search_filters))

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun restoreShell(fixture: PrimaryDeviceShell) {
    lateinit var snapshot: PrimaryNavigationSnapshot
    composeRule.runOnIdle { snapshot = fixture.saveAndUnmount() }
    composeRule.waitForIdle()
    composeRule.runOnIdle { fixture.restoreNewOwners(snapshot) }
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
}
