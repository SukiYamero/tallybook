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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_required

internal data class MovimientoFieldFocus(
    val amount: FocusRequester = FocusRequester(),
    val category: FocusRequester = FocusRequester(),
)

@Suppress("LongMethod")
@Composable
internal fun MovimientoPrimaryFields(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    categoryEntry: MovimientoCategoryEntry,
    fieldFocus: MovimientoFieldFocus,
    amountModifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  val selectedCategoria = uiState.categorias.firstOrNull { it.id == uiState.selectedCategoriaId }
  val categoryMessage =
      stringResource(Res.string.category_required)
          .takeIf { uiState.isCategoriaMissing }
          ?.let(TallybookFieldMessage::Error)
  Column {
    MovimientoTypeSelector(
        selected = uiState.tipo,
        enabled = !uiState.isSubmitting,
        onSelected = actions::onTipoChanged,
        modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    TallybookSelectionField(
        label = stringResource(Res.string.category),
        value = selectedCategoria?.nombre,
        placeholder = stringResource(Res.string.category_required),
        onClick = categoryEntry.onOpen,
        modifier = Modifier.focusRequester(fieldFocus.category),
        enabled = !uiState.isSubmitting && !uiState.isLoading,
        message = categoryMessage,
        leadingContent = selectedCategoria?.let { categoria -> { CategoryAvatar(categoria) } },
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    MovimientoDateField(
        state = MovimientoDateFieldState(fecha = uiState.fecha, enabled = !uiState.isSubmitting),
        onFechaChanged = actions::onFechaChanged,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    MovimientoAmountField(
        state =
            MovimientoAmountFieldState(
                value = uiState.amountInput,
                moneda = uiState.moneda,
                error = uiState.amountError?.message(),
                prompt = movimientoAmountPrompt(uiState.tipo),
            ),
        enabled = !uiState.isSubmitting && !uiState.isLoading && selectedCategoria != null,
        callbacks =
            MovimientoAmountFieldCallbacks(
                onValueChange = actions::onAmountChanged,
                focusRequester = fieldFocus.amount,
                inputModifier = amountModifier,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            ),
        modifier = Modifier.padding(top = TallybookTheme.spacing.space40),
    )
  }
}
