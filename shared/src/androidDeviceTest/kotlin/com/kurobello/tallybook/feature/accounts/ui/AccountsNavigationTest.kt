package com.kurobello.tallybook.feature.accounts.ui

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.navigation.PrimaryDeviceShell
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.accounts_edit
import tallybook.shared.generated.resources.accounts_title
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.home_open_accounts
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.profile_accounts_body

internal class AccountsNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun profileOpensAccountsWhoseSheetClosesBeforeBackReturnsToProfile() {
    withShell {
      profileTab().performClick()
      composeRule.onNodeWithText(text(Res.string.profile_accounts_body)).performClick()
      composeRule.onNodeWithText(text(Res.string.accounts_title)).assertIsDisplayed()
      homeTab().assertDoesNotExist()

      composeRule.onNode(hasText("Efectivo") and hasClickAction()).performClick()
      composeRule.onNodeWithText(text(Res.string.accounts_edit)).assertIsDisplayed()
      systemBack()
      composeRule.onNodeWithText(text(Res.string.accounts_edit)).assertDoesNotExist()
      composeRule.onNodeWithText(text(Res.string.accounts_title)).assertIsDisplayed()

      systemBack()
      profileTab().assertIsSelected()
    }
  }

  @Test
  fun homesTotalBalanceCardOpensAccountsAndBackReturnsHome() {
    withShell {
      balanceCard().performClick()
      composeRule.onNodeWithText(text(Res.string.accounts_title)).assertIsDisplayed()

      composeRule.onNodeWithContentDescription(text(Res.string.back)).performClick()
      homeTab().assertIsSelected()
      balanceCard().assertIsDisplayed()
    }
  }

  /** The card is one button whose click label says it opens the accounts. */
  private fun balanceCard() =
      composeRule.onNode(
          hasText(Money(125_000, Moneda.COP).format()) and
              SemanticsMatcher("opens the accounts") { node ->
                node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label ==
                    text(Res.string.home_open_accounts)
              }
      )

  private fun profileTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_profile)) and hasClickAction()
      )

  private fun homeTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_home)) and hasClickAction()
      )

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun withShell(block: () -> Unit) {
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
      block()
    } finally {
      composeRule.runOnIdle { fixture.close() }
    }
  }
}
