package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val BOM = "﻿"
private const val CRLF = "\r\n"
private const val HEADER =
    "Fecha,Tipo,Categoría,Categoría padre,Tags,Título,Descripción,Monto,Moneda"

private val labels =
    MovimientoCsvLabels(
        dateHeader = "Fecha",
        typeHeader = "Tipo",
        categoryHeader = "Categoría",
        parentCategoryHeader = "Categoría padre",
        tagsHeader = "Tags",
        titleHeader = "Título",
        descriptionHeader = "Descripción",
        amountHeader = "Monto",
        currencyHeader = "Moneda",
        expenseLabel = "Gasto",
        incomeLabel = "Ingreso",
    )

private val comida = categoria(id = "comida", nombre = "Comida")
private val supermercado = categoria(id = "super", nombre = "Supermercado", padreId = "comida")
private val sueldo = categoria(id = "sueldo", nombre = "Sueldo")
private val categorias = listOf(comida, supermercado, sueldo)
private val etiquetas =
    listOf(
        Etiqueta("empresa", "Empresa", Instant.fromEpochSeconds(0)),
        Etiqueta("viaje", "Viaje Chile", Instant.fromEpochSeconds(0)),
    )

class MovimientoCsvTest {

  @Test
  fun startsWithBomAndLocalizedHeaderAndEndsEveryRecordWithCrlf() {
    val csv = buildMovimientoCsv(listOf(movimiento(id = "a")), categorias, etiquetas, labels)

    assertTrue(csv.startsWith(BOM + HEADER + CRLF))
    assertTrue(csv.endsWith(CRLF))
    assertEquals(2, csv.split(CRLF).size - 1)
    assertEquals(
        BOM + HEADER + CRLF,
        buildMovimientoCsv(emptyList(), categorias, etiquetas, labels),
    )
  }

  @Test
  fun sortsByFechaThenCreatedAtThenIdDescending() {
    val movimientos =
        listOf(
            movimiento(id = "old", fecha = LocalDate(2026, 1, 1), createdAtSeconds = 50),
            movimiento(id = "a", fecha = LocalDate(2026, 2, 1), createdAtSeconds = 10),
            movimiento(id = "b", fecha = LocalDate(2026, 2, 1), createdAtSeconds = 10),
            movimiento(id = "late", fecha = LocalDate(2026, 2, 1), createdAtSeconds = 20),
            movimiento(id = "new", fecha = LocalDate(2026, 3, 1), createdAtSeconds = 0),
        )

    val labelled = movimientos.map { it.copy(titulo = it.id) }

    val titles =
        dataRows(buildMovimientoCsv(labelled, categorias, etiquetas, labels)).map {
          it.split(",")[5]
        }

    assertEquals(listOf("new", "late", "b", "a", "old"), titles)
  }

  @Test
  fun writesTypeLabelsCategoryNamesAndParentCategory() {
    val movimientos =
        listOf(
            movimiento(
                id = "gasto",
                fecha = LocalDate(2026, 3, 5),
                categoriaId = "super",
                titulo = "Feria",
                descripcion = "Frutas y verduras",
                monto = Money(1500, Moneda.CLP),
            ),
            movimiento(
                id = "ingreso",
                fecha = LocalDate(2026, 3, 1),
                tipo = TipoMovimiento.INGRESO,
                categoriaId = "sueldo",
                monto = Money(250_000_000, Moneda.COP),
            ),
        )

    assertEquals(
        listOf(
            "2026-03-05,Gasto,Supermercado,Comida,,Feria,Frutas y verduras,1500,CLP",
            "2026-03-01,Ingreso,Sueldo,Sueldo,,,,2500000.00,COP",
        ),
        dataRows(buildMovimientoCsv(movimientos, categorias, etiquetas, labels)),
    )
  }

  @Test
  fun formatsAmountsWithExactlyTheMonedaScaleAndNoSign() {
    val amounts =
        listOf(
                Money(0, Moneda.CLP),
                Money(1_234_567, Moneda.CLP),
                Money(5, Moneda.COP),
                Money(100, Moneda.COP),
                Money(123_456, Moneda.USD),
                Money(-4_250, Moneda.BRL),
            )
            .mapIndexed { index, monto -> movimiento(id = "$index", monto = monto) }

    val formatted =
        dataRows(buildMovimientoCsv(amounts, categorias, etiquetas, labels)).map {
          it.split(",")[7]
        }

    assertEquals(listOf("0", "1234567", "0.05", "1.00", "1234.56", "42.50"), formatted.reversed())
  }

  @Test
  fun quotesFieldsWithSeparatorsQuotesAndLineBreaksDoublingInnerQuotes() {
    val descriptions =
        listOf(
            "Pan, leche" to "\"Pan, leche\"",
            "Dijo \"hola\"" to "\"Dijo \"\"hola\"\"\"",
            "Línea 1\nLínea 2" to "\"Línea 1\nLínea 2\"",
            "Línea 1\r\nLínea 2" to "\"Línea 1\r\nLínea 2\"",
            "Sin comillas" to "Sin comillas",
        )
    descriptions.forEach { (descripcion, expected) ->
      val csv =
          buildMovimientoCsv(
              listOf(movimiento(id = "a", categoriaId = "sueldo", descripcion = descripcion)),
              categorias,
              etiquetas,
              labels,
          )

      assertTrue(
          csv.endsWith(",Sueldo,Sueldo,,,$expected,1000,CLP$CRLF"),
          "for '$descripcion': $csv",
      )
    }
  }

  @Test
  fun prefixesTextThatSpreadsheetsWouldEvaluateAsFormula() {
    val guarded =
        listOf(
            "=SUM(A1)" to "'=SUM(A1)",
            "+56 9 1234" to "'+56 9 1234",
            "-5 descuento" to "'-5 descuento",
            "@persona" to "'@persona",
            "=1,2" to "\"'=1,2\"",
            "a=b" to "a=b",
        )
    guarded.forEach { (descripcion, expected) ->
      val row =
          dataRows(
                  buildMovimientoCsv(
                      listOf(movimiento(id = "a", descripcion = descripcion)),
                      categorias,
                      etiquetas,
                      labels,
                  ),
              )
              .single()

      assertTrue(row.contains(",$expected,"), "for '$descripcion': $row")
    }
    val formulaCategoria = categoria(id = "f", nombre = "=HYPERLINK(\"x\")")
    val row =
        dataRows(
                buildMovimientoCsv(
                    listOf(movimiento(id = "a", categoriaId = "f")),
                    listOf(formulaCategoria),
                    emptyList(),
                    labels,
                ),
            )
            .single()
    assertTrue(row.contains(",\"'=HYPERLINK(\"\"x\"\")\","), row)
  }

  @Test
  fun joinsTagNamesInTheMovimientoOrderAndSkipsDeletedTags() {
    val movimientos =
        listOf(
            movimiento(id = "a", etiquetaIds = listOf("viaje", "empresa")),
            movimiento(id = "b", etiquetaIds = listOf("deleted", "empresa"), createdAtSeconds = 5),
        )

    assertEquals(
        listOf(
            "2026-01-15,Gasto,Comida,Comida,Empresa,,,1000,CLP",
            "2026-01-15,Gasto,Comida,Comida,Viaje Chile · Empresa,,,1000,CLP",
        ),
        dataRows(buildMovimientoCsv(movimientos, categorias, etiquetas, labels)),
    )
  }

  @Test
  fun leavesUnknownCategoryAndMissingParentOrDescriptionEmpty() {
    val orphan = categoria(id = "orphan", nombre = "Huérfana", padreId = "deleted")
    val movimientos =
        listOf(
            movimiento(id = "b", categoriaId = "missing"),
            movimiento(id = "a", categoriaId = "orphan"),
        )

    assertEquals(
        listOf("2026-01-15,Gasto,,,,,,1000,CLP", "2026-01-15,Gasto,Huérfana,,,,,1000,CLP"),
        dataRows(buildMovimientoCsv(movimientos, categorias + orphan, etiquetas, labels)),
    )
  }
}

private fun dataRows(csv: String): List<String> =
    csv.removePrefix(BOM).removeSuffix(CRLF).split(CRLF).drop(1)

private fun categoria(id: String, nombre: String, padreId: String? = null) =
    Categoria(
        id = id,
        nombre = nombre,
        padreId = padreId,
        icono = CategoryIcon.entries.first(),
        color = CategoryTint.entries.first(),
    )

private fun movimiento(
    id: String,
    fecha: LocalDate = LocalDate(2026, 1, 15),
    createdAtSeconds: Long = 0,
    tipo: TipoMovimiento = TipoMovimiento.GASTO,
    monto: Money = Money(1000, Moneda.CLP),
    categoriaId: String = "comida",
    titulo: String? = null,
    descripcion: String? = null,
    etiquetaIds: List<String> = emptyList(),
) =
    Movimiento(
        id = id,
        tipo = tipo,
        monto = monto,
        fecha = fecha,
        categoriaId = categoriaId,
        titulo = titulo,
        descripcion = descripcion,
        createdAt = Instant.fromEpochSeconds(createdAtSeconds),
        etiquetaIds = etiquetaIds,
    )
