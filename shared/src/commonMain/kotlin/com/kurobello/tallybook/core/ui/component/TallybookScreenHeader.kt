package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

/**
 * A screen title row whose height is the title's own, like the title-only headers of Search and
 * Profile, so the title line lands at the same position on every screen. The [navigation] and
 * [actions] controls are centered on the title and, being taller than it (48 dp targets), extend
 * past the row instead of pushing the title down.
 */
@Composable
internal fun TallybookScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
  val gap = TallybookTheme.spacing.space4
  Layout(
      contents =
          listOf(
              { Box { navigation() } },
              {
                TallybookText(
                    title,
                    modifier = Modifier.semantics { heading() },
                    style = TallybookTheme.typography.headline,
                )
              },
              { Box { actions() } },
          ),
      modifier = modifier.fillMaxWidth(),
  ) { (navigationMeasurables, titleMeasurables, actionMeasurables), constraints ->
    val loose = constraints.copy(minWidth = 0, minHeight = 0)
    val leading = navigationMeasurables.single().measure(loose)
    val trailing = actionMeasurables.single().measure(loose)
    val gapPx = gap.roundToPx()
    val leadingEnd = if (leading.width > 0) leading.width + gapPx else 0
    val trailingStart = if (trailing.width > 0) trailing.width + gapPx else 0
    val titleWidth = (constraints.maxWidth - leadingEnd - trailingStart).coerceAtLeast(0)
    val titlePlaceable =
        titleMeasurables.single().measure(loose.copy(minWidth = titleWidth, maxWidth = titleWidth))
    val height = maxOf(titlePlaceable.height, constraints.minHeight)
    layout(constraints.maxWidth, height) {
      leading.placeRelative(0, (height - leading.height) / 2)
      titlePlaceable.placeRelative(leadingEnd, (height - titlePlaceable.height) / 2)
      trailing.placeRelative(
          constraints.maxWidth - trailing.width,
          (height - trailing.height) / 2,
      )
    }
  }
}
