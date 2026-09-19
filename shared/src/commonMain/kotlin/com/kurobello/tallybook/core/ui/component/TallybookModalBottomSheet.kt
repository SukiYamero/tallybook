package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.DragIndication
import com.composeunstyled.ModalBottomSheetProperties
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState

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
  val expandedDetent =
      remember(layout.sheetFillsAvailableHeight) {
        if (layout.sheetFillsAvailableHeight) {
          SheetDetent("available-height") { containerHeight, _ -> containerHeight }
        } else {
          SheetDetent.FullyExpanded
        }
      }
  val sheetState =
      rememberModalBottomSheetState(
          initialDetent = SheetDetent.Hidden,
          detents = listOf(SheetDetent.Hidden, expandedDetent),
      )
  val currentCallbacks by rememberUpdatedState(callbacks)

  LaunchedEffect(visible) {
    sheetState.targetDetent = if (visible) expandedDetent else SheetDetent.Hidden
  }

  LaunchedEffect(visible, sheetState.currentDetent, sheetState.isIdle) {
    if (!visible && sheetState.currentDetent == SheetDetent.Hidden && sheetState.isIdle) {
      currentCallbacks.onHidden()
    }
  }

  UnstyledModalBottomSheet(
      state = sheetState,
      properties = ModalBottomSheetProperties(offsetForIme = false),
      onDismiss = currentCallbacks.onDismiss,
      overlay = { Scrim(scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)) },
  ) {
    Sheet(
        modifier =
            layout.modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .then(if (layout.sheetFillsAvailableHeight) Modifier.fillMaxHeight() else Modifier)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                .semantics { this.paneTitle = paneTitle },
    ) {
      Column(modifier = if (layout.fillAvailableHeight) Modifier.fillMaxHeight() else Modifier) {
        DragIndication(
            modifier =
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp, vertical = 20.dp)
        )
        if (layout.fillAvailableHeight) {
          Box(modifier = Modifier.weight(1f)) { content() }
        } else {
          content()
        }
      }
    }
  }
}

internal data class ModalBottomSheetCallbacks(
    val onDismiss: () -> Unit,
    val onHidden: () -> Unit = {},
)

internal data class ModalBottomSheetLayout(
    val modifier: Modifier = Modifier,
    val fillAvailableHeight: Boolean = false,
) {
  val sheetFillsAvailableHeight: Boolean
    get() = fillAvailableHeight
}
