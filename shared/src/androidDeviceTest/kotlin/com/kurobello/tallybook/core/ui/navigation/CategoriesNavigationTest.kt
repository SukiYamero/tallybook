package com.kurobello.tallybook.core.ui.navigation

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.category_hide_named
import tallybook.shared.generated.resources.category_view_movimientos_count
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.profile_categories_body
import tallybook.shared.generated.resources.search_remove_filter

internal class CategoriesNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun profileOpensCategoriesOverItAndBothBacksReturnToProfile() {
    withShell {
      profileTab().performClick()
      categoriesEntry().performClick()
      backButton().assertIsDisplayed()
      composeRule.onNodeWithText("Food").assertIsDisplayed()
      homeTab().assertDoesNotExist()
      systemBack()
      profileTab().assertIsSelected()
      categoriesEntry().performClick()
      backButton().performClick()
      profileTab().assertIsSelected()
      categoriesEntry().assertIsDisplayed()
    }
  }

  @Test
  fun aCategorySheetShowsItsMovimientosInSearchAndBackGoesHome() {
    withShell {
      profileTab().performClick()
      categoriesEntry().performClick()
      composeRule.onNodeWithText("Food").performClick()
      composeRule
          .onNodeWithContentDescription(
              runBlocking { getString(Res.string.category_hide_named, "Food") }
          )
          .assertIsDisplayed()
      composeRule
          .onNodeWithText(
              runBlocking { getPluralString(Res.plurals.category_view_movimientos_count, 60, 60) }
          )
          .performClick()
      composeRule.assertSearchShownWithNoTabSelected()
      composeRule
          .onNodeWithContentDescription(
              runBlocking { getString(Res.string.search_remove_filter, "Food") }
          )
          .assertIsDisplayed()
      systemBack()
      homeTab().assertIsSelected()
    }
  }

  private fun profileTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_profile)) and hasClickAction()
      )

  private fun homeTab() = composeRule.onNodeWithContentDescription(text(Res.string.navigation_home))

  private fun categoriesEntry() =
      composeRule.onNodeWithText(text(Res.string.profile_categories_body))

  private fun backButton() = composeRule.onNodeWithContentDescription(text(Res.string.back))

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

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
}
