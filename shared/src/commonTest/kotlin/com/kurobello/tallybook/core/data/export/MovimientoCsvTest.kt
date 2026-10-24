package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val BOM = "﻿"
private const val CRLF = "\r\n"
private const val HEADER =
    "Fecha,Tipo,Categoría,Categoría padre,Tags,Título,Descripción,Monto,Moneda," +
        "Cuenta,Medio,Cuenta destino,Monto destino,Moneda destino"

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
        accountHeader = "Cuenta",
        medioHeader = "Medio",
        destinationAccountHeader = "Cuenta destino",
        destinationAmountHeader = "Monto destino",
        destinationCurrencyHeader = "Moneda destino",
        transferLabel = "Transferencia entre cuentas",
        medioLabels =
            mapOf(
                MedioPago.EFECTIVO to "Efectivo",
                MedioPago.DEBITO to "Débito",
                MedioPago.TRANSFERENCIA to "Transferencia",
                MedioPago.CREDITO to "Crédito",
            ),
    )

private val cuentas =
    listOf(
        cuenta(id = Cuenta.defaultCashId(Moneda.CLP), nombre = "Billetera", moneda = Moneda.CLP),
        cuenta(id = Cuenta.defaultCashId(Moneda.COP), nombre = "Caja COP", moneda = Moneda.COP),
        cuenta(id = "nequi", nombre = "Nequi", moneda = Moneda.COP, tipo = TipoCuenta.BANCO),
        cuenta(id = "usd", nombre = "Ahorro USD", moneda = Moneda.USD, tipo = TipoCuenta.BANCO),
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
    val csv = exportCsv(listOf(movimiento(id = "a")), emptyList(), cuentas, categorias, etiquetas)

    assertTrue(csv.startsWith(BOM + HEADER + CRLF))
    assertTrue(csv.endsWith(CRLF))
    assertEquals(2, csv.split(CRLF).size - 1)
    assertEquals(
        BOM + HEADER + CRLF,
        exportCsv(emptyList(), emptyList(), cuentas, categorias, etiquetas),
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
        dataRows(exportCsv(labelled, emptyList(), cuentas, categorias, etiquetas)).map {
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
            "2026-03-05,Gasto,Supermercado,Comida,,Feria,Frutas y verduras,1500,CLP,Billetera,Efectivo,,,",
            "2026-03-01,Ingreso,Sueldo,Sueldo,,,,2500000.00,COP,Caja COP,Efectivo,,,",
        ),
        dataRows(exportCsv(movimientos, emptyList(), cuentas, categorias, etiquetas)),
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
        dataRows(exportCsv(amounts, emptyList(), cuentas, categorias, etiquetas)).map {
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
          exportCsv(
              listOf(movimiento(id = "a", categoriaId = "sueldo", descripcion = descripcion)),
              emptyList(),
              cuentas,
              categorias,
              etiquetas,
          )

      assertTrue(
          csv.endsWith(",Sueldo,Sueldo,,,$expected,1000,CLP,Billetera,Efectivo,,,$CRLF"),
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
                  exportCsv(
                      listOf(movimiento(id = "a", descripcion = descripcion)),
                      emptyList(),
                      cuentas,
                      categorias,
                      etiquetas,
                  ),
              )
              .single()

      assertTrue(row.contains(",$expected,"), "for '$descripcion': $row")
    }
    val formulaCategoria = categoria(id = "f", nombre = "=HYPERLINK(\"x\")")
    val row =
        dataRows(
                exportCsv(
                    listOf(movimiento(id = "a", categoriaId = "f")),
                    emptyList(),
                    cuentas,
                    listOf(formulaCategoria),
                    emptyList(),
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
            "2026-01-15,Gasto,Comida,Comida,Empresa,,,1000,CLP,Billetera,Efectivo,,,",
            "2026-01-15,Gasto,Comida,Comida,Viaje Chile · Empresa,,,1000,CLP,Billetera,Efectivo,,,",
        ),
        dataRows(exportCsv(movimientos, emptyList(), cuentas, categorias, etiquetas)),
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
        listOf(
            "2026-01-15,Gasto,,,,,,1000,CLP,Billetera,Efectivo,,,",
            "2026-01-15,Gasto,Huérfana,,,,,1000,CLP,Billetera,Efectivo,,,",
        ),
        dataRows(exportCsv(movimientos, emptyList(), cuentas, categorias + orphan, etiquetas)),
    )
  }

  @Test
  fun writesTheMovimientoAccountAndMedioAndLeavesDestinationCellsEmpty() {
    val movimiento =
        movimiento(id = "a", monto = Money(1250, Moneda.COP), categoriaId = "sueldo")
            .copy(cuentaId = "nequi", medio = MedioPago.DEBITO)

    assertEquals(
        listOf("2026-01-15,Gasto,Sueldo,Sueldo,,,,12.50,COP,Nequi,Débito,,,"),
        dataRows(
            exportCsv(listOf(movimiento), emptyList(), cuentas, categorias, etiquetas),
        ),
    )
  }

  @Test
  fun writesASameCurrencyTransferWithOnlyTypeDescriptionAmountsAndAccounts() {
    val transferencia =
        transferencia(
            id = "t",
            origenId = "nequi",
            destinoId = Cuenta.defaultCashId(Moneda.COP),
            montoOrigen = Money(5_000_000, Moneda.COP),
            montoDestino = Money(5_000_000, Moneda.COP),
            descripcion = "Retiro cajero",
        )

    assertEquals(
        listOf(
            "2026-01-15,Transferencia entre cuentas,,,,,Retiro cajero,50000.00,COP," +
                "Nequi,,Caja COP,50000.00,COP"
        ),
        dataRows(
            exportCsv(emptyList(), listOf(transferencia), cuentas, categorias, etiquetas),
        ),
    )
  }

  @Test
  fun writesACrossCurrencyTransferWithEachSideInItsOwnScale() {
    val transferencia =
        transferencia(
            id = "t",
            origenId = Cuenta.defaultCashId(Moneda.CLP),
            destinoId = "usd",
            montoOrigen = Money(50_000, Moneda.CLP),
            montoDestino = Money(5_250, Moneda.USD),
            descripcion = null,
        )

    assertEquals(
        listOf(
            "2026-01-15,Transferencia entre cuentas,,,,,,50000,CLP,Billetera,,Ahorro USD,52.50,USD"
        ),
        dataRows(
            exportCsv(emptyList(), listOf(transferencia), cuentas, categorias, etiquetas),
        ),
    )
  }

  @Test
  fun mergesMovimientosAndTransfersByFechaThenCreatedAtThenIdDescending() {
    val movimientos =
        listOf(
            movimiento(id = "m-old", fecha = LocalDate(2026, 1, 1), titulo = "m-old"),
            movimiento(id = "m-b", fecha = LocalDate(2026, 2, 1), createdAtSeconds = 10)
                .copy(titulo = "m-b"),
        )
    val transferencias =
        listOf(
            transferencia(id = "t-new", fecha = LocalDate(2026, 3, 1), descripcion = "t-new"),
            transferencia(
                id = "t-late",
                fecha = LocalDate(2026, 2, 1),
                createdAtSeconds = 20,
                descripcion = "t-late",
            ),
            transferencia(
                id = "m-a",
                fecha = LocalDate(2026, 2, 1),
                createdAtSeconds = 10,
                descripcion = "m-a",
            ),
        )

    val labelsInOrder =
        dataRows(
                exportCsv(movimientos, transferencias, cuentas, categorias, etiquetas),
            )
            .map { row -> row.split(",").let { it[5].ifEmpty { it[6] } } }

    assertEquals(listOf("t-new", "t-late", "m-b", "m-a", "m-old"), labelsInOrder)
  }

  @Test
  fun escapesAccountNamesAndTransferDescriptionsAndLeavesMissingAccountsEmpty() {
    val tricky = cuenta(id = "tricky", nombre = "Banco \"Uno\", ahorro", moneda = Moneda.COP)
    val formula = cuenta(id = "formula", nombre = "=Caja", moneda = Moneda.COP)
    val transferencia =
        transferencia(
            id = "t",
            origenId = "tricky",
            destinoId = "formula",
            descripcion = "+57 300",
        )
    val orphanMovimiento =
        movimiento(id = "m", monto = Money(1250, Moneda.COP)).copy(cuentaId = "deleted")

    assertEquals(
        listOf(
            "2026-01-15,Transferencia entre cuentas,,,,,'+57 300,10.00,COP," +
                "\"Banco \"\"Uno\"\", ahorro\",,'=Caja,10.00,COP",
            "2026-01-15,Gasto,Comida,Comida,,,,12.50,COP,,Efectivo,,,",
        ),
        dataRows(
            exportCsv(
                listOf(orphanMovimiento),
                listOf(transferencia),
                listOf(tricky, formula),
                categorias,
                etiquetas,
            ),
        ),
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
        cuentaId = Cuenta.defaultCashId(monto.moneda),
        medio = MedioPago.EFECTIVO,
        etiquetaIds = etiquetaIds,
    )

private fun cuenta(
    id: String,
    nombre: String,
    moneda: Moneda,
    tipo: TipoCuenta = TipoCuenta.EFECTIVO,
) =
    Cuenta(
        id = id,
        nombre = nombre,
        tipo = tipo,
        saldoInicial = Money(0, moneda),
        diaCorte = null,
        archivada = false,
        createdAt = Instant.fromEpochSeconds(0),
    )

private fun transferencia(
    id: String,
    fecha: LocalDate = LocalDate(2026, 1, 15),
    createdAtSeconds: Long = 0,
    origenId: String = "nequi",
    destinoId: String = Cuenta.defaultCashId(Moneda.COP),
    montoOrigen: Money = Money(1000, Moneda.COP),
    montoDestino: Money = montoOrigen,
    descripcion: String? = null,
) =
    Transferencia(
        id = id,
        cuentaOrigenId = origenId,
        cuentaDestinoId = destinoId,
        montoOrigen = montoOrigen,
        montoDestino = montoDestino,
        fecha = fecha,
        descripcion = descripcion,
        createdAt = Instant.fromEpochSeconds(createdAtSeconds),
    )

private fun exportCsv(
    movimientos: List<Movimiento>,
    transferencias: List<Transferencia>,
    cuentas: List<Cuenta>,
    categorias: List<Categoria>,
    etiquetas: List<Etiqueta>,
): String =
    buildMovimientoCsv(
        MovimientoExportContent(movimientos, transferencias, cuentas, categorias, etiquetas),
        labels,
    )
