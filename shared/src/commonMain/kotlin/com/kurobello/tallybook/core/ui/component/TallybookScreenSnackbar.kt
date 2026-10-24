package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_notification_close

/**
 * Places an action-less [TallybookSnackbarHost] at the bottom of a secondary screen without bottom
 * navigation, inside the safe drawing area and the screen's content width.
 */
@Composable
internal fun TallybookScreenSnackbar(
    message: TallybookSnackbarMessage?,
    isActive: Boolean,
    onDismiss: (Long) -> Unit,
) {
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
  Box(
      modifier =
          Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .padding(
                  start = TallybookTheme.dimensions.screenHorizontal,
                  end = TallybookTheme.dimensions.screenHorizontal,
                  bottom = TallybookTheme.spacing.space16,
              )
  ) {
    TallybookSnackbarHost(
        message = message,
        isActive = isActive && lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
        closeLabel = stringResource(Res.string.movimiento_notification_close),
        onAction = { _, _ -> },
        onDismiss = { token, _, _ ->
          onDismiss(token)
          true
        },
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                .fillMaxWidth(),
    )
  }
}
