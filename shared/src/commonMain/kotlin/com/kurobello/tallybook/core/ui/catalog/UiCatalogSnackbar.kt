package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarHost
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_snackbar_action
import tallybook.shared.generated.resources.ui_catalog_snackbar_actionable
import tallybook.shared.generated.resources.ui_catalog_snackbar_busy
import tallybook.shared.generated.resources.ui_catalog_snackbar_busy_message
import tallybook.shared.generated.resources.ui_catalog_snackbar_close
import tallybook.shared.generated.resources.ui_catalog_snackbar_message
import tallybook.shared.generated.resources.ui_catalog_snackbar_persistent
import tallybook.shared.generated.resources.ui_catalog_snackbar_plain
import tallybook.shared.generated.resources.ui_catalog_snackbar_reviewed

@Composable
internal fun UiCatalogSnackbar() {
  var token by remember { mutableLongStateOf(0) }
  var messages by remember { mutableStateOf<List<TallybookSnackbarMessage>>(emptyList()) }
  val sample = stringResource(Res.string.ui_catalog_snackbar_message)
  val busy = stringResource(Res.string.ui_catalog_snackbar_busy_message)
  val action = stringResource(Res.string.ui_catalog_snackbar_action)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    SnackbarSampleControls(
        onPlain = {
          messages +=
              TallybookSnackbarMessage(
                  ++token,
                  0,
                  sample,
                  TallybookSnackbarDuration.Short,
                  status = TallybookSnackbarStatus.Success,
              )
        },
        onActionable = {
          messages +=
              TallybookSnackbarMessage(++token, 0, sample, TallybookSnackbarDuration.Long, action)
        },
        onBusy = {
          messages +=
              TallybookSnackbarMessage(
                  ++token,
                  0,
                  busy,
                  TallybookSnackbarDuration.Indefinite,
                  action,
                  isBusy = true,
              )
        },
        onPersistent = {
          messages +=
              TallybookSnackbarMessage(
                  ++token,
                  0,
                  sample,
                  TallybookSnackbarDuration.Indefinite,
                  action,
                  status = TallybookSnackbarStatus.Warning,
              )
        },
    )
    SnackbarSampleHost(
        message = messages.firstOrNull(),
        hasWaitingMessages = messages.size > 1,
        onMessageChange = { expected, replacement ->
          val current = messages.firstOrNull()
          if (current?.token == expected.token && current.revision == expected.revision) {
            messages = listOfNotNull(replacement) + messages.drop(1)
          }
        },
    )
  }
}

@Composable
private fun SnackbarSampleHost(
    message: TallybookSnackbarMessage?,
    onMessageChange: (TallybookSnackbarMessage, TallybookSnackbarMessage?) -> Unit,
    hasWaitingMessages: Boolean,
) {
  val reviewed = stringResource(Res.string.ui_catalog_snackbar_reviewed)
  SnackbarSampleBusyCompletion(message, onMessageChange)
  TallybookSnackbarHost(
      message = message,
      isActive = true,
      closeLabel = stringResource(Res.string.ui_catalog_snackbar_close),
      hasWaitingMessages = hasWaitingMessages,
      onAction = { actionToken, revision ->
        message
            ?.takeIf { it.token == actionToken && it.revision == revision }
            ?.let {
              onMessageChange(
                  it,
                  it.copy(
                      revision = it.revision + 1,
                      message = reviewed,
                      duration = TallybookSnackbarDuration.Short,
                      actionLabel = null,
                      status = TallybookSnackbarStatus.Success,
                  ),
              )
            }
      },
      onDismiss = { dismissToken, revision, _ ->
        if (message?.token == dismissToken && message.revision == revision) {
          onMessageChange(message, null)
        }
      },
      modifier = Modifier.fillMaxWidth(),
  )
}

private const val SAMPLE_PREPARATION_MILLIS = 1_500L

@Composable
private fun SnackbarSampleBusyCompletion(
    message: TallybookSnackbarMessage?,
    onMessageChange: (TallybookSnackbarMessage, TallybookSnackbarMessage?) -> Unit,
) {
  val ready = stringResource(Res.string.ui_catalog_snackbar_message)
  val latestOnMessageChange by rememberUpdatedState(onMessageChange)
  LaunchedEffect(message?.token, message?.revision, message?.isBusy) {
    val current = message?.takeIf { it.isBusy } ?: return@LaunchedEffect
    delay(SAMPLE_PREPARATION_MILLIS)
    latestOnMessageChange(
        current,
        current.copy(
            revision = current.revision + 1,
            message = ready,
            duration = TallybookSnackbarDuration.Short,
            actionLabel = null,
            isBusy = false,
            status = TallybookSnackbarStatus.Success,
        ),
    )
  }
}

@Composable
private fun SnackbarSampleControls(
    onPlain: () -> Unit,
    onActionable: () -> Unit,
    onBusy: () -> Unit,
    onPersistent: () -> Unit,
) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookTextButton(onPlain) {
      TallybookText(stringResource(Res.string.ui_catalog_snackbar_plain))
    }
    TallybookTextButton(onActionable) {
      TallybookText(stringResource(Res.string.ui_catalog_snackbar_actionable))
    }
    TallybookTextButton(onBusy) {
      TallybookText(stringResource(Res.string.ui_catalog_snackbar_busy))
    }
    TallybookTextButton(onPersistent) {
      TallybookText(stringResource(Res.string.ui_catalog_snackbar_persistent))
    }
  }
}
