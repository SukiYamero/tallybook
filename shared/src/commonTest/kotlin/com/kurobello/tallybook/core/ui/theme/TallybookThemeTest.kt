package com.kurobello.tallybook.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

class TallybookThemeTest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun lightAndDarkThemesProvideDifferentSemanticCanvases() = runComposeUiTest {
    var darkTheme by mutableStateOf(false)
    var currentCanvas = Color.Unspecified

    setContent {
      TallybookTheme(darkTheme = darkTheme) { currentCanvas = TallybookTheme.colors.canvas }
    }
    waitForIdle()
    val lightCanvas = currentCanvas

    runOnIdle { darkTheme = true }
    waitForIdle()

    assertNotEquals(Color.Unspecified, lightCanvas)
    assertNotEquals(lightCanvas, currentCanvas)
  }
}
