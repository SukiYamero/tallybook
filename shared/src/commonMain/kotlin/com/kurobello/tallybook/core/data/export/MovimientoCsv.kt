package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento

private const val BYTE_ORDER_MARK = "﻿"
private const val RECORD_SEPARATOR = "\r\n"
private const val FIELD_SEPARATOR = ","
private const val QUOTE = "\""
private const val FORMULA_ESCAPE = "'"
private const val CATEGORY_SEPARATOR = " · "
private val CHARACTERS_REQUIRING_QUOTES = charArrayOf(',', '"', '\r', '\n')

// OWASP CSV-injection list: spreadsheets evaluate a cell starting with any of these as a formula.
private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')

data class MovimientoCsvLabels(
    val dateHeader: String,
    val typeHeader: String,
    val categoryHeader: String,
    val parentCategoryHeader: String,
    val descriptionHeader: String,
    val amountHeader: String,
    val currencyHeader: String,
    val expenseLabel: String,
    val incomeLabel: String,
)

/**
 * RFC 4180 document (CRLF records, every record terminated) prefixed with a UTF-8 BOM so Excel
 * detects the encoding. Amounts are unsigned plain decimals with `Moneda.scale` digits after a `.`:
 * the type column carries the direction, and a locale-free number stays importable everywhere. A
 * movimiento with several categorias lists every name in display order, joined by " · ".
 */
fun buildMovimientoCsv(
    movimientos: List<Movimiento>,
    categorias: List<Categoria>,
    labels: MovimientoCsvLabels,
): String {
  val categoriasById = categorias.associateBy(Categoria::id)
  val header =
      listOf(
          labels.dateHeader,
          labels.typeHeader,
          labels.categoryHeader,
          labels.parentCategoryHeader,
          labels.descriptionHeader,
          labels.amountHeader,
          labels.currencyHeader,
      )
  val rows =
      movimientos
          .sortedWith(
              compareByDescending<Movimiento> { it.fecha }
                  .thenByDescending { it.createdAt }
                  .thenByDescending { it.id },
          )
          .map { movimiento ->
            val categorias = movimiento.categoriaIds.mapNotNull(categoriasById::get)
            listOf(
                movimiento.fecha.toString(),
                labels.typeLabel(movimiento.tipo),
                categorias.joinToString(CATEGORY_SEPARATOR, transform = Categoria::nombre),
                categorias
                    .mapNotNull { it.padreId?.let(categoriasById::get)?.nombre }
                    .distinct()
                    .joinToString(CATEGORY_SEPARATOR),
                movimiento.descripcion.orEmpty(),
                movimiento.monto.toPlainUnsignedDecimal(),
                movimiento.monto.moneda.name,
            )
          }
  return buildString {
    append(BYTE_ORDER_MARK)
    (listOf(header) + rows).forEach { record ->
      record.joinTo(this, FIELD_SEPARATOR) { it.toCsvField() }
      append(RECORD_SEPARATOR)
    }
  }
}

private fun MovimientoCsvLabels.typeLabel(tipo: TipoMovimiento): String =
    when (tipo) {
      TipoMovimiento.GASTO -> expenseLabel
      TipoMovimiento.INGRESO -> incomeLabel
    }

private fun Money.toPlainUnsignedDecimal(): String {
  val digits = minorUnits.toString().removePrefix("-").padStart(moneda.scale + 1, '0')
  if (moneda.scale == 0) return digits
  val cut = digits.length - moneda.scale
  return digits.take(cut) + "." + digits.drop(cut)
}

private fun String.toCsvField(): String {
  val guarded = if (isNotEmpty() && first() in FORMULA_TRIGGERS) FORMULA_ESCAPE + this else this
  return if (guarded.any { it in CHARACTERS_REQUIRING_QUOTES }) {
    QUOTE + guarded.replace(QUOTE, QUOTE + QUOTE) + QUOTE
  } else {
    guarded
  }
}
