package com.kurobello.tallybook.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookAppShell(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    overlay: (@Composable (Dp) -> Unit)? = null,
    content: @Composable (Dp) -> Unit,
) {
  SubcomposeLayout(modifier.background(TallybookTheme.colors.canvas)) { constraints ->
    require(constraints.hasBoundedWidth && constraints.hasBoundedHeight)
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val bar =
        subcompose(ShellSlot.Bar) { bottomBar?.invoke() }
            .map { it.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = height)) }
    val occupiedHeight = bar.maxOfOrNull { it.height } ?: 0
    val body =
        subcompose(ShellSlot.Content) { content(occupiedHeight.toDp()) }
            .map { it.measure(Constraints.fixed(width, height)) }
    val foreground =
        subcompose(ShellSlot.Overlay) { overlay?.invoke(occupiedHeight.toDp()) }
            .map { it.measure(Constraints.fixed(width, height)) }
    layout(width, height) {
      body.forEach { it.placeRelative(0, 0) }
      bar.forEach { it.placeRelative(0, height - it.height) }
      foreground.forEach { it.placeRelative(0, 0) }
    }
  }
}

private enum class ShellSlot {
  Bar,
  Content,
  Overlay,
}
