package com.kurobello.tallybook.feature.transfers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TRANSFERENCIA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.TransferenciaCuentaError
import com.kurobello.tallybook.core.ui.component.TallybookAmountInput
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import com.kurobello.tallybook.core.ui.component.TallybookDateChipSize
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessageContent
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AmountInputError
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferDraftState
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferFormActions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.description_optional
import tallybook.shared.generated.resources.future_date_not_allowed
import tallybook.shared.generated.resources.transfer_cuenta_missing
import tallybook.shared.generated.resources.transfer_destino_label
import tallybook.shared.generated.resources.transfer_needs_two_accounts
import tallybook.shared.generated.resources.transfer_origen_label
import tallybook.shared.generated.resources.transfer_received_label
import tallybook.shared.generated.resources.transfer_same_cuenta
import tallybook.shared.generated.resources.transfer_sent_label
import tallybook.shared.generated.resources.transfer_with_moneda

/**
 * The fields of one transfer, stateless over [TransferDraftState]. The received amount appears only
 * while the two accounts' currencies differ, and both amount labels then carry the currency code,
 * since several currencies share the "$" symbol.
 */
@Composable
internal fun TransferForm(
    form: TransferDraftState,
    actions: TransferFormActions,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    revealFocusedInput: @Composable Modifier.() -> Modifier = { this },
) {
  val errors = form.visibleErrors
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
  ) {
    CuentaChoices(
        label = stringResource(Res.string.transfer_origen_label),
        options = form.origenOptions,
        selectedId = form.draft.origenId,
        error = errors.origen,
        form = form,
        enabled = enabled,
        onSelect = actions::onOrigenSelected,
    )
    CuentaChoices(
        label = stringResource(Res.string.transfer_destino_label),
        options = form.destinoOptions,
        selectedId = form.draft.destinoId,
        error = errors.destino,
        form = form,
        enabled = enabled,
        onSelect = actions::onDestinoSelected,
    )
    if (form.original == null && form.origenOptions.size < 2) {
      TallybookText(
          stringResource(Res.string.transfer_needs_two_accounts),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.body,
      )
    }
    TransferAmounts(form, enabled, actions, revealFocusedInput)
    TransferDateField(form, errors.fechaFuture, enabled, actions)
    TransferDescripcionField(
        form.draft.descripcion,
        enabled,
        actions::onDescripcionChanged,
        Modifier.revealFocusedInput(),
    )
  }
}

@Composable
private fun TransferAmounts(
    form: TransferDraftState,
    enabled: Boolean,
    actions: TransferFormActions,
    revealFocusedInput: @Composable Modifier.() -> Modifier,
) {
  val errors = form.visibleErrors
  TransferAmountField(
      label =
          if (form.isCrossCurrency)
              labelWithMoneda(Res.string.transfer_sent_label, form.montoOrigenMoneda)
          else stringResource(Res.string.amount),
      value = form.draft.montoOrigen,
      moneda = form.montoOrigenMoneda,
      error = errors.montoOrigen,
      enabled = enabled,
      onValueChange = actions::onMontoOrigenChanged,
      modifier = Modifier.revealFocusedInput(),
  )
  if (form.isCrossCurrency) {
    TransferAmountField(
        label = labelWithMoneda(Res.string.transfer_received_label, form.montoDestinoMoneda),
        value = form.draft.montoDestino,
        moneda = form.montoDestinoMoneda,
        error = errors.montoDestino,
        enabled = enabled,
        onValueChange = actions::onMontoDestinoChanged,
        modifier = Modifier.revealFocusedInput(),
    )
  }
}

@Suppress("LongParameterList")
@Composable
private fun CuentaChoices(
    label: String,
    options: List<Cuenta>,
    selectedId: String?,
    error: TransferenciaCuentaError?,
    form: TransferDraftState,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
  val message =
      when (error) {
        TransferenciaCuentaError.MISSING -> stringResource(Res.string.transfer_cuenta_missing)
        TransferenciaCuentaError.SAME_AS_ORIGEN -> stringResource(Res.string.transfer_same_cuenta)
        null -> null
      }
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing)) {
    TallybookInputLabel(
        label,
        color =
            if (message != null) TallybookTheme.colors.status.danger.base
            else TallybookTheme.colors.foreground.tertiary,
    )
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      options.forEach { cuenta ->
        TallybookChoicePill(
            label =
                if (form.showsMoneda(cuenta))
                    stringResource(
                        Res.string.transfer_with_moneda,
                        cuenta.nombre,
                        cuenta.moneda.name,
                    )
                else cuenta.nombre,
            selected = cuenta.id == selectedId,
            onClick = { onSelect(cuenta.id) },
            enabled = enabled,
        )
      }
    }
    message?.let { TallybookFieldMessageContent(TallybookFieldMessage.Error(it)) }
  }
}

@Suppress("LongParameterList")
@Composable
private fun TransferAmountField(
    label: String,
    value: String,
    moneda: Moneda,
    error: AmountInputError?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
  val message = error?.let { TallybookFieldMessage.Error(amountMessage(it)) }
  TallybookAmountInput(
      value = value,
      onValueChange = onValueChange,
      label = label,
      currencySymbol = moneda.symbol,
      modifier = modifier,
      enabled = enabled,
      message = message,
      labelContent = {
        TallybookInputLabel(
            text = label,
            color =
                if (message != null) TallybookTheme.colors.status.danger.base
                else TallybookTheme.colors.foreground.tertiary,
        )
      },
  )
}

@Composable
private fun TransferDateField(
    form: TransferDraftState,
    isFuture: Boolean,
    enabled: Boolean,
    actions: TransferFormActions,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing)) {
    TallybookInputLabel(stringResource(Res.string.date))
    TallybookDateChipPicker(
        value = form.draft.fecha,
        onValueChange = actions::onFechaChanged,
        label = stringResource(Res.string.date),
        size = TallybookDateChipSize.M,
        enabled = enabled,
        maxDate = form.today,
    )
    if (isFuture) {
      TallybookFieldMessageContent(
          TallybookFieldMessage.Error(stringResource(Res.string.future_date_not_allowed))
      )
    }
  }
}

@Composable
private fun TransferDescripcionField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
      label = stringResource(Res.string.description_optional),
      message = TallybookFieldMessage.Supporting("${value.length}/$TRANSFERENCIA_DESCRIPCION_MAX"),
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}

@Composable
private fun labelWithMoneda(label: StringResource, moneda: Moneda): String =
    stringResource(Res.string.transfer_with_moneda, stringResource(label), moneda.name)

@Composable
private fun amountMessage(error: AmountInputError): String =
    stringResource(
        when (error) {
          AmountInputError.EMPTY -> Res.string.amount_required
          AmountInputError.MALFORMED -> Res.string.amount_malformed
          AmountInputError.NOT_POSITIVE -> Res.string.amount_not_positive
          AmountInputError.TOO_MANY_DECIMALS -> Res.string.amount_too_many_decimals
          AmountInputError.OVERFLOW -> Res.string.amount_overflow
        }
    )
