package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookSnackbarTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun messageAnnouncesPolitelyAndControlsDeliverTheirRenderedIdentityOncePerClick() {
    val actions = mutableListOf<Pair<Long, Int>>()
    val dismissals = mutableListOf<Triple<Long, Int, TallybookSnackbarDismissReason>>()
    composeRule.setContent {
      TallybookTheme {
        TallybookSnackbarHost(
            message = message(),
            isActive = true,
            closeLabel = "Close notification",
            onAction = { token, revision -> actions += token to revision },
            onDismiss = { token, revision, reason ->
              dismissals += Triple(token, revision, reason)
            },
        )
      }
    }
    composeRule
        .onNodeWithText("Example saved")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    composeRule.onNodeWithText("Undo").assertHasClickAction().performClick()
    composeRule
        .onNodeWithContentDescription("Close notification")
        .assertHasClickAction()
        .performClick()
    composeRule.runOnIdle {
      assertEquals(listOf(7L to 2), actions)
      assertEquals(listOf(Triple(7L, 2, TallybookSnackbarDismissReason.Close)), dismissals)
    }
  }

  @Test
  fun busyRemovesActionInteractionAndDisablesCloseWithoutClickSemantics() {
    composeRule.mainClock.autoAdvance = false
    var actions = 0
    var dismissals = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookSnackbarHost(
            message = message().copy(isBusy = true),
            isActive = true,
            closeLabel = "Close notification",
            onAction = { _, _ -> actions++ },
            onDismiss = { _, _, _ -> dismissals++ },
        )
      }
    }
    composeRule.onNodeWithText("Undo").assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription("Close notification")
        .assertIsNotEnabled()
        .assertHasNoClickAction()
    composeRule.runOnIdle {
      assertEquals(0, actions)
      assertEquals(0, dismissals)
    }
  }

  @Test
  fun timedMessagesKeepTheirActionWithoutACloseControl() {
    val current = mutableStateOf(message().copy(duration = TallybookSnackbarDuration.Short))
    composeRule.setContent {
      TallybookTheme {
        TallybookSnackbarHost(
            current.value,
            true,
            "Close notification",
            { _, _ -> },
            { _, _, _ -> },
        )
      }
    }
    composeRule.onNodeWithText("Undo").assertHasClickAction()
    composeRule.onNodeWithContentDescription("Close notification").assertDoesNotExist()
    composeRule.runOnIdle {
      current.value = current.value.copy(duration = TallybookSnackbarDuration.Long)
    }
    composeRule.onNodeWithContentDescription("Close notification").assertDoesNotExist()
  }

  @Test
  fun accessibilityWithoutATimeoutAddsAnExplicitCloseControl() {
    composeRule.setContent {
      CompositionLocalProvider(LocalAccessibilityManager provides NoTimeoutAccessibilityManager) {
        TallybookTheme {
          TallybookSnackbarHost(
              message().copy(duration = TallybookSnackbarDuration.Short),
              true,
              "Close notification",
              { _, _ -> },
              { _, _, _ -> },
          )
        }
      }
    }
    composeRule.onNodeWithContentDescription("Close notification").assertHasClickAction()
  }

  @Test
  fun busyProgressPrecedesTheMessageInBothLayoutDirections() {
    composeRule.mainClock.autoAdvance = false
    val direction = mutableStateOf(LayoutDirection.Ltr)
    composeRule.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
        TallybookTheme {
          TallybookSnackbarHost(
              message().copy(isBusy = true),
              true,
              "Close notification",
              { _, _ -> },
              { _, _, _ -> },
              Modifier.width(320.dp),
          )
        }
      }
    }
    val progress =
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        )
    val text = composeRule.onNodeWithText("Example saved")
    assertTrue(progress.getUnclippedBoundsInRoot().right <= text.getUnclippedBoundsInRoot().left)
    composeRule.runOnIdle { direction.value = LayoutDirection.Rtl }
    composeRule.mainClock.advanceTimeByFrame()
    assertTrue(progress.getUnclippedBoundsInRoot().left >= text.getUnclippedBoundsInRoot().right)
    composeRule.onNodeWithText("Undo").assertDoesNotExist()
  }

  @Test
  fun inactiveHostRemovesMessageAndControlsFromTheTree() {
    val active = mutableStateOf(true)
    composeRule.setContent {
      TallybookTheme {
        TallybookSnackbarHost(
            message(),
            active.value,
            "Close notification",
            { _, _ -> },
            { _, _, _ -> },
        )
      }
    }
    composeRule.onNodeWithText("Example saved").assertIsDisplayed()
    composeRule.runOnIdle { active.value = false }
    composeRule.onNodeWithText("Example saved").assertDoesNotExist()
    composeRule.onNodeWithText("Undo").assertDoesNotExist()
    composeRule.onNodeWithContentDescription("Close notification").assertDoesNotExist()
  }

  @Test
  fun largeTextAndLongActionWrapWithoutClippingAndRetainFullTouchTargets() {
    val copy = "The sample is ready to review with all of its details"
    val action = "Review the complete sample again"
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme {
          TallybookSnackbarHost(
              message().copy(message = copy, actionLabel = action),
              true,
              "Close notification",
              { _, _ -> },
              { _, _, _ -> },
              Modifier.width(280.dp).testTag("snackbar"),
          )
        }
      }
    }
    val message = composeRule.onNodeWithText(copy)
    val label = composeRule.onNodeWithText(action, useUnmergedTree = true)
    val textResults = mutableListOf<TextLayoutResult>()
    label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textResults) }
    assertTrue(textResults.single().lineCount > 1)
    assertFalse(textResults.single().hasVisualOverflow)
    val messageBounds = message.getUnclippedBoundsInRoot()
    val actionBounds = composeRule.onNodeWithText(action).getUnclippedBoundsInRoot()
    val closeBounds =
        composeRule.onNodeWithContentDescription("Close notification").getUnclippedBoundsInRoot()
    val rootBounds = composeRule.onNodeWithTag("snackbar").getUnclippedBoundsInRoot()
    assertTrue(actionBounds.top >= messageBounds.bottom)
    listOf(actionBounds, closeBounds).forEach {
      assertTrue(it.right - it.left >= 48.dp)
      assertTrue(it.bottom - it.top >= 48.dp)
      assertTrue(it.right <= rootBounds.right)
      assertTrue(it.bottom <= rootBounds.bottom)
    }
  }

  @Test
  fun plainRenderingAcceptsSlotsAndCallerPlacementAtTheRoot() {
    composeRule.setContent {
      TallybookTheme {
        Box(Modifier.padding(start = 24.dp, top = 32.dp)) {
          TallybookSnackbar(
              message = "Preview message",
              modifier = Modifier.width(260.dp).testTag("snackbar-root"),
              actionContent = { TallybookTextButton({}) { TallybookText("Sample action") } },
              dismissContent = null,
          )
        }
      }
    }
    val bounds = composeRule.onNodeWithTag("snackbar-root").getUnclippedBoundsInRoot()
    assertEquals(24.dp, bounds.left)
    assertEquals(32.dp, bounds.top)
    assertEquals(260.dp, bounds.right - bounds.left)
    composeRule.onNodeWithText("Preview message").assertIsDisplayed()
    composeRule.onNodeWithText("Sample action").assertIsDisplayed()
  }

  private fun message() =
      TallybookSnackbarMessage(7, 2, "Example saved", TallybookSnackbarDuration.Indefinite, "Undo")
}

private object NoTimeoutAccessibilityManager : AccessibilityManager {
  override fun calculateRecommendedTimeoutMillis(
      originalTimeoutMillis: Long,
      containsIcons: Boolean,
      containsText: Boolean,
      containsControls: Boolean,
  ): Long = Long.MAX_VALUE
}
