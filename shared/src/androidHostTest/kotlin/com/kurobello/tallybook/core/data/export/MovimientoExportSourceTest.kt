package com.kurobello.tallybook.core.data.export

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.createSchemaWithReferences
import com.kurobello.tallybook.core.data.insertLegacy
import com.kurobello.tallybook.core.data.insertTransferenciaRow
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MovimientoExportSourceTest {

  @Test
  fun `every list of the export is read in one transaction`() = runTest {
    val driver = TransactionTrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      val database = createSchemaWithReferences(driver, listOf("food"), listOf("trip"))
      database.insertLegacy(MOVIMIENTO)
      database.insertTransferenciaRow(TRANSFERENCIA, ChangeStamp.SEED)
      driver.readTransactions.clear()

      val content =
          assertIs<DataOutcome.Success<MovimientoExportContent>>(
                  DatabaseMovimientoExportSource(database, StandardTestDispatcher(testScheduler))
                      .read()
              )
              .data

      assertEquals(listOf(MOVIMIENTO), content.movimientos)
      assertEquals(listOf(TRANSFERENCIA), content.transferencias)
      assertEquals(
          Moneda.entries.map { Cuenta.defaultCashId(it) }.sorted(),
          content.cuentas.map { it.id }.sorted(),
      )
      assertEquals(CategorySeedCatalog.categories.size + 1, content.categorias.size)
      assertEquals(listOf("trip"), content.etiquetas.map { it.id })
      assertEquals(5, driver.readTransactions.size)
      val transaction = assertNotNull(driver.readTransactions.first())
      assertTrue(driver.readTransactions.all { it === transaction })
    } finally {
      driver.close()
    }
  }

  @Test
  fun `closed storage fails the whole export`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val database = createSchemaWithReferences(driver)
    driver.close()

    assertIs<DataError.Local>(
        assertIs<DataOutcome.Failure>(
                DatabaseMovimientoExportSource(database, StandardTestDispatcher(testScheduler))
                    .read()
            )
            .error
    )
  }

  private class TransactionTrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    val readTransactions = mutableListOf<Any?>()

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (sql.trimStart().startsWith("SELECT")) readTransactions += currentTransaction()
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }
  }

  private companion object {
    val MOVIMIENTO =
        Movimiento(
            id = "lunch",
            tipo = TipoMovimiento.GASTO,
            monto = Money(1_500, Moneda.COP),
            fecha = LocalDate(2026, 10, 1),
            categoriaId = "food",
            titulo = "Lunch",
            descripcion = null,
            createdAt = Instant.parse("2026-10-01T12:00:00Z"),
            cuentaId = Cuenta.defaultCashId(Moneda.COP),
            medio = MedioPago.EFECTIVO,
            etiquetaIds = listOf("trip"),
        )
    val TRANSFERENCIA =
        Transferencia(
            id = "move",
            cuentaOrigenId = Cuenta.defaultCashId(Moneda.COP),
            cuentaDestinoId = Cuenta.defaultCashId(Moneda.USD),
            montoOrigen = Money(400_000, Moneda.COP),
            montoDestino = Money(10_000, Moneda.USD),
            fecha = LocalDate(2026, 10, 2),
            descripcion = null,
            createdAt = Instant.parse("2026-10-02T12:00:00Z"),
        )
  }
}
