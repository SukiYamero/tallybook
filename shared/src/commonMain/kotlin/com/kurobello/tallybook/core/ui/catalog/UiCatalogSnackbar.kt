package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarHost
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_snackbar_action
import tallybook.shared.generated.resources.ui_catalog_snackbar_actionable
import tallybook.shared.generated.resources.ui_catalog_snackbar_busy
import tallybook.shared.generated.resources.ui_catalog_snackbar_busy_message
import tallybook.shared.generated.resources.ui_catalog_snackbar_close
import tallybook.shared.generated.resources.ui_catalog_snackbar_message
import tallybook.shared.generated.resources.ui_catalog_snackbar_plain
import tallybook.shared.generated.resources.ui_catalog_snackbar_reviewed

@Composable
internal fun UiCatalogSnackbar() {
  var token by remember { mutableLongStateOf(0) }
  var message by remember { mutableStateOf<TallybookSnackbarMessage?>(null) }
  val sample = stringResource(Res.string.ui_catalog_snackbar_message)
  val busy = stringResource(Res.string.ui_catalog_snackbar_busy_message)
  val reviewed = stringResource(Res.string.ui_catalog_snackbar_reviewed)
  val action = stringResource(Res.string.ui_catalog_snackbar_action)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    SnackbarSampleControls(
        onPlain = {
          token++
          message = TallybookSnackbarMessage(token, 0, sample, TallybookSnackbarDuration.Short)
        },
        onActionable = {
          token++
          message =
              TallybookSnackbarMessage(token, 0, sample, TallybookSnackbarDuration.Long, action)
        },
        onBusy = {
          token++
          message =
              TallybookSnackbarMessage(
                  token,
                  0,
                  busy,
                  TallybookSnackbarDuration.Long,
                  action,
                  isBusy = true,
              )
        },
    )
    TallybookSnackbarHost(
        message = message,
        isActive = true,
        closeLabel = stringResource(Res.string.ui_catalog_snackbar_close),
        onAction = { actionToken, revision ->
          message
              ?.takeIf { it.token == actionToken && it.revision == revision }
              ?.let {
                message =
                    it.copy(
                        revision = it.revision + 1,
                        message = reviewed,
                        duration = TallybookSnackbarDuration.Short,
                        actionLabel = null,
                    )
              }
        },
        onDismiss = { dismissToken, revision, _ ->
          if (message?.token == dismissToken && message?.revision == revision) message = null
        },
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun SnackbarSampleControls(
    onPlain: () -> Unit,
    onActionable: () -> Unit,
    onBusy: () -> Unit,
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
  }
}
