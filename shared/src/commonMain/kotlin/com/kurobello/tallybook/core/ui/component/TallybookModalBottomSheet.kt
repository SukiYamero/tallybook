package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
) {
  val sheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.Hidden)

  LaunchedEffect(visible) {
    sheetState.targetDetent = if (visible) SheetDetent.FullyExpanded else SheetDetent.Hidden
  }

  UnstyledModalBottomSheet(
      state = sheetState,
      properties = ModalBottomSheetProperties(offsetForIme = true),
      onDismiss = onDismiss,
      overlay = { Scrim(scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)) },
  ) {
    Sheet(
        modifier =
            modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                .semantics { this.paneTitle = paneTitle },
    ) {
      Column {
        DragIndication(
            modifier =
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp, vertical = 20.dp)
        )
        content()
      }
    }
  }
}
