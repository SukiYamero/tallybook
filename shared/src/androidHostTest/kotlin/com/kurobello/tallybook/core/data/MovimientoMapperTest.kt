package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento as DatabaseMovimiento
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class MovimientoMapperTest {

  @Test
  fun `toDomain parses a valid created at instant`() {
    val createdAt = Instant.parse("2026-09-03T12:00:00Z")

    assertEquals(
        com.kurobello.tallybook.core.model.Movimiento(
            id = "mov1",
            tipo = TipoMovimiento.GASTO,
            monto = Money(1_050, Moneda.USD),
            fecha = LocalDate(2026, 9, 3),
            categoriaId = "cat_comida",
            descripcion = "almuerzo",
            createdAt = createdAt,
        ),
        databaseMovimiento(createdAt.toString()).toDomain(),
    )
  }

  @Test
  fun `toDomain rejects an invalid created at instant`() {
    assertFailsWith<IllegalArgumentException> { databaseMovimiento("not-an-instant").toDomain() }
  }

  @Test
  fun `toDomain rejects zero and negative persisted movimiento amounts`() {
    for (amount in listOf(0L, -1L, Long.MIN_VALUE)) {
      assertFailsWith<IllegalArgumentException> {
        databaseMovimiento("2026-09-03T12:00:00Z").copy(monto_minor = amount).toDomain()
      }
    }
  }

  private fun databaseMovimiento(createdAt: String) =
      DatabaseMovimiento(
          id = "mov1",
          tipo = "GASTO",
          monto_minor = 1_050,
          monto_moneda = "USD",
          fecha = "2026-09-03",
          categoria_id = "cat_comida",
          descripcion = "almuerzo",
          created_at = createdAt,
      )
}
