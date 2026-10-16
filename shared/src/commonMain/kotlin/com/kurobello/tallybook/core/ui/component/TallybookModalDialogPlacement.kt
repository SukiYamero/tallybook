package com.kurobello.tallybook.core.ui.component

/** Vertical band, in pixels from the window top, that a dialog may occupy. */
internal data class DialogBand(val top: Int, val bottom: Int) {
  val height: Int
    get() = (bottom - top).coerceAtLeast(0)
}

internal fun centeredDialogTop(band: DialogBand, dialogHeight: Int): Int =
    band.top + (band.height - dialogHeight).coerceAtLeast(0) / 2

/**
 * Keeps [anchorTop] unless the dialog's bottom would leave [band]; then it moves up just enough.
 */
internal fun anchoredDialogTop(anchorTop: Int, dialogHeight: Int, band: DialogBand): Int =
    minOf(anchorTop, band.bottom - dialogHeight).coerceAtLeast(band.top)

/**
 * Vertical position of a dialog whose height changes while it is open. The first placement centers
 * the dialog in the band without the keyboard and that top edge is kept, so growth extends
 * downward; only the keyboard or the screen edge pushes the dialog up, and it returns once they
 * leave. A different [restBand] (rotation, window resize) centers again.
 */
internal class DialogVerticalPlacement {
  private var restBand: DialogBand? = null
  private var anchorTop = 0

  fun top(restBand: DialogBand, currentBand: DialogBand, dialogHeight: Int): Int {
    if (this.restBand != restBand) {
      this.restBand = restBand
      anchorTop = centeredDialogTop(restBand, dialogHeight)
    }
    return anchoredDialogTop(anchorTop, dialogHeight, currentBand)
  }
}
