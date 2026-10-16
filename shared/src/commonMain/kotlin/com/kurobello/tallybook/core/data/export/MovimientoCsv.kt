package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento

private const val BYTE_ORDER_MARK = "﻿"
private const val RECORD_SEPARATOR = "\r\n"
private const val FIELD_SEPARATOR = ","
private const val QUOTE = "\""
private const val FORMULA_ESCAPE = "'"
private const val TAG_SEPARATOR = " · "
private val CHARACTERS_REQUIRING_QUOTES = charArrayOf(',', '"', '\r', '\n')

// OWASP CSV-injection list: spreadsheets evaluate a cell starting with any of these as a formula.
private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')

data class MovimientoCsvLabels(
    val dateHeader: String,
    val typeHeader: String,
    val categoryHeader: String,
    val parentCategoryHeader: String,
    val tagsHeader: String,
    val titleHeader: String,
    val descriptionHeader: String,
    val amountHeader: String,
    val currencyHeader: String,
    val expenseLabel: String,
    val incomeLabel: String,
)

/**
 * RFC 4180 document (CRLF records, every record terminated) prefixed with a UTF-8 BOM so Excel
 * detects the encoding. Amounts are unsigned plain decimals with `Moneda.scale` digits after a `.`:
 * the type column carries the direction, and a locale-free number stays importable everywhere. Tags
 * keep the movimiento's display order, joined by " · ".
 */
fun buildMovimientoCsv(
    movimientos: List<Movimiento>,
    categorias: List<Categoria>,
    etiquetas: List<Etiqueta>,
    labels: MovimientoCsvLabels,
): String {
  val categoriasById = categorias.associateBy(Categoria::id)
  val etiquetasById = etiquetas.associateBy(Etiqueta::id)
  val header =
      listOf(
          labels.dateHeader,
          labels.typeHeader,
          labels.categoryHeader,
          labels.parentCategoryHeader,
          labels.tagsHeader,
          labels.titleHeader,
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
            val categoria = categoriasById[movimiento.categoriaId]
            listOf(
                movimiento.fecha.toString(),
                labels.typeLabel(movimiento.tipo),
                categoria?.nombre.orEmpty(),
                categoria?.parentName(categoriasById).orEmpty(),
                movimiento.etiquetaIds
                    .mapNotNull(etiquetasById::get)
                    .joinToString(TAG_SEPARATOR, transform = Etiqueta::nombre),
                movimiento.titulo.orEmpty(),
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

// A root category is its own top level, so its parent cell repeats its name; a parent that no
// longer exists stays empty.
private fun Categoria.parentName(categoriasById: Map<String, Categoria>): String =
    if (padreId == null) nombre else categoriasById[padreId]?.nombre.orEmpty()

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
