package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.navigation.UiCatalog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_title

class UiCatalogScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun catalogRouteDisplaysItsLocalizedTitle() {
    val title = localizedString(Res.string.ui_catalog_title)

    composeRule.setContent { TallybookTheme { TallybookNavHost(startDestination = UiCatalog) } }

    composeRule.onNodeWithText(title).assertIsDisplayed()
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }
}
