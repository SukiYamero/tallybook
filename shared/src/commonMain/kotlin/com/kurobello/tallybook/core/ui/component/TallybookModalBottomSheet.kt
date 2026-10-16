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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
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
  val detentHeights = remember(layout.contentDetentIdentifiers) { mutableMapOf<String, Dp>() }
  val expandedDetents = rememberContentDetents(layout, detentHeights)
  val expandedDetent = expandedDetents.first {
    it.identifier == layout.activeContentDetentIdentifier
  }
  val sheetState =
      rememberTallybookModalBottomSheetState(
          expandedDetents = expandedDetents,
          isVisible = { currentVisible.value },
          onDismissRequest = { currentCallbacks.value.onDismissRequest() },
      )

  val activeDetentIdentifier by rememberUpdatedState(layout.activeContentDetentIdentifier)
  val onContentHeightMeasured =
      remember(sheetState, detentHeights) {
        { height: Dp ->
          if (detentHeights[activeDetentIdentifier] != height) {
            detentHeights[activeDetentIdentifier] = height
            sheetState.invalidateDetents()
          }
        }
      }

  LaunchedEffect(visible, expandedDetent) {
    sheetState.targetDetent = if (visible) expandedDetent else SheetDetent.Hidden
  }

  ModalBottomSheetVisibilityEffect(visible, sheetState, expandedDetent, currentCallbacks)

  UnstyledModalBottomSheet(
      state = sheetState,
      properties = modalBottomSheetProperties(dismissOnBackPress = callbacks.onBack == null),
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
    // Android modal windows have a separate Back dispatcher from their Activity.
    ModalBottomSheetBackHandling(visible, currentCallbacks)
    Sheet(
        modifier = modalSheetModifier(layout, paneTitle),
    ) {
      TallybookModalSheetBody(
          layout = layout,
          onContentHeightMeasured = onContentHeightMeasured,
          content = content,
      )
    }
  }
}

@Composable
private fun modalSheetModifier(layout: ModalBottomSheetLayout, title: String): Modifier =
    layout.modifier
        .widthIn(max = TallybookTheme.dimensions.modalSheetMaximumWidth)
        .fillMaxWidth()
        .then(if (layout.sheetFillsAvailableHeight) Modifier.fillMaxHeight() else Modifier)
        .background(
            TallybookTheme.colors.surface.card,
            RoundedCornerShape(TallybookTheme.radii.radius30),
        )
        .semantics { paneTitle = title }

@Composable
private fun rememberContentDetents(
    layout: ModalBottomSheetLayout,
    detentHeights: Map<String, Dp>,
): List<SheetDetent> =
    remember(layout.sheetFillsAvailableHeight, layout.contentDetentIdentifiers, detentHeights) {
      layout.contentDetentIdentifiers.map { identifier ->
        SheetDetent(identifier) { containerHeight, sheetHeight ->
          if (layout.sheetFillsAvailableHeight) {
            containerHeight
          } else {
            detentHeights[identifier] ?: sheetHeight
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

private fun modalBottomSheetProperties(dismissOnBackPress: Boolean) =
    ModalBottomSheetProperties(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = false,
        offsetForIme = false,
    )

@Composable
private fun ModalBottomSheetScope.TallybookModalSheetBody(
    layout: ModalBottomSheetLayout,
    onContentHeightMeasured: (Dp) -> Unit,
    content: @Composable () -> Unit,
) {
  Column(
      modifier =
          (if (layout.fillAvailableHeight) Modifier.fillMaxHeight()
              else if (layout.hasContentDetentTransitions)
                  Modifier.measureAgainstWindowHeight(onContentHeightMeasured)
              else Modifier)
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
    val onBack: (() -> Unit)? = null,
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

// A detent's height is read back from the sheet, which is bounded by that same detent, so measuring
// it there can only confirm the previous height. Measuring against the window instead sees content
// that grows after its detent settled (the keyboard, an error message) and reports its natural
// height; the sheet then clips what the detent has not reached yet.
@Composable
private fun Modifier.measureAgainstWindowHeight(onMeasured: (Dp) -> Unit): Modifier {
  val windowHeight = LocalWindowInfo.current.containerSize.height
  val currentOnMeasured by rememberUpdatedState(onMeasured)
  return layout { measurable, constraints ->
    val placeable =
        measurable.measure(constraints.copy(maxHeight = maxOf(constraints.maxHeight, windowHeight)))
    currentOnMeasured(placeable.height.toDp())
    layout(
        placeable.width,
        placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight),
    ) {
      placeable.placeRelative(0, 0)
    }
  }
}
