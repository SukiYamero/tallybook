package com.kurobello.tallybook.core.ui.navigation

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.history_scope_week
import tallybook.shared.generated.resources.history_year_description
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.ui_catalog_title

internal class HistoryNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun actualShellRetainsTypedSelectionScrollAndRestoresIntoFreshOwners() {
    withShell { fixture ->
      historyTab().performClick()
      composeRule.onNodeWithText(text(Res.string.history_scope_week)).performClick()
      val list = historyList()
      list.performScrollToNode(hasText("Item 35"))
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      historyTab().performClick()
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      val starts = fixture.movimientos.starts.get()
      restoreShell(fixture)
      historyTab().assertIsSelected()
      composeRule.waitUntil(5_000) { fixture.movimientos.starts.get() > starts }
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      list.performScrollToNode(hasText(text(Res.string.history_scope_week)))
      composeRule.onNodeWithText(text(Res.string.history_scope_week)).assertIsSelected()
    }
  }

  @Test
  fun onlyPrimariesNavigateAndCreationBlocksUnderlyingSelectionUntilHidden() {
    withShell {
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_profile))
          .assertHasClickAction()
          .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_home))
          .assertHasClickAction()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
      historyTab().performClick()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      historyTab().assertIsSelected()
      composeRule
          .onNodeWithContentDescription(text(Res.string.movimiento_phrase_label))
          .assertIsDisplayed()
      val pane =
          SemanticsMatcher.expectValue(
              SemanticsProperties.PaneTitle,
              text(Res.string.add_movimiento_pane_title),
          )
      repeat(3) {
        if (composeRule.onAllNodes(pane).fetchSemanticsNodes().isNotEmpty()) {
          var imeVisible = false
          InstrumentationRegistry.getInstrumentation().runOnMainSync {
            imeVisible =
                WindowInspector.getGlobalWindowViews().any { view ->
                  view.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
                }
          }
          InstrumentationRegistry.getInstrumentation()
              .sendStatus(
                  2,
                  Bundle().apply {
                    putString(
                        "history_diagnostic",
                        JSONObject()
                            .put("phase", "form-dismiss")
                            .put("imeVisible", imeVisible)
                            .toString(),
                    )
                  },
              )
          systemBack()
          historyTab().assertIsSelected()
        }
      }
      composeRule.onNode(pane).assertDoesNotExist()
      historyTab().assertIsSelected()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  @Test
  fun freshShellDoesNotRestoreChangedCreationDraftOrAnOpenSession() {
    withShell { fixture ->
      historyTab().performClick()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      val sentence = text(Res.string.movimiento_phrase_label)
      composeRule.onNodeWithContentDescription(sentence).performTextInput("Gasté 1234")
      restoreShell(fixture)
      historyTab().assertIsSelected()
      composeRule.onNodeWithContentDescription(sentence).assertDoesNotExist()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule
          .onNodeWithContentDescription(sentence)
          .assert(
              SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
          )
    }
  }

  @Test
  fun outgoingPrimaryReadsStopAfterSettledTransitionAndSharingTimeout() {
    withShell { fixture ->
      composeRule.waitUntil(5_000) { fixture.movimientos.activeRecent.get() == 1 }
      historyTab().performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(7_000) { fixture.movimientos.activeRecent.get() == 0 }
      assertEquals(1, fixture.movimientos.activeRanges.get())
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(7_000) { fixture.movimientos.activeRanges.get() == 0 }
      assertEquals(1, fixture.movimientos.activeRecent.get())
    }
  }

  @Test
  fun focusedYearPopupConsumesBackBeforeHistoryRootAndCatalogReturnsHome() {
    withShell {
      historyTab().performClick()
      val year = runBlocking { getString(Res.string.history_year_description, 2026) }
      composeRule.onNodeWithContentDescription(year).performClick()
      recordPopupWindows("before-popup-focus")
      composeRule.onNode(isPopup()).assertExists()
      val popupYear = hasText("2026") and hasAnyAncestor(isPopup())
      composeRule.onNode(popupYear).assertIsDisplayed()
      try {
        composeRule.waitUntil(5_000) { popupWindowState().isReady }
      } finally {
        recordPopupWindows("after-popup-focus-wait")
      }
      systemBack()
      composeRule.onNode(popupYear).assertDoesNotExist()
      composeRule.onNode(isPopup()).assertDoesNotExist()
      composeRule.waitUntil(5_000) { popupWindowState().count == 0 }
      historyTab().assertIsSelected()
      systemBack()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  @Test
  fun explicitCatalogLaunchHasSupportedPrimaryReturnWithoutBlankRoot() {
    withShell(start = UiCatalog) {
      composeRule.onNodeWithText(text(Res.string.ui_catalog_title)).assertExists()
      systemBack()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  private fun historyTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_history)) and hasClickAction()
      )

  private fun historyList() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_history)) and
              SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
      )

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun popupWindowState(): PopupWindowState {
    var state = PopupWindowState(0, false)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val popups =
          WindowInspector.getGlobalWindowViews().filter {
            it.javaClass.name == "androidx.compose.ui.window.PopupLayout"
          }
      val popup = popups.singleOrNull()
      state =
          PopupWindowState(
              popups.size,
              popup != null && popup.isAttachedToWindow && popup.isShown && popup.hasWindowFocus(),
          )
    }
    return state
  }

  private fun recordPopupWindows(phase: String) {
    val nativeWindows = JSONArray()
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      WindowInspector.getGlobalWindowViews().forEach { view ->
        val params = view.layoutParams as? WindowManager.LayoutParams
        nativeWindows.put(
            JSONObject()
                .put("class", view.javaClass.name)
                .put("attached", view.isAttachedToWindow)
                .put("shown", view.isShown)
                .put("focused", view.hasWindowFocus())
                .put("width", view.width)
                .put("height", view.height)
                .put("flags", params?.flags)
                .put("type", params?.type)
        )
      }
    }
    val diagnostic = JSONObject().put("phase", phase).put("windows", nativeWindows)
    InstrumentationRegistry.getInstrumentation()
        .sendStatus(2, Bundle().apply { putString("history_diagnostic", diagnostic.toString()) })
  }

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun restoreShell(fixture: PrimaryDeviceShell) {
    lateinit var snapshot: PrimaryNavigationSnapshot
    composeRule.runOnIdle { snapshot = fixture.saveAndUnmount() }
    composeRule.waitForIdle()
    composeRule.runOnIdle { fixture.restoreNewOwners(snapshot) }
    composeRule.waitForIdle()
  }

  private fun withShell(start: Destination = Home, block: (PrimaryDeviceShell) -> Unit) {
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
              TallybookTheme {
                TallybookViewportOverlayHost { TallybookNavHost(startDestination = start) }
              }
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

private data class PopupWindowState(val count: Int, val isReady: Boolean)
