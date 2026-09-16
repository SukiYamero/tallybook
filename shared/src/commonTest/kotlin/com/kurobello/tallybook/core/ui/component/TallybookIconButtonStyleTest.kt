package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TallybookIconButtonStyleTest {

  @Test
  fun neutralAndBrandStylesMapToDistinctSemanticTokens() {
    val tokens =
        TallybookIconButtonColorTokens(
            neutralContainer = Color.White,
            neutralContent = Color.DarkGray,
            brandContainer = Color.Green,
            brandContent = Color.Black,
            disabledContainer = Color.LightGray,
            disabledContent = Color.Gray,
        )

    val neutral = resolveTallybookIconButtonColors(TallybookIconButtonStyle.Neutral, tokens)
    val brand = resolveTallybookIconButtonColors(TallybookIconButtonStyle.Brand, tokens)

    assertEquals(tokens.neutralContainer, neutral.container)
    assertEquals(tokens.neutralContent, neutral.content)
    assertEquals(tokens.brandContainer, brand.container)
    assertEquals(tokens.brandContent, brand.content)
    assertEquals(tokens.disabledContainer, brand.disabledContainer)
    assertEquals(tokens.disabledContent, brand.disabledContent)
    assertNotEquals(neutral.container, brand.container)
  }
}
