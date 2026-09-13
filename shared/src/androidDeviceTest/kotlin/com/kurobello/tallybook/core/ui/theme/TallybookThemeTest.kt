package com.kurobello.tallybook.core.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookThemeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun darkThemeProvidesReadableContentColor() {
    var contentColor = Color.Unspecified

    composeRule.setContent {
      TallybookTheme(darkTheme = true) { contentColor = LocalContentColor.current }
    }

    composeRule.runOnIdle { assertEquals(DarkColorScheme.onBackground, contentColor) }
  }
}
