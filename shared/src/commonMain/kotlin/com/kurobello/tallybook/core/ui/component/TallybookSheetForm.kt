package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal data class TallybookSheetFormLayout(
    val horizontalPadding: Dp = TallybookTheme.dimensions.screenHorizontal,
    val minimumBodyHeight: Dp = Dp.Unspecified,
    val maximumBodyHeight: Dp = TallybookTheme.dimensions.sheetFormMaximumHeight,
)

@Stable
internal interface TallybookSheetFormScope : ColumnScope {
  /** Keeps this input centered in the body's part above the keyboard while it has focus. */
  @Composable fun Modifier.revealWhenFocused(): Modifier
}

/**
 * A modal sheet form whose sheet stays put while the keyboard is open: the [header] and [footer]
 * sit outside the scrolling body, the body keeps the height it had at rest and gains the keyboard's
 * height as scroll room, and the footer gives way to the keyboard (it would sit under it) until the
 * keyboard closes, when the body scrolls back to the top.
 */
@Composable
internal fun TallybookSheetForm(
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    layout: TallybookSheetFormLayout = TallybookSheetFormLayout(),
    content: @Composable TallybookSheetFormScope.() -> Unit,
) {
  val scrollState = rememberScrollState()
  val viewport = rememberScrollableInputViewport()
  val density = LocalDensity.current
  val restingBodyHeight = remember { mutableStateOf<Dp?>(null) }
  val imeBottom = WindowInsets.ime.getBottom(density)
  val isImeVisible = imeBottom > 0
  LaunchedEffect(isImeVisible) { if (!isImeVisible) scrollState.scrollTo(0) }
  val maximumBodyHeight =
      if (isImeVisible) restingBodyHeight.value ?: layout.maximumBodyHeight
      else layout.maximumBodyHeight

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = layout.horizontalPadding)
              .clearFocusOnUnconsumedTap()
  ) {
    header()
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space16))
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = layout.minimumBodyHeight, max = maximumBodyHeight)
                .verticalScroll(scrollState)
                .captureScrollableInputViewport(viewport)
                .captureRestingHeight(isImeVisible, restingBodyHeight)
    ) {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .then(
                      if (isImeVisible)
                          Modifier.padding(bottom = with(density) { imeBottom.toDp() })
                      else Modifier
                  ),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
      ) {
        val scope =
            remember(this, scrollState, viewport) { SheetFormScope(this, scrollState, viewport) }
        scope.content()
      }
    }
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space18))
    if (isImeVisible) {
      Spacer(
          Modifier.fillMaxWidth()
              .height(TallybookTheme.dimensions.minimumTouchTarget + TallybookTheme.spacing.space8)
      )
    } else {
      footer()
    }
  }
}

private class SheetFormScope(
    column: ColumnScope,
    private val scrollState: ScrollState,
    private val viewport: ScrollableInputViewport,
) : TallybookSheetFormScope, ColumnScope by column {
  @Composable
  override fun Modifier.revealWhenFocused(): Modifier = centerFocusedInputIn(scrollState, viewport)
}

@Composable
private fun Modifier.captureRestingHeight(
    isImeVisible: Boolean,
    restingHeight: MutableState<Dp?>,
): Modifier {
  val density = LocalDensity.current
  return onSizeChanged { size ->
    if (!isImeVisible) {
      restingHeight.value = with(density) { size.height.toDp() }
    }
  }
}
