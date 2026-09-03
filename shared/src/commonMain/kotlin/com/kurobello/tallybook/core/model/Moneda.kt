package com.kurobello.tallybook.core.model

private const val NBSP = "\u00A0"

/**
 * Símbolo y separadores portados de CLDR — los mismos valores que `Intl.NumberFormat(locale, {
 * style: 'currency', currencyDisplay: 'narrowSymbol' })` produce en moneta
 * (`src/components/shared/movimientoView.ts`), leídos en el locale de origen de cada moneda. moneta
 * delega en `Intl` y no tiene tabla propia; acá hace falta explícita porque `commonMain` no tiene
 * API de locale.
 *
 * `symbolSeparator` es NBSP y no espacio común: es lo que trae CLDR, y evita que el símbolo quede
 * separado del monto en un salto de línea.
 */
enum class Moneda(
    val isoCode: String,
    val scale: Int,
    val symbol: String,
    val symbolSeparator: String,
    val groupSeparator: String,
    val decimalSeparator: String,
) {
  COP("COP", 2, "$", NBSP, ".", ","),
  MXN("MXN", 2, "$", "", ",", "."),
  ARS("ARS", 2, "$", NBSP, ".", ","),
  CLP("CLP", 0, "$", "", ".", ","),
  BRL("BRL", 2, "R$", NBSP, ".", ","),
  USD("USD", 2, "$", "", ",", "."),
  DOP("DOP", 2, "$", "", ",", "."),
  PEN("PEN", 2, "S/", NBSP, ",", "."),
}
