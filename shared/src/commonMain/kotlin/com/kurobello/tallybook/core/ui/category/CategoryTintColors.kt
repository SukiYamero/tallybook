package com.kurobello.tallybook.core.ui.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.LocalTallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookColors

private const val CATEGORY_CONTAINER_ALPHA = 0.15f
private const val CATEGORY_BORDER_ALPHA = 0.4f

// The tints in the order of TallybookColors.chart. Every accent is a distinct chart hue: the status
// colors sit too close to emerald, red and blue to tell apart in a picker.
private val chartOrder: List<CategoryTint> =
    listOf(
        CategoryTint.EMERALD,
        CategoryTint.BLUE,
        CategoryTint.AMBER,
        CategoryTint.RED,
        CategoryTint.PURPLE,
        CategoryTint.TEAL,
        CategoryTint.INDIGO,
        CategoryTint.ORANGE,
        CategoryTint.PINK,
    )

@Immutable
internal data class CategoryTintColors(
    val content: Color,
    val container: Color,
    val border: Color,
)

// NEUTRAL has no chart accent; tintColors() resolves it through the Material neutral roles.
internal fun CategoryTint.accentColor(colors: TallybookColors): Color? =
    chartOrder.indexOf(this).takeIf { it >= 0 }?.let(colors.chart::get)

@Composable
internal fun CategoryTint.tintColors(): CategoryTintColors {
  val accent = accentColor(LocalTallybookColors.current) ?: return neutralCategoryTintColors()
  return CategoryTintColors(
      content = accent,
      container = accent.copy(alpha = CATEGORY_CONTAINER_ALPHA),
      border = accent.copy(alpha = CATEGORY_BORDER_ALPHA),
  )
}

@Composable
internal fun neutralCategoryTintColors(): CategoryTintColors =
    CategoryTintColors(
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = MaterialTheme.colorScheme.outlineVariant,
    )
