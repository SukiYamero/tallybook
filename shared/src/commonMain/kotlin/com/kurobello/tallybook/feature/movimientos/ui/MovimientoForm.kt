package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AmountInputError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_categories_error
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.movimiento_save_error
import tallybook.shared.generated.resources.retry

internal data class MovimientoFormPresentation(
    val title: String,
    val submitLabel: String,
    val isSubmissionEnabled: Boolean = true,
)

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun MovimientoForm(
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    modifier: Modifier = Modifier,
    presentation: MovimientoFormPresentation? = null,
    headerAction: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    interactionEnabled: Boolean = true,
    categoryCreation: CategoryPickerCreation? = null,
) {
  val fieldFocus = remember { MovimientoFieldFocus() }
  val categoryPicker = rememberMovimientoCategoryPicker(interactionEnabled)
  NavigationBackHandler(
      state = rememberNavigationEventState(NavigationEventInfo.None),
      isBackEnabled = categoryPicker.isVisible && interactionEnabled,
      onBackCompleted = {
        if (categoryCreation?.state?.draft != null) categoryCreation.actions.onClose()
        else categoryPicker.onDismiss()
      },
  )
  val title = presentation?.title ?: stringResource(Res.string.add_movimiento_pane_title)
  MovimientoValidationFocus(uiState, fieldFocus)

  TallybookSheetForm(
      header = { MovimientoFormHeader(title, headerAction) },
      footer = {
        if (footerContent == null) {
          MovimientoFormSubmit(
              uiState,
              actions::onSubmit,
              presentation,
              interactionEnabled,
              modifier = Modifier.padding(bottom = TallybookTheme.spacing.space8),
          )
        } else footerContent()
      },
      modifier = modifier,
  ) {
    MovimientoPrimaryFields(
        uiState = uiState,
        interactionEnabled = interactionEnabled,
        actions = actions,
        onOpenCategory = categoryPicker.onOpen,
        fieldFocus = fieldFocus.copy(amountModifier = Modifier.revealWhenFocused()),
    )
    MovimientoDescriptionField(
        uiState,
        actions::onDescripcionChanged,
        Modifier.revealWhenFocused(),
        interactionEnabled && !uiState.isSubmitting,
    )
    supportingContent?.invoke()
    MovimientoFormErrorMessage(
        error = uiState.error,
        onRetry = actions::onRetryLoad,
        interactionEnabled = interactionEnabled,
    )
    if (uiState.isLoading) {
      TallybookProgressIndicator(
          contentDescription = stringResource(Res.string.loading),
          modifier = Modifier.align(Alignment.CenterHorizontally),
      )
    }
  }
  MovimientoCategoryPickerSheet(
      visible = interactionEnabled && categoryPicker.isVisible,
      uiState = uiState,
      onSelectionChange = actions::onCategoriasChanged,
      picker = categoryPicker,
      creation = categoryCreation,
  )
}

@Composable
private fun MovimientoValidationFocus(
    uiState: MovimientoFormState,
    fieldFocus: MovimientoFieldFocus,
) {
  LaunchedEffect(uiState.submitAttempted) {
    if (uiState.amountError != null) fieldFocus.amount.requestFocus()
  }
}

@Composable
internal fun AmountInputError.message(): String =
    when (this) {
      AmountInputError.EMPTY -> stringResource(Res.string.amount_required)
      AmountInputError.MALFORMED -> stringResource(Res.string.amount_malformed)
      AmountInputError.NOT_POSITIVE -> stringResource(Res.string.amount_not_positive)
      AmountInputError.TOO_MANY_DECIMALS -> stringResource(Res.string.amount_too_many_decimals)
      AmountInputError.OVERFLOW -> stringResource(Res.string.amount_overflow)
    }

@Composable
internal fun MovimientoFormErrorMessage(
    error: MovimientoFormError?,
    onRetry: () -> Unit,
    interactionEnabled: Boolean,
) {
  when (error) {
    null -> Unit
    MovimientoFormError.PREFERENCES_UNAVAILABLE ->
        MovimientoFormRetryError(
            stringResource(Res.string.movimiento_preferences_error),
            onRetry,
            interactionEnabled,
        )
    MovimientoFormError.CATEGORIES_UNAVAILABLE ->
        MovimientoFormRetryError(
            stringResource(Res.string.movimiento_categories_error),
            onRetry,
            interactionEnabled,
        )
    MovimientoFormError.SAVE_FAILED ->
        TallybookText(
            text = stringResource(Res.string.movimiento_save_error),
            color = TallybookTheme.colors.status.danger.base,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = TallybookTheme.typography.body,
        )
  }
}

@Composable
private fun MovimientoFormRetryError(
    message: String,
    onRetry: () -> Unit,
    interactionEnabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = message,
        color = TallybookTheme.colors.status.danger.base,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = TallybookTheme.typography.body,
    )
    TallybookTextButton(onClick = onRetry, enabled = interactionEnabled) {
      TallybookText(stringResource(Res.string.retry))
    }
  }
}
