package com.kurobello.tallybook.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.manrope_bold
import tallybook.shared.generated.resources.manrope_extrabold
import tallybook.shared.generated.resources.manrope_medium
import tallybook.shared.generated.resources.manrope_regular
import tallybook.shared.generated.resources.manrope_semibold

@Immutable
internal data class TallybookTypography(
    val caption: TextStyle,
    val label: TextStyle,
    val bodySmall: TextStyle,
    val body: TextStyle,
    val bodyEmphasis: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
)

internal object TallybookTypeScale {
  val size10 = 10.sp
  val size11 = 11.sp
  val size12 = 12.sp
  val size13 = 13.sp
  val size14 = 14.sp
  val size15 = 15.sp
  val size16 = 16.sp
  val size17 = 17.sp
  val size18 = 18.sp
  val size19 = 19.sp
  val size20 = 20.sp
  val size21 = 21.sp
  val size22 = 22.sp
  val size28 = 28.sp
  val size30 = 30.sp
}

internal val LocalTallybookTypography = staticCompositionLocalOf {
  createTallybookTypography(FontFamily.Default)
}

@Composable
internal fun rememberTallybookTypography(): TallybookTypography {
  val regular = Font(Res.font.manrope_regular, FontWeight.Normal)
  val medium = Font(Res.font.manrope_medium, FontWeight.Medium)
  val semiBold = Font(Res.font.manrope_semibold, FontWeight.SemiBold)
  val bold = Font(Res.font.manrope_bold, FontWeight.Bold)
  val extraBold = Font(Res.font.manrope_extrabold, FontWeight.ExtraBold)
  val manrope =
      remember(regular, medium, semiBold, bold, extraBold) {
        FontFamily(regular, medium, semiBold, bold, extraBold)
      }
  return remember(manrope) { createTallybookTypography(manrope) }
}

internal fun legacyMaterialTypography(typography: TallybookTypography): Typography =
    Typography(
        bodyLarge = typography.body,
        bodyMedium = typography.body,
        bodySmall = typography.bodySmall,
        labelLarge = typography.label,
        labelMedium = typography.label,
        labelSmall = typography.caption,
        titleLarge = typography.headline,
        titleMedium = typography.title,
        titleSmall = typography.bodyEmphasis,
        headlineLarge = typography.headline,
        headlineMedium = typography.headline,
        headlineSmall = typography.title,
        displayLarge = typography.headline,
        displayMedium = typography.headline,
        displaySmall = typography.headline,
    )

private fun createTallybookTypography(fontFamily: FontFamily): TallybookTypography =
    TallybookTypography(
        caption =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size12,
                lineHeight = TallybookTypeScale.size16,
                fontWeight = FontWeight.Medium,
            ),
        label =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size13,
                lineHeight = TallybookTypeScale.size18,
                fontWeight = FontWeight.SemiBold,
            ),
        bodySmall =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size14,
                lineHeight = TallybookTypeScale.size20,
                fontWeight = FontWeight.Medium,
            ),
        body =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size15,
                lineHeight = TallybookTypeScale.size21,
                fontWeight = FontWeight.Normal,
            ),
        bodyEmphasis =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size15,
                lineHeight = TallybookTypeScale.size21,
                fontWeight = FontWeight.Bold,
            ),
        title =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size18,
                lineHeight = TallybookTypeScale.size22,
                fontWeight = FontWeight.Bold,
            ),
        headline =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = TallybookTypeScale.size21,
                lineHeight = TallybookTypeScale.size28,
                fontWeight = FontWeight.ExtraBold,
            ),
    )
