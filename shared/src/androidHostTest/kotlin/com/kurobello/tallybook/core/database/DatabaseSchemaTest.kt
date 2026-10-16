package com.kurobello.tallybook.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.toDomain
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    TallybookDatabase.Schema.migrate(
        driver,
        oldVersion = 1,
        newVersion = TallybookDatabase.Schema.version,
    )

    val row =
        TallybookDatabase(driver).movimientoQueries.selectMovimientoById("legacy").executeAsOne()
    assertEquals("legacy", row.id)
    assertEquals("GASTO", row.tipo)
    assertEquals(1_050L, row.monto_minor)
    assertEquals("USD", row.monto_moneda)
    assertEquals("2026-09-03", row.fecha)
    assertEquals(listOf("cat_comida"), row.toDomain().categoriaIds)
    assertEquals("almuerzo", row.descripcion)
    assertEquals(Instant.parse("2026-09-03T00:00:00Z").toString(), row.created_at)
  }

  @Test
  fun `migration from schema 2 moves every categoria into the join table and rebuilds movimiento`() {
    val snapshot = File("src/commonMain/sqldelight/databases/2.db")
    assertTrue(snapshot.exists(), "schema 2 snapshot not found at ${snapshot.absolutePath}")
    val file = File.createTempFile("tallybook-v2", ".db")
    try {
      snapshot.copyTo(file, overwrite = true)
      val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
      try {
        val v2Rows =
            listOf(
                Triple("cafe", "cat_comida", "2026-09-03"),
                Triple("sueldo", "cat_sueldo", "2026-09-01"),
                Triple("orphan", "category-that-was-never-seeded", "2026-08-15"),
                Triple("tied", "cat_comida", "2026-09-03"),
            )
        v2Rows.forEachIndexed { index, (id, categoriaId, fecha) ->
          driver.execute(
              null,
              "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, categoria_id, " +
                  "descripcion, created_at) VALUES ('$id', 'GASTO', ${1_000 + index}, 'COP', " +
                  "'$fecha', '$categoriaId', ${if (index == 0) "'almuerzo'" else "NULL"}, " +
                  "'2026-09-03T12:00:0${index}Z')",
              0,
          )
        }

        TallybookDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = 3)

        val queries = TallybookDatabase(driver).movimientoQueries
        val migrated = queries.selectAllMovimientos().executeAsList().map { it.toDomain() }
        assertEquals(
            listOf("tied", "cafe", "sueldo", "orphan"),
            migrated.map { it.id },
            "fecha DESC, created_at DESC, id DESC order survives the rebuild",
        )
        assertEquals(
            mapOf(
                "cafe" to listOf("cat_comida"),
                "sueldo" to listOf("cat_sueldo"),
                "orphan" to listOf("category-that-was-never-seeded"),
                "tied" to listOf("cat_comida"),
            ),
            migrated.associate { it.id to it.categoriaIds },
        )
        val cafe = migrated.single { it.id == "cafe" }
        assertEquals("almuerzo", cafe.descripcion)
        assertEquals(1_000L, cafe.monto.minorUnits)
        assertEquals(Instant.parse("2026-09-03T12:00:00Z"), cafe.createdAt)
        assertEquals(
            listOf("cafe:0", "orphan:0", "sueldo:0", "tied:0"),
            driver.queryStrings(
                "SELECT movimiento_id || ':' || posicion FROM movimiento_categoria ORDER BY 1"
            ),
        )
        assertEquals(
            listOf(
                "id",
                "tipo",
                "monto_minor",
                "monto_moneda",
                "fecha",
                "descripcion",
                "created_at",
            ),
            driver.queryStrings("SELECT name FROM pragma_table_info('movimiento')"),
        )
        assertEquals(
            listOf("movimiento_categoria_categoria", "movimiento_recent"),
            driver.queryStrings(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            ),
        )
        assertEquals(
            listOf("categoria_id"),
            driver.queryStrings(
                "SELECT name FROM pragma_index_info('movimiento_categoria_categoria')"
            ),
        )
        assertEquals(
            listOf("fecha", "created_at", "id"),
            driver.queryStrings("SELECT name FROM pragma_index_info('movimiento_recent')"),
        )
        assertEquals(
            listOf("movimiento_con_categorias"),
            driver.queryStrings("SELECT name FROM sqlite_master WHERE type = 'view'"),
        )
        assertEquals(
            listOf("categoria", "movimiento", "movimiento_categoria"),
            driver.queryStrings(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name"
            ),
        )
      } finally {
        driver.close()
      }
    } finally {
      file.delete()
    }
  }

  private fun SqlDriver.queryStrings(sql: String): List<String> =
      executeQuery(
              null,
              sql,
              { cursor ->
                val rows = mutableListOf<String>()
                while (cursor.next().value) rows += requireNotNull(cursor.getString(0))
                QueryResult.Value(rows)
              },
              0,
          )
          .value

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
      categoriaIds: List<String> = listOf("cat1"),
      descripcion: String? = null,
      createdAt: String = "2026-09-03T12:00:00Z",
  ) {
    transaction {
      movimientoQueries.insertMovimiento(
          id = id,
          tipo = tipo,
          monto_minor = montoMinor,
          monto_moneda = moneda,
          fecha = fecha,
          descripcion = descripcion,
          created_at = createdAt,
      )
      categoriaIds.forEachIndexed { posicion, categoriaId ->
        movimientoQueries.insertMovimientoCategoria(id, categoriaId, posicion.toLong())
      }
    }
  }

  @Test
  fun `a movimiento round-trips through the database`() {
    val db = inMemoryDatabase()

    db.insert(
        id = "mov1",
        tipo = "GASTO",
        montoMinor = 1050,
        moneda = "USD",
        fecha = "2026-09-03",
        categoriaIds = listOf("cat_comida"),
        descripcion = "almuerzo",
    )
    val row = db.movimientoQueries.selectMovimientoById("mov1").executeAsOne()

    assertEquals("GASTO", row.tipo)
    assertEquals(1050L, row.monto_minor)
    assertEquals("USD", row.monto_moneda)
    assertEquals("2026-09-03", row.fecha)
    assertEquals(listOf("cat_comida"), row.toDomain().categoriaIds)
    assertEquals("almuerzo", row.descripcion)
    assertEquals("2026-09-03T12:00:00Z", row.created_at)
  }

  @Test
  fun `descripcion is the only optional movimiento field`() {
    val db = inMemoryDatabase()

    db.insert(
        id = "mov1",
        tipo = "INGRESO",
        montoMinor = 120_000,
        categoriaIds = listOf("cat_sueldo"),
    )

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
    db.insert(
        id = "in1",
        montoMinor = 1_500,
        categoriaIds = listOf("cat_comida"),
        fecha = "2026-09-05",
    )
    db.insert(
        id = "in2",
        montoMinor = 2_500,
        categoriaIds = listOf("cat_comida"),
        fecha = "2026-09-20",
    )
    db.insert(
        id = "in3",
        tipo = "INGRESO",
        montoMinor = 700,
        categoriaIds = listOf("cat_comida"),
        fecha = "2026-09-10",
    )
    db.insert(
        id = "in4",
        montoMinor = -300,
        categoriaIds = listOf("cat_transporte"),
        fecha = "2026-09-11",
    )
    db.insert(
        id = "edge1",
        montoMinor = 1_000,
        categoriaIds = listOf("cat_transporte"),
        fecha = "2026-09-01",
    )
    db.insert(
        id = "edge2",
        montoMinor = 2_000,
        categoriaIds = listOf("cat_transporte"),
        fecha = "2026-09-30",
    )
    db.insert(
        id = "before",
        montoMinor = 9_999,
        categoriaIds = listOf("cat_comida"),
        fecha = "2026-08-31",
    )
    db.insert(
        id = "after",
        montoMinor = 8_888,
        categoriaIds = listOf("cat_comida"),
        fecha = "2026-10-01",
    )
    db.insert(
        id = "otherMoneda",
        montoMinor = 7_777,
        moneda = "USD",
        categoriaIds = listOf("cat_comida"),
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
  fun `sumMovimientosPorCategoria counts the full amount in every categoria of a movimiento`() {
    val db = inMemoryDatabase()
    db.insert(id = "both", montoMinor = 1_000, categoriaIds = listOf("cat_comida", "cat_social"))
    db.insert(id = "one", montoMinor = 500, categoriaIds = listOf("cat_comida"))

    val perCategoria =
        db.movimientoQueries
            .sumMovimientosPorCategoria("COP", "2026-09-01", "2026-09-30")
            .executeAsList()
            .associate { (it.categoria_id to it.tipo) to it.total_minor }

    assertEquals(
        mapOf(("cat_comida" to "GASTO") to 1_500L, ("cat_social" to "GASTO") to 1_000L),
        perCategoria,
    )
    assertEquals(
        mapOf("GASTO" to 1_500L),
        db.movimientoQueries.sumMovimientosByTipo("COP").executeAsList().associate {
          it.tipo to it.total_minor
        },
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
