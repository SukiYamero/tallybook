package com.kurobello.tallybook.core.model

private const val NBSP = "\u00A0"

/**
 * Symbol and separators ported from CLDR — the same values `Intl.NumberFormat(locale, { style:
 * 'currency', currencyDisplay: 'narrowSymbol' })` produces in moneta
 * (`src/components/shared/movimientoView.ts`), read in each moneda's origin locale. moneta
 * delegates to `Intl` and has no table of its own; here it has to be explicit because `commonMain`
 * has no locale API.
 *
 * `symbolSeparator` is NBSP and not a plain space: that is what CLDR ships, and it keeps the symbol
 * from being split off the monto by a line break.
 */
enum class Moneda(
    val scale: Int,
    val symbol: String,
    val symbolSeparator: String,
    val groupSeparator: String,
    val decimalSeparator: String,
) {
  COP(2, "$", NBSP, ".", ","),
  MXN(2, "$", "", ",", "."),
  ARS(2, "$", NBSP, ".", ","),
  CLP(0, "$", "", ".", ","),
  BRL(2, "R$", NBSP, ".", ","),
  USD(2, "$", "", ",", "."),
  DOP(2, "$", "", ",", "."),
  PEN(2, "S/", NBSP, ",", "."),
}
