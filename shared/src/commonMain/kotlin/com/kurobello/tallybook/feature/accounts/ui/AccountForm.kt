package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaNombreError
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaTypeIcon
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessageContent
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountDraftState
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSaldoError
import com.kurobello.tallybook.feature.accounts.viewmodel.onDiaCorte
import com.kurobello.tallybook.feature.accounts.viewmodel.onMoneda
import com.kurobello.tallybook.feature.accounts.viewmodel.onNombre
import com.kurobello.tallybook.feature.accounts.viewmodel.onSaldo
import com.kurobello.tallybook.feature.accounts.viewmodel.onTipo
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_amount_decimals
import tallybook.shared.generated.resources.account_amount_malformed
import tallybook.shared.generated.resources.account_amount_no_decimals
import tallybook.shared.generated.resources.account_amount_overflow
import tallybook.shared.generated.resources.account_amount_signed
import tallybook.shared.generated.resources.account_currency_label
import tallybook.shared.generated.resources.account_currency_locked
import tallybook.shared.generated.resources.account_duplicate
import tallybook.shared.generated.resources.account_name_blank
import tallybook.shared.generated.resources.account_name_label
import tallybook.shared.generated.resources.account_name_placeholder
import tallybook.shared.generated.resources.account_name_too_long
import tallybook.shared.generated.resources.account_opening_balance
import tallybook.shared.generated.resources.account_opening_debt
import tallybook.shared.generated.resources.account_statement_day_error
import tallybook.shared.generated.resources.account_statement_day_hint
import tallybook.shared.generated.resources.account_statement_day_label
import tallybook.shared.generated.resources.account_type_label
import tallybook.shared.generated.resources.account_type_locked

/**
 * [QUICK] is the in-place creation of a movimiento's selector: the medio presets the type, shown
 * read-only, and the optional statement day is left for the Accounts screen.
 */
internal enum class AccountFormMode {
  FULL,
  QUICK,
}

/** Rejections the repository returned for the current values, shown on their field. */
@Immutable
internal data class AccountFieldErrors(
    val duplicate: Cuenta? = null,
    val currencyLocked: Boolean = false,
    val typeLocked: Boolean = false,
)

/**
 * The fields of one account, shared by the account sheet and quick-create. Stateless over
 * [AccountDraftState]; callers own the title, save and dismissal. [duplicateAction] follows the
 * duplicate-name error, e.g. "Use Nequi".
 */
@Suppress("LongParameterList")
@Composable
internal fun AccountForm(
    state: AccountDraftState,
    onDraftChanged: (AccountDraftState.() -> AccountDraftState) -> Unit,
    modifier: Modifier = Modifier,
    mode: AccountFormMode = AccountFormMode.FULL,
    enabled: Boolean = true,
    errors: AccountFieldErrors = AccountFieldErrors(),
    duplicateAction: (@Composable () -> Unit)? = null,
    revealFocusedInput: @Composable Modifier.() -> Modifier = { this },
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
      AccountNombreField(
          state,
          errors.duplicate,
          enabled,
          onDraftChanged,
          Modifier.revealFocusedInput(),
      )
      if (errors.duplicate != null) duplicateAction?.invoke()
    }
    if (mode == AccountFormMode.FULL) {
      AccountTypeChoices(state.tipo, errors.typeLocked, enabled) { tipo ->
        onDraftChanged { onTipo(tipo) }
      }
    } else {
      PresetAccountType(state.tipo)
    }
    AccountCurrencyChoices(state.moneda, errors.currencyLocked, enabled) { moneda ->
      onDraftChanged { onMoneda(moneda) }
    }
    AccountSaldoField(state, enabled, onDraftChanged, Modifier.revealFocusedInput())
    if (mode == AccountFormMode.FULL && state.tipo == TipoCuenta.CREDITO) {
      AccountDiaCorteField(state, enabled, onDraftChanged, Modifier.revealFocusedInput())
    }
  }
}

@Composable
private fun AccountNombreField(
    state: AccountDraftState,
    duplicate: Cuenta?,
    enabled: Boolean,
    onDraftChanged: (AccountDraftState.() -> AccountDraftState) -> Unit,
    modifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  val message =
      when {
        state.visibleNombreError == CuentaNombreError.BLANK ->
            TallybookFieldMessage.Error(stringResource(Res.string.account_name_blank))
        state.visibleNombreError == CuentaNombreError.TOO_LONG ->
            TallybookFieldMessage.Error(
                stringResource(Res.string.account_name_too_long, CuentaRules.NOMBRE_MAX)
            )
        duplicate != null ->
            TallybookFieldMessage.Error(
                stringResource(
                    Res.string.account_duplicate,
                    duplicate.nombre,
                    duplicate.moneda.name,
                )
            )
        else -> TallybookFieldMessage.Supporting("${state.nombre.length}/${CuentaRules.NOMBRE_MAX}")
      }
  TallybookTextField(
      value = state.nombre,
      onValueChange = { value -> onDraftChanged { onNombre(value) } },
      modifier = modifier.fillMaxWidth(),
      label = stringResource(Res.string.account_name_label),
      placeholder = stringResource(Res.string.account_name_placeholder),
      message = message,
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Done,
          ),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}

@Composable
private fun AccountTypeChoices(
    selected: TipoCuenta,
    locked: Boolean,
    enabled: Boolean,
    onSelect: (TipoCuenta) -> Unit,
) {
  ChoiceGroup(
      label = stringResource(Res.string.account_type_label),
      error = if (locked) stringResource(Res.string.account_type_locked) else null,
  ) {
    TipoCuenta.entries.forEach { tipo ->
      TallybookChoicePill(
          stringResource(tipo.labelResource),
          selected == tipo,
          { onSelect(tipo) },
          enabled = enabled,
      )
    }
  }
}

@Composable
private fun PresetAccountType(tipo: TipoCuenta) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing)) {
    TallybookInputLabel(stringResource(Res.string.account_type_label))
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CuentaTypeIcon(
          tipo,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          contentDescription = null,
          tint = TallybookTheme.colors.foreground.secondary,
      )
      TallybookText(stringResource(tipo.labelResource), style = TallybookTheme.typography.body)
    }
  }
}

@Composable
private fun AccountCurrencyChoices(
    selected: Moneda,
    locked: Boolean,
    enabled: Boolean,
    onSelect: (Moneda) -> Unit,
) {
  ChoiceGroup(
      label = stringResource(Res.string.account_currency_label),
      error = if (locked) stringResource(Res.string.account_currency_locked) else null,
  ) {
    Moneda.entries.forEach { moneda ->
      TallybookChoicePill(moneda.name, selected == moneda, { onSelect(moneda) }, enabled = enabled)
    }
  }
}

@Composable
private fun ChoiceGroup(label: String, error: String?, choices: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing)) {
    TallybookInputLabel(
        label,
        color =
            if (error != null) TallybookTheme.colors.status.danger.base
            else TallybookTheme.colors.foreground.tertiary,
    )
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      choices()
    }
    error?.let { TallybookFieldMessageContent(TallybookFieldMessage.Error(it)) }
  }
}

@Composable
private fun AccountSaldoField(
    state: AccountDraftState,
    enabled: Boolean,
    onDraftChanged: (AccountDraftState.() -> AccountDraftState) -> Unit,
    modifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = state.saldo,
      onValueChange = { value -> onDraftChanged { onSaldo(value) } },
      modifier = modifier.fillMaxWidth(),
      label =
          stringResource(
              if (state.tipo == TipoCuenta.CREDITO) Res.string.account_opening_debt
              else Res.string.account_opening_balance
          ),
      placeholder = "0",
      message =
          state.saldoError?.let { TallybookFieldMessage.Error(saldoMessage(it, state.moneda)) },
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
      leadingContent = {
        TallybookText(
            state.moneda.symbol,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.body,
        )
      },
  )
}

@Composable
private fun saldoMessage(error: AccountSaldoError, moneda: Moneda): String =
    when (error) {
      AccountSaldoError.MALFORMED ->
          stringResource(Res.string.account_amount_malformed, moneda.decimalSeparator)
      AccountSaldoError.TOO_MANY_DECIMALS ->
          if (moneda.scale == 0) stringResource(Res.string.account_amount_no_decimals)
          else stringResource(Res.string.account_amount_decimals, moneda.scale)
      AccountSaldoError.OVERFLOW -> stringResource(Res.string.account_amount_overflow)
      AccountSaldoError.SIGNED -> stringResource(Res.string.account_amount_signed)
    }

@Composable
private fun AccountDiaCorteField(
    state: AccountDraftState,
    enabled: Boolean,
    onDraftChanged: (AccountDraftState.() -> AccountDraftState) -> Unit,
    modifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = state.diaCorte,
      onValueChange = { value -> onDraftChanged { onDiaCorte(value) } },
      modifier = modifier.fillMaxWidth(),
      label = stringResource(Res.string.account_statement_day_label),
      message =
          if (state.diaCorteError != null)
              TallybookFieldMessage.Error(stringResource(Res.string.account_statement_day_error))
          else
              TallybookFieldMessage.Supporting(
                  stringResource(Res.string.account_statement_day_hint)
              ),
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}
