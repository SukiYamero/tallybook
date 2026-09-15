package com.kurobello.tallybook.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class DatabaseSchemaTest {

  @Test
  fun `migration from schema 1 preserves a legacy movimiento and derives created at from fecha`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(
        null,
        """
        CREATE TABLE movimiento (
            id TEXT NOT NULL PRIMARY KEY,
            tipo TEXT NOT NULL,
            monto_minor INTEGER NOT NULL,
            monto_moneda TEXT NOT NULL,
            fecha TEXT NOT NULL,
            categoria_id TEXT NOT NULL,
            descripcion TEXT
        )
        """
            .trimIndent(),
        0,
    )
    driver.execute(null, "CREATE INDEX movimiento_fecha ON movimiento(fecha)", 0)
    driver.execute(
        null,
        """
        INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, categoria_id, descripcion)
        VALUES ('legacy', 'GASTO', 1_050, 'USD', '2026-09-03', 'cat_comida', 'almuerzo')
        """
            .trimIndent(),
        0,
    )

    TallybookDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)

    val row =
        TallybookDatabase(driver).movimientoQueries.selectMovimientoById("legacy").executeAsOne()
    assertEquals("legacy", row.id)
    assertEquals("GASTO", row.tipo)
    assertEquals(1_050L, row.monto_minor)
    assertEquals("USD", row.monto_moneda)
    assertEquals("2026-09-03", row.fecha)
    assertEquals("cat_comida", row.categoria_id)
    assertEquals("almuerzo", row.descripcion)
    assertEquals(Instant.parse("2026-09-03T00:00:00Z").toString(), row.created_at)
  }

  private fun inMemoryDatabase(): TallybookDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    return TallybookDatabase(driver)
  }

  private fun TallybookDatabase.insert(
      id: String,
      tipo: String = "GASTO",
      montoMinor: Long,
      moneda: String = "COP",
      fecha: String = "2026-09-03",
      categoriaId: String = "cat1",
      descripcion: String? = null,
      createdAt: String = "2026-09-03T12:00:00Z",
  ) =
      movimientoQueries.insertMovimiento(
          id = id,
          tipo = tipo,
          monto_minor = montoMinor,
          monto_moneda = moneda,
          fecha = fecha,
          categoria_id = categoriaId,
          descripcion = descripcion,
          created_at = createdAt,
      )

  @Test
  fun `a movimiento round-trips through the database`() {
    val db = inMemoryDatabase()

    db.insert(
        id = "mov1",
        tipo = "GASTO",
        montoMinor = 1050,
        moneda = "USD",
        fecha = "2026-09-03",
        categoriaId = "cat_comida",
        descripcion = "almuerzo",
    )
    val row = db.movimientoQueries.selectMovimientoById("mov1").executeAsOne()

    assertEquals("GASTO", row.tipo)
    assertEquals(1050L, row.monto_minor)
    assertEquals("USD", row.monto_moneda)
    assertEquals("2026-09-03", row.fecha)
    assertEquals("cat_comida", row.categoria_id)
    assertEquals("almuerzo", row.descripcion)
    assertEquals("2026-09-03T12:00:00Z", row.created_at)
  }

  @Test
  fun `descripcion is the only optional movimiento field`() {
    val db = inMemoryDatabase()

    db.insert(id = "mov1", tipo = "INGRESO", montoMinor = 120_000, categoriaId = "cat_sueldo")

    assertNull(db.movimientoQueries.selectMovimientoById("mov1").executeAsOne().descripcion)
  }

  @Test
  fun `a negative monto_minor round-trips exactly`() {
    val db = inMemoryDatabase()

    db.insert(id = "mov1", montoMinor = -2_500)

    assertEquals(
        -2_500L,
        db.movimientoQueries.selectMovimientoById("mov1").executeAsOne().monto_minor,
    )
  }

  @Test
  fun `monto_minor is stored verbatim whatever the scale of the moneda`() {
    val db = inMemoryDatabase()

    db.insert(id = "cop", montoMinor = 15_000, moneda = "COP")
    db.insert(id = "clp", montoMinor = 15_000, moneda = "CLP")

    assertEquals(
        15_000L,
        db.movimientoQueries.selectMovimientoById("cop").executeAsOne().monto_minor,
    )
    assertEquals(
        15_000L,
        db.movimientoQueries.selectMovimientoById("clp").executeAsOne().monto_minor,
    )
  }

  @Test
  fun `selectAllMovimientos returns the most recent fecha first`() {
    val db = inMemoryDatabase()
    listOf("2026-01-15", "2026-09-03", "2026-05-20").forEachIndexed { index, fecha ->
      db.insert(id = "mov$index", montoMinor = 100, fecha = fecha)
    }

    assertEquals(
        listOf("2026-09-03", "2026-05-20", "2026-01-15"),
        db.movimientoQueries.selectAllMovimientos().executeAsList().map { it.fecha },
    )
  }

  @Test
  fun `selectRecentMovimientos uses fecha then created at then id and obeys limit`() {
    val db = inMemoryDatabase()
    db.insert(id = "a", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T12:00:00Z")
    db.insert(id = "c", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T12:00:00Z")
    db.insert(id = "b", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T11:00:00Z")
    db.insert(id = "d", montoMinor = 100, fecha = "2026-09-02", createdAt = "2026-09-04T12:00:00Z")

    assertEquals(
        listOf("c", "a", "b"),
        db.movimientoQueries.selectRecentMovimientos(3).executeAsList().map { it.id },
    )
  }

  @Test
  fun `sumMovimientosByTipo filters by moneda before summing`() {
    val db = inMemoryDatabase()
    db.insert(id = "gasto", montoMinor = 500, moneda = "COP")
    db.insert(id = "ingreso", tipo = "INGRESO", montoMinor = 2_000, moneda = "COP")
    db.insert(id = "other", tipo = "INGRESO", montoMinor = 9_999, moneda = "USD")

    assertEquals(
        mapOf("GASTO" to 500L, "INGRESO" to 2_000L),
        db.movimientoQueries.sumMovimientosByTipo("COP").executeAsList().associate {
          it.tipo to it.total_minor
        },
    )
  }

  @Test
  fun `deleteMovimiento removes the row`() {
    val db = inMemoryDatabase()
    db.insert(id = "mov1", montoMinor = 100)

    db.movimientoQueries.deleteMovimiento("mov1")

    assertNull(db.movimientoQueries.selectMovimientoById("mov1").executeAsOneOrNull())
  }

  @Test
  fun `sumMovimientosPorCategoria totals minor units per categoria and tipo`() {
    val db = inMemoryDatabase()
    db.insert(id = "in1", montoMinor = 1_500, categoriaId = "cat_comida", fecha = "2026-09-05")
    db.insert(id = "in2", montoMinor = 2_500, categoriaId = "cat_comida", fecha = "2026-09-20")
    db.insert(
        id = "in3",
        tipo = "INGRESO",
        montoMinor = 700,
        categoriaId = "cat_comida",
        fecha = "2026-09-10",
    )
    db.insert(id = "in4", montoMinor = -300, categoriaId = "cat_transporte", fecha = "2026-09-11")
    db.insert(
        id = "edge1",
        montoMinor = 1_000,
        categoriaId = "cat_transporte",
        fecha = "2026-09-01",
    )
    db.insert(
        id = "edge2",
        montoMinor = 2_000,
        categoriaId = "cat_transporte",
        fecha = "2026-09-30",
    )
    db.insert(id = "before", montoMinor = 9_999, categoriaId = "cat_comida", fecha = "2026-08-31")
    db.insert(id = "after", montoMinor = 8_888, categoriaId = "cat_comida", fecha = "2026-10-01")
    db.insert(
        id = "otherMoneda",
        montoMinor = 7_777,
        moneda = "USD",
        categoriaId = "cat_comida",
        fecha = "2026-09-15",
    )

    val totals =
        db.movimientoQueries
            .sumMovimientosPorCategoria("COP", "2026-09-01", "2026-09-30")
            .executeAsList()
            .associate { (it.categoria_id to it.tipo) to it.total_minor }

    assertEquals(
        mapOf(
            ("cat_comida" to "GASTO") to 4_000L,
            ("cat_comida" to "INGRESO") to 700L,
            ("cat_transporte" to "GASTO") to 2_700L,
        ),
        totals,
    )
  }

  @Test
  fun `a categoria hija keeps its padre_id while a categoria padre has none`() {
    val db = inMemoryDatabase()

    db.categoriaQueries.insertCategoria("cat_ingresos", "Ingresos", null, "trending-up", "success")
    db.categoriaQueries.insertCategoria("cat_sueldo", "Sueldo", "cat_ingresos", "wallet", "emerald")

    val byId = db.categoriaQueries.selectAllCategorias().executeAsList().associateBy { it.id }
    assertNull(byId.getValue("cat_ingresos").padre_id)
    assertEquals("cat_ingresos", byId.getValue("cat_sueldo").padre_id)
    assertEquals("trending-up", byId.getValue("cat_ingresos").icono)
    assertEquals("emerald", byId.getValue("cat_sueldo").color)
  }
}
