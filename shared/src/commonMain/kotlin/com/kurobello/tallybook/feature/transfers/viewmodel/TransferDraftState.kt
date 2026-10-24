package com.kurobello.tallybook.feature.transfers.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TRANSFERENCIA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaCuentaError
import com.kurobello.tallybook.core.model.TransferenciaErrors
import com.kurobello.tallybook.core.model.TransferenciaMontoError
import com.kurobello.tallybook.core.model.TransferenciaRules
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** The fields as typed; each amount is text in its own side's currency separators. */
internal data class TransferDraft(
    val origenId: String?,
    val destinoId: String?,
    val montoOrigen: String,
    val montoDestino: String,
    val fecha: LocalDate,
    val descripcion: String,
)

internal data class TransferFieldErrors(
    val origen: TransferenciaCuentaError? = null,
    val destino: TransferenciaCuentaError? = null,
    val montoOrigen: AmountInputError? = null,
    val montoDestino: AmountInputError? = null,
    val fechaFuture: Boolean = false,
)

/**
 * One transfer being written over a snapshot of the live [cuentas]. Unless both accounts are known
 * and differ in currency there is a single amount: [TransferDraft.montoOrigen] and
 * [TransferDraft.montoDestino] then hold the same text. Changing an account keeps the amount of the
 * side that did not change, so a Pay card debt survives choosing an origin in another currency.
 * [original] is the stored transfer being edited, whose archived sides stay selectable on their
 * side.
 */
@Suppress("TooManyFunctions")
internal data class TransferDraftState(
    val cuentas: List<Cuenta>,
    val primary: Moneda,
    val draft: TransferDraft,
    val today: LocalDate,
    val original: Transferencia? = null,
    val baseline: TransferDraft = draft,
    val submitAttempted: Boolean = false,
) {
  val origen: Cuenta?
    get() = draft.origenId?.let(::cuenta)

  val destino: Cuenta?
    get() = draft.destinoId?.let(::cuenta)

  val isCrossCurrency: Boolean
    get() = isCross(origen, destino)

  /** The currency of the single amount, or of the sent amount when the currencies differ. */
  val montoOrigenMoneda: Moneda
    get() = origen?.moneda ?: destino?.moneda ?: primary

  val montoDestinoMoneda: Moneda
    get() = destino?.moneda ?: montoOrigenMoneda

  val origenOptions: List<Cuenta>
    get() = selectable(original?.cuentaOrigenId)

  val destinoOptions: List<Cuenta>
    get() = selectable(original?.cuentaDestinoId)

  val visibleErrors: TransferFieldErrors
    get() = if (submitAttempted) errors else TransferFieldErrors()

  val hasUnsavedChanges: Boolean
    get() = contentKey(draft) != contentKey(baseline)

  fun showsMoneda(cuenta: Cuenta): Boolean = cuenta.moneda != primary

  /**
   * Null while any field is invalid; [id] and [createdAt] belong to the stored row when editing.
   */
  fun toTransferencia(id: String, createdAt: Instant): Transferencia? {
    val origenMoney = parsedOrigen()
    val destinoMoney = parsedDestino()
    val valid = errors == TransferFieldErrors() && rulesErrors(origenMoney, destinoMoney).isValid
    if (!valid || origenMoney == null || destinoMoney == null) return null
    return Transferencia(
        id = id,
        cuentaOrigenId = requireNotNull(draft.origenId),
        cuentaDestinoId = requireNotNull(draft.destinoId),
        montoOrigen = origenMoney,
        montoDestino = destinoMoney,
        fecha = draft.fecha,
        descripcion = draft.descripcion.normalizeOptionalText(),
        createdAt = createdAt,
    )
  }

  fun withOrigen(id: String): TransferDraftState =
      if (origenOptions.none { it.id == id }) this
      else withCuenta(draft.copy(origenId = id), changedOrigen = true)

  fun withDestino(id: String): TransferDraftState =
      if (destinoOptions.none { it.id == id }) this
      else withCuenta(draft.copy(destinoId = id), changedOrigen = false)

  fun withMontoOrigen(value: String): TransferDraftState =
      copy(
          draft =
              if (isCrossCurrency) draft.copy(montoOrigen = value)
              else draft.copy(montoOrigen = value, montoDestino = value)
      )

  fun withMontoDestino(value: String): TransferDraftState =
      if (isCrossCurrency) copy(draft = draft.copy(montoDestino = value))
      else withMontoOrigen(value)

  fun withFecha(value: LocalDate): TransferDraftState = copy(draft = draft.copy(fecha = value))

  fun withDescripcion(value: String): TransferDraftState =
      copy(draft = draft.copy(descripcion = value.take(TRANSFERENCIA_DESCRIPCION_MAX)))

  private val errors: TransferFieldErrors
    get() {
      val origenMoney = parsedOrigen()
      val destinoMoney = parsedDestino()
      val rules = rulesErrors(origenMoney, destinoMoney)
      return TransferFieldErrors(
          origen = rules.cuentaOrigen,
          destino = rules.cuentaDestino,
          montoOrigen = amountError(draft.montoOrigen, montoOrigenMoneda, rules.montoOrigen),
          montoDestino =
              if (isCrossCurrency)
                  amountError(draft.montoDestino, montoDestinoMoneda, rules.montoDestino)
              else null,
          fechaFuture = rules.fechaFuture,
      )
    }

  private fun rulesErrors(origenMoney: Money?, destinoMoney: Money?): TransferenciaErrors =
      TransferenciaRules.validate(
          origen = origen,
          destino = destino,
          montoOrigen = origenMoney,
          montoDestino = destinoMoney,
          fecha = draft.fecha,
          descripcion = draft.descripcion.normalizeOptionalText(),
          today = today,
      )

  private fun parsedOrigen(): Money? = parse(draft.montoOrigen, montoOrigenMoneda)

  /** A single amount moves the same money; the rules require it on both sides. */
  private fun parsedDestino(): Money? =
      if (isCrossCurrency) parse(draft.montoDestino, montoDestinoMoneda) else parsedOrigen()

  private fun withCuenta(next: TransferDraft, changedOrigen: Boolean): TransferDraftState {
    val wasCross = isCrossCurrency
    val before = if (changedOrigen) origen else destino
    val nowOrigen = next.origenId?.let(::cuenta)
    val nowDestino = next.destinoId?.let(::cuenta)
    val after = if (changedOrigen) nowOrigen else nowDestino
    val nowCross = isCross(nowOrigen, nowDestino)
    if (!nowCross) {
      val keptMoneda = if (changedOrigen) montoDestinoMoneda else montoOrigenMoneda
      val singleMoneda = nowOrigen?.moneda ?: nowDestino?.moneda ?: primary
      val kept = if (changedOrigen) next.montoDestino else next.montoOrigen
      val single = kept.reformatAmountInput(keptMoneda, singleMoneda)
      return copy(draft = next.copy(montoOrigen = single, montoDestino = single))
    }
    val changedText =
        when {
          !wasCross || before?.moneda != after?.moneda -> ""
          else -> if (changedOrigen) next.montoOrigen else next.montoDestino
        }
    val amounts =
        if (changedOrigen) next.copy(montoOrigen = changedText)
        else next.copy(montoDestino = changedText)
    return copy(draft = amounts)
  }

  private fun selectable(keptId: String?): List<Cuenta> =
      cuentas
          .filter { !it.archivada || it.id == keptId }
          .sortedWith(compareBy<Cuenta> { it.tipo.ordinal }.thenBy { it.nombre.lowercase() })

  private fun cuenta(id: String): Cuenta? = cuentas.firstOrNull { it.id == id }

  /** Equal once normalized, so "150" against "150,00" or trailing spaces are not changes. */
  private fun contentKey(value: TransferDraft): List<Any?> {
    val origenCuenta = value.origenId?.let(::cuenta)
    val destinoCuenta = value.destinoId?.let(::cuenta)
    val cross = isCross(origenCuenta, destinoCuenta)
    val origenMoneda = origenCuenta?.moneda ?: destinoCuenta?.moneda ?: primary
    return listOf(
        value.origenId,
        value.destinoId,
        parse(value.montoOrigen, origenMoneda) ?: value.montoOrigen.trim(),
        if (cross)
            destinoCuenta?.let { parse(value.montoDestino, it.moneda) } ?: value.montoDestino.trim()
        else null,
        value.fecha,
        value.descripcion.normalizeOptionalText(),
    )
  }

  companion object {
    fun forNew(
        cuentas: List<Cuenta>,
        primary: Moneda,
        entry: TransferEntry.New,
        today: LocalDate,
    ): TransferDraftState {
      val prefill = entry.prefill?.toAmountInput().orEmpty()
      val empty =
          TransferDraftState(
              cuentas = cuentas,
              primary = primary,
              draft = TransferDraft(null, null, prefill, prefill, today, ""),
              today = today,
          )
      val withDestino = entry.destinoId?.let(empty::withDestino) ?: empty
      val prefilled = entry.origenId?.let(withDestino::withOrigen) ?: withDestino
      return prefilled.copy(baseline = prefilled.draft)
    }

    fun forEdit(
        cuentas: List<Cuenta>,
        primary: Moneda,
        transferencia: Transferencia,
        today: LocalDate,
    ): TransferDraftState =
        TransferDraftState(
            cuentas = cuentas,
            primary = primary,
            draft =
                TransferDraft(
                    origenId = transferencia.cuentaOrigenId,
                    destinoId = transferencia.cuentaDestinoId,
                    montoOrigen = transferencia.montoOrigen.toAmountInput(),
                    montoDestino = transferencia.montoDestino.toAmountInput(),
                    fecha = transferencia.fecha,
                    descripcion = transferencia.descripcion.orEmpty(),
                ),
            today = today,
            original = transferencia,
        )
  }
}

private fun isCross(origen: Cuenta?, destino: Cuenta?): Boolean =
    origen != null && destino != null && origen.moneda != destino.moneda

private fun parse(text: String, moneda: Moneda): Money? =
    (parseAmountInput(text, moneda) as? AmountInputResult.Valid)?.money

/**
 * Parsing explains the typed text first. Each side is parsed in its own account's currency and a
 * single amount is passed for both sides, so the rules' currency and mismatch errors cannot arise
 * from this form; they still block [TransferDraftState.toTransferencia].
 */
private fun amountError(
    text: String,
    moneda: Moneda,
    rule: TransferenciaMontoError?,
): AmountInputError? =
    (parseAmountInput(text, moneda) as? AmountInputResult.Invalid)?.reason
        ?: when (rule) {
          TransferenciaMontoError.MISSING -> AmountInputError.EMPTY
          TransferenciaMontoError.NOT_POSITIVE -> AmountInputError.NOT_POSITIVE
          TransferenciaMontoError.WRONG_MONEDA,
          TransferenciaMontoError.DIFFERS_FROM_ORIGEN,
          null -> null
        }
