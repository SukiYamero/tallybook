package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.graphics.compositeOver
import com.kurobello.tallybook.core.ui.theme.LightTallybookColors
import kotlin.test.Test
import kotlin.test.assertEquals

class TallybookSegmentedControlColorsTest {

  @Test
  fun neutralSelectedContainerUsesAStrongerSurfaceContrast() {
    val expected =
        LightTallybookColors.foreground.primary
            .copy(alpha = neutralSelectedContainerDarkeningAlpha)
            .compositeOver(LightTallybookColors.surface.highest)

    assertEquals(expected, neutralSelectedSegmentContainer(LightTallybookColors))
  }

  private companion object {
    const val neutralSelectedContainerDarkeningAlpha = 0.04f
  }
}
