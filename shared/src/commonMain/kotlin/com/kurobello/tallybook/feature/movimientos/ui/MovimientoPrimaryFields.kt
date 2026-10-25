package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.message
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_future_date

/** Opens the pickers; a null [onDeselectCategoria] keeps the category fixed. */
internal data class MovimientoSelectionCallbacks(
    val onChooseCategoria: () -> Unit,
    val onDeselectCategoria: (() -> Unit)?,
    val onAddEtiqueta: () -> Unit,
    val onChooseCuenta: () -> Unit,
    val onCreateCuenta: (() -> Unit)? = null,
)

internal data class MovimientoFieldFocus(
    val amount: FocusRequester = FocusRequester(),
    val amountModifier: Modifier = Modifier,
)

@Suppress("LongMethod")
@Composable
internal fun MovimientoPrimaryFields(
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    selection: MovimientoSelectionCallbacks,
    fieldFocus: MovimientoFieldFocus,
    interactionEnabled: Boolean = true,
) {
  val focusManager = LocalFocusManager.current
  Column {
    MovimientoTypeSelector(
        selected = uiState.tipo,
        enabled = interactionEnabled && !uiState.isSubmitting,
        onSelected = actions::onTipoChanged,
        modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    MovimientoCuentaField(
        field = uiState.cuentaField,
        callbacks =
            actions.cuentaCallbacks(
                onOpenPicker = selection.onChooseCuenta,
                onCreateCuenta = selection.onCreateCuenta,
            ),
        enabled = interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    MovimientoCategoryField(
        uiState = uiState,
        onChoose = selection.onChooseCategoria,
        onSuggestion = { actions.onCategoriaSelected(it.id) },
        onDeselect = selection.onDeselectCategoria,
        enabled = interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space16))
    MovimientoEtiquetasField(
        etiquetas = uiState.selectedEtiquetas,
        load = uiState.etiquetasLoad,
        callbacks = actions.etiquetasCallbacks(onAdd = selection.onAddEtiqueta),
        enabled = interactionEnabled && !uiState.isSubmitting,
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    MovimientoDateField(
        state =
            uiState.maxSelectableDate?.let {
              MovimientoDateFieldState(
                  uiState.fecha,
                  interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
                  it,
              )
            }
                ?: MovimientoDateFieldState(
                    fecha = uiState.fecha,
                    enabled = interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
                ),
        onFechaChanged = actions::onFechaChanged,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    if (uiState.isFechaInvalid)
        TallybookText(
            stringResource(Res.string.movimiento_future_date),
            color = TallybookTheme.colors.status.danger.base,
        )
    MovimientoAmountField(
        state =
            MovimientoAmountFieldState(
                value = uiState.amountInput,
                moneda = uiState.moneda,
                error = uiState.amountError?.message(),
                prompt = movimientoAmountPrompt(uiState.tipo),
            ),
        enabled = interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
        callbacks =
            MovimientoAmountFieldCallbacks(
                onValueChange = actions::onAmountChanged,
                focusRequester = fieldFocus.amount,
                inputModifier = fieldFocus.amountModifier,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            ),
        modifier = Modifier.padding(top = TallybookTheme.spacing.space40),
    )
  }
}
