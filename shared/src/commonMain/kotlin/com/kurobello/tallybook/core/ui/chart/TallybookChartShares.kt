package com.kurobello.tallybook.core.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_share
import tallybook.shared.generated.resources.statistics_share_small

/**
 * The palette entry of the [rank]-th group, skipping the red one that reads as gasto; null once the
 * palette runs out, where the group takes the "Otras" color.
 */
internal fun chartPaletteIndex(rank: Int, paletteSize: Int): Int? {
  val index = if (rank < SKIPPED_PALETTE_INDEX) rank else rank + 1
  return index.takeIf { rank >= 0 && it < paletteSize }
}

/** The [rank]-th group's color, see [chartPaletteIndex]. */
@Composable
internal fun chartRankColor(rank: Int): Color {
  val palette = TallybookTheme.colors.chart
  return chartPaletteIndex(rank, palette.size)?.let(palette::get) ?: chartRestColor()
}

/** "Otras" and every group past the palette. */
@Composable internal fun chartRestColor(): Color = TallybookTheme.colors.foreground.faint

/** Rounded half up; every listed group has gasto, so one that rounds to 0% reads as under 1%. */
@Composable
internal fun shareText(basisPoints: Int): String =
    if (basisPoints in 0 until HALF_PERCENT) stringResource(Res.string.statistics_share_small)
    else
        stringResource(
            Res.string.statistics_share,
            (basisPoints + HALF_PERCENT) / BASIS_POINTS_PER_PERCENT,
        )

internal fun shareFraction(basisPoints: Int): Float =
    basisPoints.toFloat() / (BASIS_POINTS_PER_PERCENT * BASIS_POINTS_PER_PERCENT)

private const val SKIPPED_PALETTE_INDEX = 3
private const val BASIS_POINTS_PER_PERCENT = 100
private const val HALF_PERCENT = 50
