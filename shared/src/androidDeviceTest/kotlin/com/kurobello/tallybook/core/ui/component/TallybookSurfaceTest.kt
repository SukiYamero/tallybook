package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.kurobello.tallybook.core.ui.theme.DarkColorScheme
import com.kurobello.tallybook.core.ui.theme.DarkTallybookColors
import com.kurobello.tallybook.core.ui.theme.LightColorScheme
import com.kurobello.tallybook.core.ui.theme.LightTallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookMotion
import kotlin.test.assertEquals
import org.junit.Test

class TallybookSurfaceTest {

  @Test
  fun brandPrimaryIsConsistentAcrossThemeModes() {
    val brandPrimary = Color(0xFFA8C9B0)

    assertEquals(brandPrimary, LightTallybookColors.brand.primary)
    assertEquals(brandPrimary, DarkTallybookColors.brand.primary)
  }

  @Test
  fun sharedBrandUsesDarkForegroundInBothThemesForContrast() {
    val onPrimary = Color(0xFF06231A)

    assertEquals(onPrimary, LightTallybookColors.brand.onPrimary)
    assertEquals(onPrimary, DarkTallybookColors.brand.onPrimary)
  }

  @Test
  fun secondaryBrandUsesEucalyptusWithLightForegroundInBothThemes() {
    val secondary = Color(0xFF3F6B56)
    val onSecondary = Color(0xFFFFFFFF)

    assertEquals(secondary, LightTallybookColors.brand.secondary)
    assertEquals(secondary, DarkTallybookColors.brand.secondary)
    assertEquals(onSecondary, LightTallybookColors.brand.onSecondary)
    assertEquals(onSecondary, DarkTallybookColors.brand.onSecondary)
  }

  @Test
  fun inputCursorAdaptsForContrastAgainstEachThemeSurface() {
    assertEquals(Color(0xFF3F6B56), LightTallybookColors.input.cursor)
    assertEquals(Color(0xFFA8C9B0), DarkTallybookColors.input.cursor)
  }

  @Test
  fun balanceCardUsesEucalyptusContainerOnlyInDarkTheme() {
    assertEquals(Color(0xFFDDEFE4), LightColorScheme.primaryContainer)
    assertEquals(Color(0xFF3F6B56), DarkColorScheme.primaryContainer)
    assertEquals(Color(0xFF0E4B34), LightColorScheme.onPrimaryContainer)
    assertEquals(Color(0xFFFFFFFF), DarkColorScheme.onPrimaryContainer)
  }

  @Test
  fun pressedOverlayUsesFivePercentContrastInBothThemes() {
    assertEquals(Color(0x0C1C1C14), LightTallybookColors.interaction.pressedOverlay)
    assertEquals(Color(0x0CFFFFFF), DarkTallybookColors.interaction.pressedOverlay)
  }

  @Test
  fun pressedSurfaceCompositesTheTonalOverlayOverItsContainer() {
    val container = Color(0xFF12A873)
    val overlay = Color(0x141C1C14)

    assertEquals(
        container,
        resolveTallybookSurfaceContainerColor(
            containerColor = container,
            pressedOverlay = overlay,
            isPressed = false,
        ),
    )
    assertEquals(
        overlay.compositeOver(container),
        resolveTallybookSurfaceContainerColor(
            containerColor = container,
            pressedOverlay = overlay,
            isPressed = true,
        ),
    )
  }

  @Test
  fun tapFeedbackKeepsTheOverlayVisibleAfterPointerRelease() {
    val container = Color(0xFF3F6B56)
    val overlay = Color(0x1FFFFFFF)

    assertEquals(
        overlay.compositeOver(container),
        resolveTallybookSurfaceContainerColor(
            containerColor = container,
            pressedOverlay = overlay,
            isPressed = false,
            hasTapFeedback = true,
        ),
    )
    assertEquals(100, TallybookMotion.tapFeedbackMillis)
  }
}
