package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_navigation
import tallybook.shared.generated.resources.ui_catalog_sample_body_emphasis
import tallybook.shared.generated.resources.ui_catalog_sample_body_small
import tallybook.shared.generated.resources.ui_catalog_sample_caption
import tallybook.shared.generated.resources.ui_catalog_sample_label
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_system
import tallybook.shared.generated.resources.ui_catalog_text_large
import tallybook.shared.generated.resources.ui_catalog_text_normal

class UiCatalogScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun catalogDisplaysCurrentComponentFamilies() {
    composeRule.setContent { UiCatalogRoute() }

    listOf(
            Res.string.ui_catalog_foundations,
            Res.string.ui_catalog_actions,
            Res.string.ui_catalog_data_display,
        )
        .forEach { resource ->
          composeRule
              .onNodeWithText(localizedString(resource))
              .performScrollTo()
              .assertIsDisplayed()
        }
    composeRule.onNode(hasScrollAction()).performScrollToIndex(4)
    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_navigation))
        .assertIsDisplayed()
    composeRule.onNode(hasScrollAction()).performScrollToIndex(5)
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.loading))
        .assertIsDisplayed()
  }

  @Test
  fun catalogDisplaysCompleteTypographyContract() {
    composeRule.setContent { UiCatalogRoute() }

    listOf(
            Res.string.ui_catalog_sample_text,
            Res.string.ui_catalog_sample_caption,
            Res.string.ui_catalog_sample_label,
            Res.string.ui_catalog_sample_body_small,
            Res.string.ui_catalog_sample_body_emphasis,
        )
        .forEach { resource ->
          composeRule
              .onNodeWithText(localizedString(resource))
              .performScrollTo()
              .assertIsDisplayed()
        }
  }

  @Test
  fun catalogBottomNavigationHasNoSelectedProductDestinationAndPreservesActions() {
    val unavailableDestinations =
        listOf(
            Res.string.navigation_home,
            Res.string.navigation_history,
            Res.string.navigation_search,
            Res.string.navigation_profile,
        )
    composeRule.setContent { UiCatalogRoute() }

    unavailableDestinations.forEach { destination ->
      composeRule
          .onNodeWithContentDescription(localizedString(destination))
          .assertHasNoClickAction()
          .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.navigation_add_movement))
        .assertHasClickAction()
  }

  @Test
  fun catalogThemeAndFontControlsUpdateSamplesWithoutPersistence() {
    var showRoute by mutableStateOf(true)
    composeRule.setContent { if (showRoute) UiCatalogRoute() }

    val dark = localizedString(Res.string.ui_catalog_dark)
    val system = localizedString(Res.string.ui_catalog_system)
    val large = localizedString(Res.string.ui_catalog_text_large)
    val normal = localizedString(Res.string.ui_catalog_text_normal)
    val sampleText = localizedString(Res.string.ui_catalog_sample_text)
    val normalSampleHeight =
        composeRule.onNodeWithText(sampleText).fetchSemanticsNode().boundsInRoot.height

    composeRule.onNodeWithText(dark).performClick().assertIsSelected()
    composeRule.onNodeWithText(large).performClick().assertIsSelected()
    val largeSampleHeight =
        composeRule.onNodeWithText(sampleText).fetchSemanticsNode().boundsInRoot.height

    assertTrue(largeSampleHeight > normalSampleHeight)

    composeRule.runOnIdle { showRoute = false }
    composeRule.runOnIdle { showRoute = true }

    composeRule.onNodeWithText(system).assertIsSelected()
    composeRule.onNodeWithText(normal).assertIsSelected()
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }
}
