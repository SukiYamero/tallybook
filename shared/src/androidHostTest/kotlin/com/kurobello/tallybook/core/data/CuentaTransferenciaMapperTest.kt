package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class CuentaTransferenciaMapperTest {

  @Test
  fun `a cuenta and a cross-currency transferencia round-trip through their queries`() {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val card =
          Cuenta(
              id = "nu",
              nombre = "NU",
              tipo = TipoCuenta.CREDITO,
              saldoInicial = Money(-150_000, Moneda.COP),
              diaCorte = 15,
              archivada = true,
              createdAt = Instant.parse("2026-01-01T08:00:00Z"),
              color = TipoCuenta.CREDITO.defaultColor,
          )
      val wallet =
          Cuenta(
              id = "wise",
              nombre = "Wise",
              tipo = TipoCuenta.BANCO,
              saldoInicial = Money(0, Moneda.USD),
              diaCorte = null,
              archivada = false,
              createdAt = Instant.parse("2026-01-02T08:00:00Z"),
              color = TipoCuenta.BANCO.defaultColor,
          )
      val transferencia =
          Transferencia(
              id = "pay",
              cuentaOrigenId = "wise",
              cuentaDestinoId = "nu",
              montoOrigen = Money(2_500, Moneda.USD),
              montoDestino = Money(100_000, Moneda.COP),
              fecha = LocalDate(2026, 2, 1),
              descripcion = "pago tarjeta",
              createdAt = Instant.parse("2026-02-01T09:00:00Z"),
          )
      listOf(card, wallet).forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
      database.insertTransferenciaRow(transferencia, ChangeStamp.SEED)

      assertEquals(card, database.cuentaQueries.selectCuentaById("nu").executeAsOne().toDomain())
      assertEquals(
          wallet,
          database.cuentaQueries.selectCuentaById("wise").executeAsOne().toDomain(),
      )
      assertEquals(
          transferencia,
          database.transferenciaQueries.selectTransferenciaById("pay").executeAsOne().toDomain(),
      )
      driver.tombstone("transferencia", "pay")
      assertNull(database.transferenciaQueries.selectTransferenciaById("pay").executeAsOneOrNull())
    }
  }
}
