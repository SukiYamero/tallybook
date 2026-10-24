package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento

/**
 * The account a sentence names. [alternativeIds] are the other matched accounts to offer; when
 * [cuenta] is null they are the tied candidates of an ambiguous name.
 */
internal data class SentenceCuenta(val cuenta: Cuenta?, val alternativeIds: List<String>)

/** [candidates] is non-empty only when [medio] is undecided. */
internal data class SentenceMedio(val medio: MedioPago?, val candidates: Set<MedioPago>)

/**
 * The earliest named account wins. A name shared across currencies resolves to the [primary]
 * currency's account, otherwise it stays undecided.
 */
internal fun resolveSentenceCuenta(
    matches: List<CuentaMatch>,
    cuentas: List<Cuenta>,
    primary: Moneda,
): SentenceCuenta {
  val first = matches.firstOrNull() ?: return SentenceCuenta(null, emptyList())
  val byId = cuentas.associateBy(Cuenta::id)
  val tied =
      matches
          .filter { it.start == first.start && it.endExclusive == first.endExclusive }
          .mapNotNull { byId[it.cuentaId] }
  val chosen = tied.singleOrNull() ?: tied.singleOrNull { it.moneda == primary }
  return SentenceCuenta(chosen, matches.map(CuentaMatch::cuentaId).filter { it != chosen?.id })
}

/**
 * A named account outranks the method words: its type implies the medio and a contradicting word is
 * dropped, except that a transfer word turns a bank account's default into
 * [MedioPago.TRANSFERENCIA]. Every result is valid for [tipo].
 */
internal fun resolveSentenceMedio(
    parsed: ParsedMovimientoPhrase,
    tipo: TipoMovimiento,
    cuentaTipo: TipoCuenta?,
): SentenceMedio {
  val mentioned = setOfNotNull(parsed.medio) + parsed.medioCandidates
  if (cuentaTipo != null) {
    val namesTransfer = cuentaTipo == TipoCuenta.BANCO && MedioPago.TRANSFERENCIA in mentioned
    val medio = if (namesTransfer) MedioPago.TRANSFERENCIA else cuentaTipo.defaultMedio(tipo)
    return SentenceMedio(medio, emptySet())
  }
  val offered = mentioned.map { it.forTipo(tipo) }.toSet()
  val medio = parsed.medio?.forTipo(tipo) ?: offered.singleOrNull()
  return SentenceMedio(medio, if (medio == null) offered else emptySet())
}
