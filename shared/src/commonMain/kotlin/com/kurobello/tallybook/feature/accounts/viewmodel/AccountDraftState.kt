package com.kurobello.tallybook.feature.accounts.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaNombreError
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.DiaCorteError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.capTo
import com.kurobello.tallybook.core.text.isAsciiDigit
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.toAmountInput

internal enum class AccountSaldoError {
  MALFORMED,
  TOO_MANY_DECIMALS,
  OVERFLOW,

  /** The form asks for an unsigned amount; [TipoCuenta.CREDITO] reads it as debt. */
  SIGNED,
}

/**
 * The editable fields of one cuenta as typed. [saldo] is unsigned text in [moneda]'s separators:
 * the opening balance, or for [TipoCuenta.CREDITO] the opening debt. [toDraft] is the only place
 * that debt becomes the negative [CuentaDraft.saldoInicial] the repository stores, and [forEdit]
 * the only place it turns back. [diaCorte] is kept while the type changes but only a credit card
 * writes it.
 */
internal data class AccountDraftState(
    val nombre: String = "",
    val tipo: TipoCuenta = TipoCuenta.BANCO,
    val moneda: Moneda = Moneda.COP,
    val saldo: String = "",
    val diaCorte: String = "",
    val nombreEdited: Boolean = false,
) {
  val nombreError: CuentaNombreError?
    get() = CuentaRules.validate(nombre, tipo, diaCorte = null).nombre

  /** A pristine empty name is not an error yet: the form opens without shouting at the user. */
  val visibleNombreError: CuentaNombreError?
    get() = nombreError?.takeIf { nombreEdited || it != CuentaNombreError.BLANK }

  val saldoError: AccountSaldoError?
    get() = (parsedSaldo as? SaldoInput.Invalid)?.error

  val diaCorteError: DiaCorteError?
    get() {
      if (tipo != TipoCuenta.CREDITO || diaCorte.isEmpty()) return null
      return CuentaRules.validate(nombre, tipo, diaCorte.toIntOrNull() ?: 0).diaCorte
    }

  val isValid: Boolean
    get() = nombreError == null && saldoError == null && diaCorteError == null

  fun toDraft(): CuentaDraft? {
    val amount = (parsedSaldo as? SaldoInput.Valid)?.money?.takeIf { isValid } ?: return null
    return CuentaDraft(
        nombre = nombre.trim(),
        tipo = tipo,
        saldoInicial = if (tipo == TipoCuenta.CREDITO) -amount else amount,
        diaCorte = writtenDiaCorte,
    )
  }

  /** Equal once normalized, so trailing spaces or "1500" against "1500,00" are not changes. */
  fun sameContentAs(other: AccountDraftState): Boolean = contentKey == other.contentKey

  private val writtenDiaCorte: Int?
    get() = if (tipo == TipoCuenta.CREDITO) diaCorte.toIntOrNull() else null

  private val parsedSaldo: SaldoInput
    get() = parseSaldo(saldo, moneda)

  private val contentKey: List<Any?>
    get() =
        listOf(
            nombre.trim(),
            tipo,
            moneda,
            (parsedSaldo as? SaldoInput.Valid)?.money ?: saldo.trim(),
            writtenDiaCorte,
        )

  companion object {
    fun forNew(nombre: String, tipo: TipoCuenta, moneda: Moneda): AccountDraftState =
        AccountDraftState(
            nombre = nombre.trim().capTo(CuentaRules.NOMBRE_MAX),
            tipo = tipo,
            moneda = moneda,
        )

    fun forEdit(cuenta: Cuenta): AccountDraftState {
      val unsigned =
          if (cuenta.tipo == TipoCuenta.CREDITO) -cuenta.saldoInicial else cuenta.saldoInicial
      return AccountDraftState(
          nombre = cuenta.nombre,
          tipo = cuenta.tipo,
          moneda = cuenta.moneda,
          saldo = if (unsigned.minorUnits == 0L) "" else unsigned.toAmountInput(),
          diaCorte = cuenta.diaCorte?.toString().orEmpty(),
      )
    }
  }
}

internal fun AccountDraftState.onNombre(value: String): AccountDraftState =
    copy(nombre = value.capTo(CuentaRules.NOMBRE_MAX), nombreEdited = true)

internal fun AccountDraftState.onTipo(value: TipoCuenta): AccountDraftState = copy(tipo = value)

/**
 * A valid amount moves to the new currency's decimal separator; invalid text stays as typed so the
 * user still sees what they wrote.
 */
internal fun AccountDraftState.onMoneda(value: Moneda): AccountDraftState =
    copy(
        moneda = value,
        saldo =
            if (parseSaldo(saldo, moneda) is SaldoInput.Valid)
                saldo.replace(moneda.decimalSeparator, value.decimalSeparator)
            else saldo,
    )

internal fun AccountDraftState.onSaldo(value: String): AccountDraftState = copy(saldo = value)

internal fun AccountDraftState.onDiaCorte(value: String): AccountDraftState =
    copy(diaCorte = value.filter(Char::isAsciiDigit).take(DIA_CORTE_DIGITS))

private const val DIA_CORTE_DIGITS = 2

private sealed interface SaldoInput {
  data class Valid(val money: Money) : SaldoInput

  data class Invalid(val error: AccountSaldoError) : SaldoInput
}

/** Blank and zero are a zero opening balance; the movimiento parser alone rejects them. */
private fun parseSaldo(raw: String, moneda: Moneda): SaldoInput {
  val zero = SaldoInput.Valid(Money(0, moneda))
  return when (val parsed = parseAmountInput(raw, moneda)) {
    is AmountInputResult.Valid -> SaldoInput.Valid(parsed.money)
    is AmountInputResult.Invalid ->
        when (parsed.reason) {
          AmountInputError.EMPTY -> zero
          AmountInputError.NOT_POSITIVE ->
              if (raw.trim().startsWith('-')) SaldoInput.Invalid(AccountSaldoError.SIGNED) else zero
          AmountInputError.MALFORMED -> SaldoInput.Invalid(AccountSaldoError.MALFORMED)
          AmountInputError.TOO_MANY_DECIMALS ->
              SaldoInput.Invalid(AccountSaldoError.TOO_MANY_DECIMALS)
          AmountInputError.OVERFLOW -> SaldoInput.Invalid(AccountSaldoError.OVERFLOW)
        }
  }
}
