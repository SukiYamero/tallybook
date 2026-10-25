package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento_con_etiquetas as DatabaseMovimiento
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.MedioPago
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
            titulo = "almuerzo",
            descripcion = "con Ana",
            createdAt = createdAt,
            cuentaId = "nequi",
            medio = MedioPago.TRANSFERENCIA,
            etiquetaIds = listOf("empresa"),
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

  @Test
  fun `toDomain orders etiquetas by posicion whatever the concatenation order`() {
    val shuffled = "2\u001Fc\u001E0\u001Fa\u001E1\u001Fb"

    assertEquals(
        listOf("a", "b", "c"),
        databaseMovimiento("2026-09-03T12:00:00Z")
            .copy(etiqueta_ids = shuffled)
            .toDomain()
            .etiquetaIds,
    )
  }

  @Test
  fun `toDomain reads a movimiento without etiquetas and rejects one without categoria`() {
    val row = databaseMovimiento("2026-09-03T12:00:00Z")

    assertEquals(emptyList(), row.copy(etiqueta_ids = "").toDomain().etiquetaIds)
    assertFailsWith<IllegalArgumentException> { row.copy(categoria_id = "").toDomain() }
  }

  private fun databaseMovimiento(createdAt: String) =
      DatabaseMovimiento(
          id = "mov1",
          tipo = "GASTO",
          monto_minor = 1_050,
          monto_moneda = "USD",
          fecha = "2026-09-03",
          descripcion = "con Ana",
          created_at = createdAt,
          titulo = "almuerzo",
          categoria_id = "cat_comida",
          cuenta_id = "nequi",
          medio_pago = "TRANSFERENCIA",
          updated_at = ChangeStamp.SEED.value,
          deleted_at = null,
          etiqueta_ids = "0\u001Fempresa",
      )
}
