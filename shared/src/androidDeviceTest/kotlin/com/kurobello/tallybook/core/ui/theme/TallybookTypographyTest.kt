package com.kurobello.tallybook.core.ui.theme

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class TallybookTypographyTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun normalTypographyMovesEveryRoleToTheNextSizeStep() {
    var typography: TallybookTypography? = null
    composeRule.setContent {
      TallybookTheme {
        val currentTypography = TallybookTheme.typography
        SideEffect { typography = currentTypography }
      }
    }

    composeRule.runOnIdle {
      assertEquals(12.sp, typography?.caption?.fontSize)
      assertEquals(13.sp, typography?.label?.fontSize)
      assertEquals(14.sp, typography?.bodySmall?.fontSize)
      assertEquals(15.sp, typography?.body?.fontSize)
      assertEquals(15.sp, typography?.bodyEmphasis?.fontSize)
      assertEquals(18.sp, typography?.title?.fontSize)
      assertEquals(21.sp, typography?.headline?.fontSize)
    }
  }

  @Test
  fun iconScalePreservesTheMovementSummaryIconSize() {
    assertEquals(16.dp, TallybookDimensions.iconSmall)
    assertEquals(20.dp, TallybookDimensions.iconMedium)
    assertEquals(24.dp, TallybookDimensions.iconLarge)
    assertEquals(TallybookDimensions.iconSmall, TallybookDimensions.movementSummaryIcon)
  }
}
