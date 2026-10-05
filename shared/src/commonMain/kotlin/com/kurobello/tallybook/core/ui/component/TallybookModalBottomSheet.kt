package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.composeunstyled.DragIndication
import com.composeunstyled.ModalBottomSheetProperties
import com.composeunstyled.ModalBottomSheetScope
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
fun TallybookModalBottomSheet(
    visible: Boolean,
    paneTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) =
    TallybookModalBottomSheet(
        visible = visible,
        paneTitle = paneTitle,
        callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss),
        layout = ModalBottomSheetLayout(modifier = modifier),
        content = content,
    )

@Composable
internal fun TallybookModalBottomSheet(
    visible: Boolean,
    paneTitle: String,
    callbacks: ModalBottomSheetCallbacks,
    layout: ModalBottomSheetLayout = ModalBottomSheetLayout(),
    content: @Composable () -> Unit,
) {
  val currentCallbacks = rememberUpdatedState(callbacks)
  val currentVisible = rememberUpdatedState(visible)
  val detentHeights = rememberContentDetentHeights(layout)
  val expandedDetents = rememberContentDetents(layout, detentHeights.value)
  val expandedDetent = expandedDetents.first {
    it.identifier == layout.activeContentDetentIdentifier
  }
  val sheetState =
      rememberTallybookModalBottomSheetState(
          expandedDetents = expandedDetents,
          isVisible = { currentVisible.value },
          onDismissRequest = { currentCallbacks.value.onDismissRequest() },
      )

  LaunchedEffect(visible, expandedDetent) {
    sheetState.targetDetent = if (visible) expandedDetent else SheetDetent.Hidden
  }

  LaunchedEffect(visible, sheetState.currentDetent, sheetState.isIdle) {
    if (!visible && sheetState.currentDetent == SheetDetent.Hidden && sheetState.isIdle) {
      currentCallbacks.value.onHidden()
    } else if (visible && sheetState.currentDetent == expandedDetent && sheetState.isIdle) {
      currentCallbacks.value.onShown()
    }
  }

  UnstyledModalBottomSheet(
      state = sheetState,
      properties = modalBottomSheetProperties(),
      onDismiss = {},
      overlay = {
        Scrim(
            modifier =
                Modifier.fillMaxSize().pointerInput(Unit) {
                  detectTapGestures { currentCallbacks.value.onDismissRequest() }
                },
            scrimColor = TallybookTheme.colors.interaction.scrim,
        )
      },
  ) {
    Sheet(
        modifier =
            layout.modifier
                .widthIn(max = TallybookTheme.dimensions.modalSheetMaximumWidth)
                .fillMaxWidth()
                .then(if (layout.sheetFillsAvailableHeight) Modifier.fillMaxHeight() else Modifier)
                .background(
                    color = TallybookTheme.colors.surface.card,
                    shape = RoundedCornerShape(TallybookTheme.radii.radius30),
                )
                .captureContentDetentHeight(layout, detentHeights)
                .semantics { this.paneTitle = paneTitle },
    ) {
      TallybookModalSheetBody(layout = layout, content = content)
    }
  }
}

@Composable
private fun rememberContentDetents(
    layout: ModalBottomSheetLayout,
    detentHeights: Map<String, Dp>,
): List<SheetDetent> {
  val currentDetentHeights = rememberUpdatedState(detentHeights)
  return remember(layout.sheetFillsAvailableHeight, layout.contentDetentIdentifiers) {
    layout.contentDetentIdentifiers.map { identifier ->
      SheetDetent(identifier) { containerHeight, sheetHeight ->
        if (layout.sheetFillsAvailableHeight) {
          containerHeight
        } else {
          currentDetentHeights.value[identifier] ?: sheetHeight
        }
      }
    }
  }
}

@Composable
private fun rememberTallybookModalBottomSheetState(
    expandedDetents: List<SheetDetent>,
    isVisible: () -> Boolean,
    onDismissRequest: () -> Boolean,
) =
    rememberModalBottomSheetState(
        initialDetent = SheetDetent.Hidden,
        detents = listOf(SheetDetent.Hidden) + expandedDetents,
        animationSpec = sheetTransitionAnimationSpec(isOpening = true),
        dismissAnimationSpec = sheetTransitionAnimationSpec(isOpening = false),
        confirmDetentChange = { detent ->
          detent != SheetDetent.Hidden || !isVisible() || onDismissRequest()
        },
    )

private fun sheetTransitionAnimationSpec(isOpening: Boolean) =
    tween<Float>(
        durationMillis =
            if (isOpening) TallybookTheme.motion.sheetUpMillis
            else TallybookTheme.motion.sheetDownMillis
    )

private fun modalBottomSheetProperties() =
    ModalBottomSheetProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        offsetForIme = false,
    )

@Composable
private fun ModalBottomSheetScope.TallybookModalSheetBody(
    layout: ModalBottomSheetLayout,
    content: @Composable () -> Unit,
) {
  Column(
      modifier =
          (if (layout.fillAvailableHeight) Modifier.fillMaxHeight() else Modifier)
              .navigationBarsPadding()
              .padding(bottom = TallybookTheme.dimensions.modalSheetBottomPadding)
  ) {
    TallybookModalSheetDragIndication()
    if (layout.fillAvailableHeight) {
      Box(modifier = Modifier.weight(1f)) { content() }
    } else {
      content()
    }
  }
}

@Composable
private fun ModalBottomSheetScope.TallybookModalSheetDragIndication() {
  Box(
      modifier = Modifier.fillMaxWidth().height(TallybookTheme.dimensions.modalSheetDragAreaHeight),
      contentAlignment = Alignment.Center,
  ) {
    DragIndication(modifier = Modifier.fillMaxSize())
    Box(
        modifier =
            Modifier.width(TallybookTheme.dimensions.modalSheetDragHandleWidth)
                .height(TallybookTheme.dimensions.modalSheetDragHandleHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(TallybookTheme.colors.foreground.faint)
    )
  }
}

internal data class ModalBottomSheetCallbacks(
    val onDismiss: () -> Unit,
    val onDismissRequest: () -> Boolean = {
      onDismiss()
      true
    },
    val onHidden: () -> Unit = {},
    val onShown: () -> Unit = {},
)

internal data class ModalBottomSheetLayout(
    val modifier: Modifier = Modifier,
    val fillAvailableHeight: Boolean = false,
    val contentDetentIdentifiers: List<String> = listOf("fully-expanded"),
    val activeContentDetentIdentifier: String = contentDetentIdentifiers.first(),
) {
  init {
    require(contentDetentIdentifiers.isNotEmpty())
    require(contentDetentIdentifiers.distinct().size == contentDetentIdentifiers.size)
    require(activeContentDetentIdentifier in contentDetentIdentifiers)
  }

  val sheetFillsAvailableHeight: Boolean
    get() = fillAvailableHeight

  val hasContentDetentTransitions: Boolean
    get() = contentDetentIdentifiers.size > 1
}

@Composable
private fun rememberContentDetentHeights(
    layout: ModalBottomSheetLayout,
): MutableState<Map<String, Dp>> {
  return remember(layout.contentDetentIdentifiers) { mutableStateOf(emptyMap<String, Dp>()) }
}

@Composable
private fun Modifier.captureContentDetentHeight(
    layout: ModalBottomSheetLayout,
    detentHeights: MutableState<Map<String, Dp>>,
): Modifier {
  if (!layout.hasContentDetentTransitions) return this

  val density = LocalDensity.current
  return onSizeChanged { size ->
    val height = with(density) { size.height.toDp() }
    val identifier = layout.activeContentDetentIdentifier
    if (detentHeights.value[identifier] != height) {
      detentHeights.value = detentHeights.value + (identifier to height)
    }
  }
}
