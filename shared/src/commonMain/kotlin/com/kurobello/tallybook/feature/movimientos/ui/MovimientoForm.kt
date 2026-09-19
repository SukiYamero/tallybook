package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.captureScrollableInputViewport
import com.kurobello.tallybook.core.ui.component.centerFocusedInputIn
import com.kurobello.tallybook.core.ui.component.clearFocusOnUnconsumedTap
import com.kurobello.tallybook.core.ui.component.inputRevealTrailingSpace
import com.kurobello.tallybook.core.ui.component.needsManualInputReveal
import com.kurobello.tallybook.core.ui.component.rememberScrollableInputViewport
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AmountInputError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.collapsed
import tallybook.shared.generated.resources.description_optional
import tallybook.shared.generated.resources.expanded
import tallybook.shared.generated.resources.more_fields
import tallybook.shared.generated.resources.movimiento_categories_error
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.movimiento_save_error
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.save_expense
import tallybook.shared.generated.resources.save_income

@Suppress("LongMethod")
@Composable
internal fun MovimientoForm(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    modifier: Modifier = Modifier,
) {
  var categoryPickerVisible by rememberSaveable { mutableStateOf(false) }
  var datePickerVisible by rememberSaveable { mutableStateOf(false) }
  var descriptionVisible by rememberSaveable { mutableStateOf(false) }
  val amountFocusRequester = remember { FocusRequester() }
  val categoryFocusRequester = remember { FocusRequester() }
  val amountBringIntoViewRequester = remember { BringIntoViewRequester() }
  val categoryBringIntoViewRequester = remember { BringIntoViewRequester() }
  val focusManager = LocalFocusManager.current
  val formScrollState = rememberScrollState()
  val formViewport = rememberScrollableInputViewport()
  val inputRevealTrailingSpace = inputRevealTrailingSpace()
  val amountError = uiState.amountError?.message()
  val submitLabel = uiState.tipo.saveLabel()
  val descriptionState =
      stringResource(if (descriptionVisible) Res.string.expanded else Res.string.collapsed)

  LaunchedEffect(uiState.submitAttempted, uiState.amountError, uiState.isCategoriaMissing) {
    when {
      uiState.amountError != null -> {
        amountFocusRequester.requestFocus()
        amountBringIntoViewRequester.bringIntoView()
      }
      uiState.isCategoriaMissing -> {
        categoryFocusRequester.requestFocus()
        categoryBringIntoViewRequester.bringIntoView()
      }
    }
  }

  Box(
      modifier =
          modifier.fillMaxWidth().fillMaxHeight().captureScrollableInputViewport(formViewport),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight()
                .then(if (needsManualInputReveal) Modifier.imePadding() else Modifier)
                .verticalScroll(formScrollState)
                .clearFocusOnUnconsumedTap(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      MovimientoTypeSelector(
          selected = uiState.tipo,
          enabled = !uiState.isSubmitting,
          onSelected = actions::onTipoChanged,
      )
      MovimientoAmountField(
          state =
              MovimientoAmountFieldState(
                  value = uiState.amountInput,
                  moneda = uiState.moneda,
                  error = amountError,
              ),
          enabled = !uiState.isSubmitting,
          onValueChange = actions::onAmountChanged,
          focusRequester = amountFocusRequester,
          modifier =
              Modifier.bringIntoViewRequester(amountBringIntoViewRequester)
                  .centerFocusedInputIn(formScrollState, formViewport),
      )
      MovimientoDateField(
          state =
              MovimientoDateFieldState(
                  fecha = uiState.fecha,
                  showPicker = datePickerVisible,
                  enabled = !uiState.isSubmitting,
              ),
          onOpenPicker = { datePickerVisible = true },
          onDismissPicker = { datePickerVisible = false },
          onFechaChanged = actions::onFechaChanged,
      )
      Column(
          modifier = Modifier.bringIntoViewRequester(categoryBringIntoViewRequester),
          verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        CategoryField(
            selected = uiState.categorias.firstOrNull { it.id == uiState.selectedCategoriaId },
            isError = uiState.isCategoriaMissing,
            onOpen = {
              focusManager.clearFocus(force = true)
              categoryPickerVisible = true
            },
            modifier = Modifier.focusRequester(categoryFocusRequester),
        )
        if (uiState.isCategoriaMissing) {
          Text(
              text = stringResource(Res.string.category_required),
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
              style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      TextButton(
          onClick = { descriptionVisible = !descriptionVisible },
          modifier =
              Modifier.align(Alignment.CenterHorizontally).semantics {
                stateDescription = descriptionState
              },
          enabled = !uiState.isSubmitting,
      ) {
        Text(stringResource(Res.string.more_fields))
      }
      if (descriptionVisible) {
        OutlinedTextField(
            value = uiState.descripcion,
            onValueChange = {
              actions.onDescripcionChanged(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH))
            },
            modifier =
                Modifier.fillMaxWidth()
                    .centerFocusedInputIn(formScrollState, formViewport)
                    .testTag("movimiento_description"),
            enabled = !uiState.isSubmitting,
            label = { Text(stringResource(Res.string.description_optional)) },
            supportingText = {
              Text(
                  text = "${uiState.descripcion.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH",
                  modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
              )
            },
        )
      }
      MovimientoFormErrorMessage(error = uiState.error, onRetry = actions::onRetryLoad)
      if (uiState.isLoading) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
      }
      Button(
          onClick = actions::onSubmit,
          modifier =
              Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 48.dp).semantics {
                contentDescription = submitLabel
              },
          enabled = !uiState.isSubmitting && !uiState.isLoading && uiState.error == null,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(text = submitLabel, modifier = Modifier.alpha(if (uiState.isSubmitting) 0f else 1f))
          if (uiState.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          }
        }
      }
      Spacer(modifier = Modifier.height(inputRevealTrailingSpace))
    }
  }
  CategoryPickerSheet(
      visible = categoryPickerVisible,
      categorias = uiState.categorias,
      selectedId = uiState.selectedCategoriaId,
      onSelect = actions::onCategoriaSelected,
      onDismiss = { categoryPickerVisible = false },
  )
}

@Composable
private fun AmountInputError.message(): String =
    when (this) {
      AmountInputError.EMPTY -> stringResource(Res.string.amount_required)
      AmountInputError.MALFORMED -> stringResource(Res.string.amount_malformed)
      AmountInputError.NOT_POSITIVE -> stringResource(Res.string.amount_not_positive)
      AmountInputError.TOO_MANY_DECIMALS -> stringResource(Res.string.amount_too_many_decimals)
      AmountInputError.OVERFLOW -> stringResource(Res.string.amount_overflow)
    }

@Composable
private fun MovimientoFormErrorMessage(error: MovimientoFormError?, onRetry: () -> Unit) {
  when (error) {
    null -> Unit
    MovimientoFormError.PREFERENCES_UNAVAILABLE ->
        MovimientoFormRetryError(stringResource(Res.string.movimiento_preferences_error), onRetry)
    MovimientoFormError.CATEGORIES_UNAVAILABLE ->
        MovimientoFormRetryError(stringResource(Res.string.movimiento_categories_error), onRetry)
    MovimientoFormError.SAVE_FAILED ->
        Text(
            text = stringResource(Res.string.movimiento_save_error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
        )
  }
}

@Composable
private fun MovimientoFormRetryError(message: String, onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
    )
    TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun TipoMovimiento.saveLabel(): String =
    when (this) {
      TipoMovimiento.GASTO -> stringResource(Res.string.save_expense)
      TipoMovimiento.INGRESO -> stringResource(Res.string.save_income)
    }
