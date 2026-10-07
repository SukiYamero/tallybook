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
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.movimiento_future_date
import tallybook.shared.generated.resources.unknown_category

internal data class MovimientoFieldFocus(
    val amount: FocusRequester = FocusRequester(),
    val category: FocusRequester = FocusRequester(),
    val amountModifier: Modifier = Modifier,
)

@Suppress("LongMethod")
@Composable
internal fun MovimientoPrimaryFields(
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    categoryEntry: MovimientoCategoryEntry,
    fieldFocus: MovimientoFieldFocus,
    interactionEnabled: Boolean = true,
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
        enabled = interactionEnabled && !uiState.isSubmitting,
        onSelected = actions::onTipoChanged,
        modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    TallybookSelectionField(
        label = stringResource(Res.string.category),
        value =
            selectedCategoria?.nombre
                ?: stringResource(Res.string.unknown_category).takeIf {
                  uiState.selectedCategoriaId != null
                },
        placeholder = stringResource(Res.string.category_required),
        onClick = categoryEntry.onOpen,
        modifier = Modifier.focusRequester(fieldFocus.category),
        enabled = interactionEnabled && !uiState.isSubmitting && !uiState.isLoading,
        message = categoryMessage,
        leadingContent = selectedCategoria?.let { categoria -> { CategoryAvatar(categoria) } },
        labelContent = {
          TallybookInputLabel(
              text = stringResource(Res.string.category),
              color =
                  if (categoryMessage is TallybookFieldMessage.Error)
                      TallybookTheme.colors.status.danger.base
                  else TallybookTheme.colors.foreground.tertiary,
          )
        },
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space24))
    MovimientoDateField(
        state =
            uiState.maxSelectableDate?.let {
              MovimientoDateFieldState(
                  uiState.fecha,
                  interactionEnabled && !uiState.isSubmitting,
                  it,
              )
            }
                ?: MovimientoDateFieldState(
                    fecha = uiState.fecha,
                    enabled = interactionEnabled && !uiState.isSubmitting,
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
