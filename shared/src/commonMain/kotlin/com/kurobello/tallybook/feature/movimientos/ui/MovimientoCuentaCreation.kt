package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetFormLayout
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.ui.AccountFieldErrors
import com.kurobello.tallybook.feature.accounts.ui.AccountForm
import com.kurobello.tallybook.feature.accounts.ui.AccountFormMode
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaActions
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaEffect
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaUiState
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_create
import tallybook.shared.generated.resources.account_save_error
import tallybook.shared.generated.resources.account_use_existing
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.entry_cuenta_new_title

internal const val QUICK_CUENTA_ADD_KEY = "quick-cuenta-add"
internal const val QUICK_CUENTA_MANAGE_KEY = "quick-cuenta-manage"
internal const val CUENTA_CREATION_TAG = "movimiento_cuenta_creation"

/**
 * The in-place account creation a movimiento surface offers when no cuenta fits its medio. Its
 * owner keeps the draft and the write; [moneda] is the currency the new cuenta starts in.
 */
internal data class CuentaCreation(
    val state: QuickCuentaUiState,
    val actions: QuickCuentaActions,
    val effects: Flow<QuickCuentaEffect>,
    val moneda: Moneda,
)

/** The type a new cuenta needs to be usable with [medio]; without one, the cash type. */
internal fun quickCuentaTipo(medio: MedioPago?): TipoCuenta =
    medio?.compatibleTipos?.first() ?: TipoCuenta.EFECTIVO

internal fun CuentaCreation.start(field: MovimientoCuentaUi) {
  actions.onStart(quickCuentaTipo(field.medio), moneda)
}

@Composable
internal fun rememberQuickCuentaViewModel(key: String): QuickCuentaViewModel =
    rememberHostScopedViewModel(key)

/**
 * The creation sheet over a movimiento surface. A created or reused cuenta reaches [field]'s
 * options only after the repository emits it, and selecting an id the draft does not know yet would
 * be dropped, so the selection waits for it.
 */
@Composable
internal fun CuentaCreationSheet(
    creation: CuentaCreation?,
    field: MovimientoCuentaUi,
    onSelect: (String) -> Unit,
) {
  if (creation == null) return
  CuentaCreationSelectionEffect(creation.effects, field, onSelect)
  val draft = creation.state.draft
  val title = stringResource(Res.string.entry_cuenta_new_title)
  val focusManager = LocalFocusManager.current
  val close = {
    focusManager.clearFocus(force = true)
    creation.actions.onClose()
  }
  TallybookModalBottomSheet(
      visible = draft != null,
      paneTitle = title,
      callbacks = ModalBottomSheetCallbacks(onDismiss = close, onBack = close),
  ) {
    if (draft != null) CuentaCreationPane(creation, title, close)
  }
}

@Composable
private fun CuentaCreationPane(creation: CuentaCreation, title: String, onBack: () -> Unit) {
  val state = creation.state
  val draft = state.draft ?: return
  TallybookSheetForm(
      header = { CuentaCreationHeader(title, onBack) },
      footer = {
        TallybookButton(
            onClick = creation.actions::onSave,
            modifier = Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space8),
            enabled = state.canSave,
        ) {
          TallybookText(stringResource(Res.string.account_create))
        }
      },
      modifier = Modifier.testTag(CUENTA_CREATION_TAG),
      layout =
          TallybookSheetFormLayout(
              horizontalPadding = TallybookTheme.spacing.space22,
              minimumBodyHeight = TallybookTheme.dimensions.editorSheetFormMinimumHeight,
              maximumBodyHeight = TallybookTheme.dimensions.pickerResultsHeight,
          ),
  ) {
    AccountForm(
        state = draft,
        onDraftChanged = creation.actions::onDraftChanged,
        mode = AccountFormMode.QUICK,
        enabled = !state.saving,
        errors = AccountFieldErrors(duplicate = state.duplicate),
        duplicateAction =
            state.duplicate
                ?.takeIf { state.canUseDuplicate }
                ?.let { duplicate ->
                  {
                    TallybookTextButton(onClick = creation.actions::onUseDuplicate) {
                      TallybookText(
                          stringResource(Res.string.account_use_existing, duplicate.nombre)
                      )
                    }
                  }
                },
        revealFocusedInput = { revealWhenFocused() },
    )
    if (state.saveFailed) {
      TallybookText(
          text = stringResource(Res.string.account_save_error),
          color = TallybookTheme.colors.status.danger.base,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          style = TallybookTheme.typography.body,
      )
    }
  }
}

@Composable
private fun CuentaCreationHeader(title: String, onBack: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookIconButton(
        onClick = onBack,
        icon = Lucide.ArrowLeft,
        contentDescription = stringResource(Res.string.back),
    )
    TallybookSheetTitle(text = title, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun CuentaCreationSelectionEffect(
    effects: Flow<QuickCuentaEffect>,
    field: MovimientoCuentaUi,
    onSelect: (String) -> Unit,
) {
  var pendingId by remember { mutableStateOf<String?>(null) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(effects, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      effects.collect { effect -> pendingId = effect.cuenta.id }
    }
  }
  val currentOnSelect by rememberUpdatedState(onSelect)
  LaunchedEffect(pendingId, field.options) {
    val id = pendingId ?: return@LaunchedEffect
    if (field.options.none { it.id == id }) return@LaunchedEffect
    pendingId = null
    currentOnSelect(id)
  }
}
