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
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.profile_tags_body
import tallybook.shared.generated.resources.tag_edit_title
import tallybook.shared.generated.resources.tag_view_movimientos_count

internal class TagsNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun profileOpensTagsWhoseSheetClosesBeforeBackReturnsToProfile() {
    withShell {
      profileTab().performClick()
      tagsEntry().performClick()
      backButton().assertIsDisplayed()
      homeTab().assertDoesNotExist()
      composeRule.onNodeWithText(EMPRESA.nombre).performClick()
      composeRule.onNodeWithText(text(Res.string.tag_edit_title)).assertIsDisplayed()

      systemBack()
      composeRule.onNodeWithText(text(Res.string.tag_edit_title)).assertDoesNotExist()
      backButton().assertIsDisplayed()

      systemBack()
      profileTab().assertIsSelected()
      tagsEntry().performClick()
      backButton().performClick()
      profileTab().assertIsSelected()
    }
  }

  @Test
  fun viewingATagsMovimientosSelectsSearchAndBackGoesHome() {
    withShell {
      profileTab().performClick()
      tagsEntry().performClick()
      composeRule.onNodeWithText(EMPRESA.nombre).performClick()
      composeRule
          .onNodeWithText(
              runBlocking { getPluralString(Res.plurals.tag_view_movimientos_count, 2, 2) }
          )
          .performClick()
      searchTab().assertIsSelected()
      systemBack()
      homeTab().assertIsSelected()
    }
  }

  private fun profileTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_profile)) and hasClickAction()
      )

  private fun searchTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_search)) and hasClickAction()
      )

  private fun homeTab() = composeRule.onNodeWithContentDescription(text(Res.string.navigation_home))

  private fun tagsEntry() = composeRule.onNodeWithText(text(Res.string.profile_tags_body))

  private fun backButton() = composeRule.onNodeWithContentDescription(text(Res.string.back))

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun withShell(block: (PrimaryDeviceShell) -> Unit) {
    val fixture = PrimaryDeviceShell()
    val etiquetas = fixture.context.koin.get<EtiquetaRepository>() as FakeEtiquetaRepository
    etiquetas.etiquetas.value = listOf(EMPRESA)
    etiquetas.movimientoCounts.value = mapOf(EMPRESA.id to 2L)
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
    val EMPRESA = Etiqueta("t_empresa", "Empresa", Instant.parse("2026-01-01T00:00:00Z"))
  }
}
