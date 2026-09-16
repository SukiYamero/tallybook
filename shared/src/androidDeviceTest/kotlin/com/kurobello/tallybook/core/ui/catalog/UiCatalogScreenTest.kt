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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_clear_search
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_disabled_icon_button
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_icon_buttons
import tallybook.shared.generated.resources.ui_catalog_loading_states
import tallybook.shared.generated.resources.ui_catalog_navigation
import tallybook.shared.generated.resources.ui_catalog_sample_body_emphasis
import tallybook.shared.generated.resources.ui_catalog_sample_body_small
import tallybook.shared.generated.resources.ui_catalog_sample_caption
import tallybook.shared.generated.resources.ui_catalog_sample_label
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_search_fields
import tallybook.shared.generated.resources.ui_catalog_segmented_controls
import tallybook.shared.generated.resources.ui_catalog_selection_fields
import tallybook.shared.generated.resources.ui_catalog_system
import tallybook.shared.generated.resources.ui_catalog_text_field_label
import tallybook.shared.generated.resources.ui_catalog_text_fields
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
    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_navigation))
        .performScrollTo()
        .assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.loading))
        .performScrollTo()
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

  @Test
  fun catalogExposesLocalizedControlFamiliesAndRepresentativeStaticStates() {
    composeRule.setContent { UiCatalogRoute() }

    controlFamilyHeadings.forEach { heading ->
      composeRule.onNodeWithText(localizedString(heading)).performScrollTo().assertIsDisplayed()
    }

    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.ui_catalog_disabled_icon_button))
        .performScrollTo()
        .assertIsNotEnabled()
    composeRule
        .onNode(
            hasText(localizedString(Res.string.category)) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    localizedString(Res.string.category_required),
                )
        )
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun catalogEditableTextAndClearableSearchUpdateTheirRenderedState() {
    composeRule.setContent { UiCatalogRoute() }

    val fieldLabel = localizedString(Res.string.ui_catalog_text_field_label)
    composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_text_fields)).performScrollTo()
    composeRule.onAllNodesWithContentDescription(fieldLabel)[0].performTextInput("Trip")
    composeRule.onNodeWithText("Trip").assertIsDisplayed()

    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_search_fields))
        .performScrollTo()
    composeRule
        .onNode(
            hasContentDescription(localizedString(Res.string.ui_catalog_clear_search)) and
                isEnabled()
        )
        .performClick()
    composeRule
        .onNodeWithText(localizedString(Res.string.home_search_placeholder))
        .assertIsDisplayed()
    composeRule
        .onNode(
            hasContentDescription(localizedString(Res.string.ui_catalog_clear_search)) and
                isEnabled()
        )
        .assertDoesNotExist()
  }

  @Test
  fun catalogSegmentSelectionAndLoadingRegionRemainConnectedToSampleState() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { UiCatalogRoute() }

    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_segmented_controls))
        .performScrollTo()
    composeRule
        .onNode(hasText(localizedString(Res.string.expense)) and isEnabled())
        .assertIsSelected()
    composeRule
        .onNode(hasText(localizedString(Res.string.income)) and isEnabled())
        .performClick()
        .assertIsSelected()

    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_loading_states))
        .performScrollTo()
    val loadingRegion =
        composeRule.onNodeWithTag(CATALOG_LOADING_REGION_TAG).fetchSemanticsNode().boundsInRoot
    val loadingIndicator =
        composeRule
            .onNodeWithContentDescription(localizedString(Res.string.loading))
            .fetchSemanticsNode()
            .boundsInRoot

    assertEquals(loadingRegion.center.x, loadingIndicator.center.x, absoluteTolerance = 1f)
    assertEquals(loadingRegion.center.y, loadingIndicator.center.y, absoluteTolerance = 1f)
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }

  private companion object {
    val controlFamilyHeadings =
        listOf(
            Res.string.ui_catalog_icon_buttons,
            Res.string.ui_catalog_text_fields,
            Res.string.ui_catalog_search_fields,
            Res.string.ui_catalog_segmented_controls,
            Res.string.ui_catalog_selection_fields,
            Res.string.ui_catalog_loading_states,
        )
  }
}
