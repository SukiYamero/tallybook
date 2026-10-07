package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class TallybookAnchoredPopupPositionProvider(
    private val verticalOffset: Int,
    private val edgeMargin: Int,
    private val leftSafeInset: Int = 0,
    private val rightSafeInset: Int = 0,
) : PopupPositionProvider {
  override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize,
  ): IntOffset {
    val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
    val minX = leftSafeInset + edgeMargin
    val maxX = maxOf(minX, windowSize.width - rightSafeInset - popupContentSize.width - edgeMargin)
    val availableBelow = windowSize.height - anchorBounds.bottom - verticalOffset
    val availableAbove = anchorBounds.top - verticalOffset
    val maxY = maxOf(edgeMargin, windowSize.height - popupContentSize.height - edgeMargin)
    val y =
        when {
          availableBelow >= popupContentSize.height -> anchorBounds.bottom + verticalOffset
          availableAbove >= popupContentSize.height ->
              anchorBounds.top - verticalOffset - popupContentSize.height
          availableAbove > availableBelow ->
              (anchorBounds.top - verticalOffset - popupContentSize.height).coerceIn(
                  edgeMargin,
                  maxY,
              )
          else -> (anchorBounds.bottom + verticalOffset).coerceIn(edgeMargin, maxY)
        }
    return IntOffset(centeredX.coerceIn(minX, maxX), y)
  }
}
