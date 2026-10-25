package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

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
    val accountHeader: String,
    val medioHeader: String,
    val destinationAccountHeader: String,
    val destinationAmountHeader: String,
    val destinationCurrencyHeader: String,
    val transferLabel: String,
    val medioLabels: Map<MedioPago, String>,
)

/** Live rows only: tombstoned rows never reach an export. */
data class MovimientoExportContent(
    val movimientos: List<Movimiento>,
    val transferencias: List<Transferencia>,
    val cuentas: List<Cuenta>,
    val categorias: List<Categoria>,
    val etiquetas: List<Etiqueta>,
)

/**
 * RFC 4180 document (CRLF records, every record terminated) prefixed with a UTF-8 BOM so Excel
 * detects the encoding. Amounts are unsigned plain decimals with `Moneda.scale` digits after a `.`:
 * the type column carries the direction, and a locale-free number stays importable everywhere. Tags
 * keep the movimiento's display order, joined by " · ". The account columns follow the original
 * nine so spreadsheets built on earlier exports keep lining up; a [Transferencia] is one row whose
 * amount and account are its origin side, with the destination side in the last three columns.
 */
fun buildMovimientoCsv(content: MovimientoExportContent, labels: MovimientoCsvLabels): String {
  val names =
      CsvNames(
          categorias = content.categorias.associateBy(Categoria::id),
          etiquetas = content.etiquetas.associateBy(Etiqueta::id),
          cuentas = content.cuentas.associate { it.id to it.nombre },
      )
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
          labels.accountHeader,
          labels.medioHeader,
          labels.destinationAccountHeader,
          labels.destinationAmountHeader,
          labels.destinationCurrencyHeader,
      )
  val rows =
      (content.movimientos.map { it.toRecord(names, labels) } +
              content.transferencias.map { it.toRecord(names, labels) })
          .sortedWith(NEWEST_FIRST)
          .map(CsvRecord::cells)
  return buildString {
    append(BYTE_ORDER_MARK)
    (listOf(header) + rows).forEach { record ->
      record.joinTo(this, FIELD_SEPARATOR) { it.toCsvField() }
      append(RECORD_SEPARATOR)
    }
  }
}

private class CsvNames(
    val categorias: Map<String, Categoria>,
    val etiquetas: Map<String, Etiqueta>,
    val cuentas: Map<String, String>,
)

private class CsvRecord(
    val fecha: LocalDate,
    val createdAt: Instant,
    val id: String,
    val cells: List<String>,
)

private val NEWEST_FIRST =
    compareByDescending<CsvRecord> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

private fun Movimiento.toRecord(names: CsvNames, labels: MovimientoCsvLabels): CsvRecord {
  val categoria = names.categorias[categoriaId]
  return CsvRecord(
      fecha = fecha,
      createdAt = createdAt,
      id = id,
      cells =
          listOf(
              fecha.toString(),
              labels.typeLabel(tipo),
              categoria?.nombre.orEmpty(),
              categoria?.parentName(names.categorias).orEmpty(),
              etiquetaIds
                  .mapNotNull(names.etiquetas::get)
                  .joinToString(TAG_SEPARATOR, transform = Etiqueta::nombre),
              titulo.orEmpty(),
              descripcion.orEmpty(),
              monto.toPlainUnsignedDecimal(),
              monto.moneda.name,
              names.cuentas[cuentaId].orEmpty(),
              labels.medioLabels.getValue(medio),
              "",
              "",
              "",
          ),
  )
}

private fun Transferencia.toRecord(names: CsvNames, labels: MovimientoCsvLabels): CsvRecord =
    CsvRecord(
        fecha = fecha,
        createdAt = createdAt,
        id = id,
        cells =
            listOf(
                fecha.toString(),
                labels.transferLabel,
                "",
                "",
                "",
                "",
                descripcion.orEmpty(),
                montoOrigen.toPlainUnsignedDecimal(),
                montoOrigen.moneda.name,
                names.cuentas[cuentaOrigenId].orEmpty(),
                "",
                names.cuentas[cuentaDestinoId].orEmpty(),
                montoDestino.toPlainUnsignedDecimal(),
                montoDestino.moneda.name,
            ),
    )

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
