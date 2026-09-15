package com.kurobello.tallybook.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier

internal object TallybookTheme {
  val colors: TallybookColors
    @Composable @ReadOnlyComposable get() = LocalTallybookColors.current

  val typography: TallybookTypography
    @Composable @ReadOnlyComposable get() = LocalTallybookTypography.current

  val spacing: TallybookSpacing
    get() = TallybookSpacing

  val dimensions: TallybookDimensions
    get() = TallybookDimensions

  val radii: TallybookRadii
    get() = TallybookRadii

  val motion: TallybookMotion
    get() = TallybookMotion

  @Composable
  operator fun invoke(
      darkTheme: Boolean = isSystemInDarkTheme(),
      content: @Composable () -> Unit,
  ) {
    val colors = if (darkTheme) DarkTallybookColors else LightTallybookColors
    val typography = rememberTallybookTypography()
    CompositionLocalProvider(
        LocalTallybookColors provides colors,
        LocalTallybookTypography provides typography,
        LocalContentColor provides colors.foreground.primary,
    ) {
      MaterialTheme(
          colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
          typography = legacyMaterialTypography(typography),
      ) {
        Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) { content() }
      }
    }
  }
}
