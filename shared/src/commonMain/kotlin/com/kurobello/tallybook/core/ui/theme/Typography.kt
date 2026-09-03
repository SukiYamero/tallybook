package com.kurobello.tallybook.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.manrope_bold
import tallybook.shared.generated.resources.manrope_extrabold
import tallybook.shared.generated.resources.manrope_medium
import tallybook.shared.generated.resources.manrope_regular
import tallybook.shared.generated.resources.manrope_semibold

@Composable
private fun manropeFontFamily(): FontFamily {
  val regular = Font(Res.font.manrope_regular, FontWeight.Normal)
  val medium = Font(Res.font.manrope_medium, FontWeight.Medium)
  val semiBold = Font(Res.font.manrope_semibold, FontWeight.SemiBold)
  val bold = Font(Res.font.manrope_bold, FontWeight.Bold)
  val extraBold = Font(Res.font.manrope_extrabold, FontWeight.ExtraBold)
  return remember(regular, medium, semiBold, bold, extraBold) {
    FontFamily(regular, medium, semiBold, bold, extraBold)
  }
}

@Composable
internal fun tallybookTypography(): Typography {
  val manrope = manropeFontFamily()
  return remember(manrope) { Typography(fontFamily = manrope) }
}
