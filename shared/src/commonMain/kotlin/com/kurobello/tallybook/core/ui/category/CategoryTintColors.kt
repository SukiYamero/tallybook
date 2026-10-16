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
private const val EMERALD_CHART_INDEX = 0
private const val BLUE_CHART_INDEX = 1
private const val AMBER_CHART_INDEX = 2
private const val ROSE_CHART_INDEX = 3
private const val PURPLE_CHART_INDEX = 4

@Immutable
internal data class CategoryTintColors(
    val content: Color,
    val container: Color,
    val border: Color,
)

// NEUTRAL has no chart accent; tintColors() resolves it through the Material neutral roles.
internal fun CategoryTint.accentColor(colors: TallybookColors): Color? =
    when (this) {
      CategoryTint.EMERALD -> colors.chart[EMERALD_CHART_INDEX]
      CategoryTint.BLUE -> colors.chart[BLUE_CHART_INDEX]
      CategoryTint.AMBER -> colors.chart[AMBER_CHART_INDEX]
      CategoryTint.ROSE -> colors.chart[ROSE_CHART_INDEX]
      CategoryTint.PURPLE -> colors.chart[PURPLE_CHART_INDEX]
      CategoryTint.SUCCESS -> colors.status.success.base
      CategoryTint.DANGER -> colors.status.danger.base
      CategoryTint.INFO -> colors.status.info
      CategoryTint.NEUTRAL -> null
    }

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
