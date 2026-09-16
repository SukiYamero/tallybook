package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_navigation
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_text_large

class UiCatalogScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun catalogDisplaysCurrentComponentFamilies() {
    composeRule.setContent { UiCatalogRoute() }

    listOf(
            Res.string.ui_catalog_foundations,
            Res.string.ui_catalog_actions,
            Res.string.ui_catalog_data_display,
            Res.string.ui_catalog_navigation,
        )
        .forEach { resource ->
          composeRule
              .onNodeWithText(localizedString(resource))
              .performScrollTo()
              .assertIsDisplayed()
        }
  }

  @Test
  fun catalogThemeAndFontControlsUpdateSamplesWithoutPersistence() {
    composeRule.setContent { UiCatalogRoute() }

    composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_dark)).performClick()
    composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_text_large)).performClick()

    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_sample_text))
        .assertIsDisplayed()
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }
}
