package com.kurobello.tallybook.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.toDomain
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import java.io.File
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DatabaseSchemaTest {

  @Test
  fun `migration from schema 1 preserves a legacy movimiento and derives created at from fecha`() =
      withSnapshot(version = 1) { driver ->
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
            TallybookDatabase(driver)
                .movimientoQueries
                .selectMovimientoById("legacy")
                .executeAsOne()
        assertEquals("legacy", row.id)
        assertEquals("GASTO", row.tipo)
        assertEquals(1_050L, row.monto_minor)
        assertEquals("USD", row.monto_moneda)
        assertEquals("2026-09-03", row.fecha)
        assertEquals("cat_comida", row.toDomain().categoriaId)
        assertEquals("almuerzo", row.titulo)
        assertNull(row.descripcion)
        assertEquals(Instant.parse("2026-09-03T00:00:00Z").toString(), row.created_at)
      }

  @Test
  fun `migration from schema 5 keeps the first categoria and starts without etiquetas`() =
      withSnapshot(version = 5) { driver ->
        listOf(
                "('none', 'GASTO', 100, 'COP', '2026-10-03', 'sin categoria', NULL, " +
                    "'2026-10-03T12:00:00Z')",
                "('one', 'INGRESO', 200, 'COP', '2026-10-02', NULL, 'detalle', " +
                    "'2026-10-02T12:00:00Z')",
                "('three', 'GASTO', 300, 'USD', '2026-10-01', 'cena', NULL, " +
                    "'2026-10-01T12:00:00Z')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, titulo, " +
                      "descripcion, created_at) VALUES $values",
                  0,
              )
            }
        listOf(
                "('one', 'cat_sueldo', 0)",
                "('three', 'cat_social', 2)",
                "('three', 'cat_restaurantes', 0)",
                "('three', 'cat_comida', 1)",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento_categoria(movimiento_id, categoria_id, posicion) " +
                      "VALUES $values",
                  0,
              )
            }

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 5,
            newVersion = TallybookDatabase.Schema.version,
        )

        val migrated =
            TallybookDatabase(driver).movimientoQueries.selectAllMovimientos().executeAsList()
        assertEquals(
            mapOf("none" to "cat_otros", "one" to "cat_sueldo", "three" to "cat_restaurantes"),
            migrated.associate { it.id to it.categoria_id },
        )
        assertEquals(listOf("", "", ""), migrated.map { it.etiqueta_ids })
        val one = migrated.single { it.id == "one" }.toDomain()
        assertEquals(TipoMovimiento.INGRESO, one.tipo)
        assertEquals(Money(200, Moneda.COP), one.monto)
        assertNull(one.titulo)
        assertEquals("detalle", one.descripcion)
        assertEquals(emptyList(), one.etiquetaIds)
        assertEquals(Instant.parse("2026-10-02T12:00:00Z"), one.createdAt)
        assertEquals(emptyList(), driver.queryStrings("SELECT id FROM etiqueta"))
        assertEquals(
            emptyList(),
            driver.queryStrings("SELECT movimiento_id FROM movimiento_etiqueta"),
        )
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 4 moves the typed sentence into titulo and keeps every row`() =
      withSnapshot(version = 4) { driver ->
        listOf(
                "('phrase', 'GASTO', 2500, 'COP', '2026-10-02', 'Gasté 25 en cine', " +
                    "'2026-10-02T12:00:00Z')",
                "('manual', 'INGRESO', 900, 'COP', '2026-10-01', NULL, '2026-10-01T12:00:00Z')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, descripcion, " +
                      "created_at) VALUES $values",
                  0,
              )
            }
        listOf(
                "('phrase', 'cat_cine', 0)",
                "('phrase', 'cat_otros', 1)",
                "('manual', 'cat_sueldo', 0)",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento_categoria(movimiento_id, categoria_id, posicion) " +
                      "VALUES $values",
                  0,
              )
            }

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 4,
            newVersion = TallybookDatabase.Schema.version,
        )

        val migrated =
            TallybookDatabase(driver).movimientoQueries.selectAllMovimientos().executeAsList().map {
              it.toDomain()
            }
        assertEquals(listOf("phrase", "manual"), migrated.map { it.id })
        assertEquals(listOf("Gasté 25 en cine", null), migrated.map { it.titulo })
        assertEquals(listOf(null, null), migrated.map { it.descripcion })
        assertEquals(listOf("cat_cine", "cat_sueldo"), migrated.map { it.categoriaId })
        assertEquals(Instant.parse("2026-10-02T12:00:00Z"), migrated.first().createdAt)
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 3 keeps every row and gives categorias the seeded defaults`() =
      withSnapshot(version = 3) { driver ->
        listOf(
                "('cat_comida', 'Comida', NULL, 'utensils', 'amber')",
                "('cat_cafe', 'Café', 'cat_comida', 'coffee', 'amber')",
                "('cat_otros', 'Otros', NULL, 'circle-dashed', 'neutral')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO categoria(id, nombre, padre_id, icono, color) VALUES $values",
                  0,
              )
            }
        listOf(
                "('multi', 'GASTO', 2500, 'COP', '2026-10-02', 'almuerzo', '2026-10-02T12:00:00Z')",
                "('usd', 'INGRESO', 900, 'USD', '2026-10-01', NULL, '2026-10-01T12:00:00Z')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, descripcion, " +
                      "created_at) VALUES $values",
                  0,
              )
            }
        listOf("('multi', 'cat_cafe', 0)", "('multi', 'cat_otros', 1)", "('usd', 'cat_comida', 0)")
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento_categoria(movimiento_id, categoria_id, posicion) " +
                      "VALUES $values",
                  0,
              )
            }

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 3,
            newVersion = TallybookDatabase.Schema.version,
        )

        val database = TallybookDatabase(driver)
        assertEquals(
            listOf(
                Categoria("cat_comida", "Comida", null, "utensils", "amber", null, "sistema", 0),
                Categoria("cat_cafe", "Café", "cat_comida", "coffee", "amber", null, "sistema", 0),
                Categoria(
                    "cat_otros",
                    "Otros",
                    null,
                    "circle-dashed",
                    "neutral",
                    null,
                    "sistema",
                    0,
                ),
            ),
            database.categoriaQueries.selectAllCategorias().executeAsList(),
        )
        assertEquals(
            mapOf("multi" to "cat_cafe", "usd" to "cat_comida"),
            database.movimientoQueries.selectAllMovimientos().executeAsList().associate {
              it.id to it.toDomain().categoriaId
            },
        )
        assertEquals(freshSchema(), driver.schemaObjects())
        assertFailsWith<SQLException> {
          driver.execute(
              null,
              "INSERT INTO categoria(id, nombre, padre_id, icono, color, origen) " +
                  "VALUES ('x', 'X', NULL, 'tag', 'neutral', 'otro')",
              0,
          )
        }
        assertFailsWith<SQLException> {
          driver.execute(null, "UPDATE categoria SET oculta = 2 WHERE id = 'cat_comida'", 0)
        }
      }

  @Test
  fun `migration from schema 2 keeps each categoria and rebuilds movimiento`() =
      withSnapshot(version = 2) { driver ->
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

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 2,
            newVersion = TallybookDatabase.Schema.version,
        )

        val queries = TallybookDatabase(driver).movimientoQueries
        val migrated = queries.selectAllMovimientos().executeAsList().map { it.toDomain() }
        assertEquals(
            listOf("tied", "cafe", "sueldo", "orphan"),
            migrated.map { it.id },
            "fecha DESC, created_at DESC, id DESC order survives the rebuild",
        )
        assertEquals(
            mapOf(
                "cafe" to "cat_comida",
                "sueldo" to "cat_sueldo",
                "orphan" to "category-that-was-never-seeded",
                "tied" to "cat_comida",
            ),
            migrated.associate { it.id to it.categoriaId },
        )
        val cafe = migrated.single { it.id == "cafe" }
        assertEquals("almuerzo", cafe.titulo)
        assertNull(cafe.descripcion)
        assertEquals(1_000L, cafe.monto.minorUnits)
        assertEquals(Instant.parse("2026-09-03T12:00:00Z"), cafe.createdAt)
        assertEquals(
            listOf(
                "id",
                "tipo",
                "monto_minor",
                "monto_moneda",
                "fecha",
                "descripcion",
                "created_at",
                "titulo",
                "categoria_id",
            ),
            driver.queryStrings("SELECT name FROM pragma_table_info('movimiento')"),
        )
        assertEquals(
            listOf("movimiento_by_categoria", "movimiento_etiqueta_etiqueta", "movimiento_recent"),
            driver.queryStrings(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            ),
        )
        assertEquals(
            listOf("categoria_id"),
            driver.queryStrings("SELECT name FROM pragma_index_info('movimiento_by_categoria')"),
        )
        assertEquals(
            listOf("fecha", "created_at", "id"),
            driver.queryStrings("SELECT name FROM pragma_index_info('movimiento_recent')"),
        )
        assertEquals(
            listOf("movimiento_con_etiquetas"),
            driver.queryStrings("SELECT name FROM sqlite_master WHERE type = 'view'"),
        )
        assertEquals(
            listOf("categoria", "etiqueta", "movimiento", "movimiento_etiqueta"),
            driver.queryStrings(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name"
            ),
        )
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

  private fun withSnapshot(version: Int, block: (SqlDriver) -> Unit) {
    val snapshot = File("src/commonMain/sqldelight/databases/$version.db")
    assertTrue(snapshot.exists(), "schema $version snapshot not found at ${snapshot.absolutePath}")
    val file = File.createTempFile("tallybook-v$version", ".db")
    try {
      snapshot.copyTo(file, overwrite = true)
      val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
      try {
        block(driver)
      } finally {
        driver.close()
      }
    } finally {
      file.delete()
    }
  }

  private fun freshSchema(): List<String> {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      TallybookDatabase.Schema.create(driver)
      return driver.schemaObjects()
    } finally {
      driver.close()
    }
  }

  private fun SqlDriver.schemaObjects(): List<String> =
      queryStrings(
          "SELECT type || ' ' || name || ' ' || tbl_name || ' ' || COALESCE(sql, '') " +
              "FROM sqlite_master ORDER BY type, name"
      )

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
      etiquetaIds: List<String> = emptyList(),
      titulo: String? = null,
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
          titulo = titulo,
          descripcion = descripcion,
          created_at = createdAt,
          categoria_id = categoriaId,
      )
      etiquetaIds.forEachIndexed { posicion, etiquetaId ->
        movimientoQueries.insertMovimientoEtiqueta(id, etiquetaId, posicion.toLong())
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
        categoriaId = "cat_comida",
        etiquetaIds = listOf("tag_b", "tag_a"),
        titulo = "almuerzo",
        descripcion = "con Ana",
    )
    val row = db.movimientoQueries.selectMovimientoById("mov1").executeAsOne()

    assertEquals("GASTO", row.tipo)
    assertEquals(1050L, row.monto_minor)
    assertEquals("USD", row.monto_moneda)
    assertEquals("2026-09-03", row.fecha)
    assertEquals("cat_comida", row.toDomain().categoriaId)
    assertEquals(listOf("tag_b", "tag_a"), row.toDomain().etiquetaIds)
    assertEquals("almuerzo", row.titulo)
    assertEquals("con Ana", row.descripcion)
    assertEquals("2026-09-03T12:00:00Z", row.created_at)
  }

  @Test
  fun `titulo and descripcion are the only optional movimiento fields`() {
    val db = inMemoryDatabase()

    db.insert(
        id = "mov1",
        tipo = "INGRESO",
        montoMinor = 120_000,
        categoriaId = "cat_sueldo",
    )

    val row = db.movimientoQueries.selectMovimientoById("mov1").executeAsOne()
    assertNull(row.titulo)
    assertNull(row.descripcion)
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
        categoriaId = "cat_comida",
        fecha = "2026-09-05",
    )
    db.insert(
        id = "in2",
        montoMinor = 2_500,
        categoriaId = "cat_comida",
        fecha = "2026-09-20",
    )
    db.insert(
        id = "in3",
        tipo = "INGRESO",
        montoMinor = 700,
        categoriaId = "cat_comida",
        fecha = "2026-09-10",
    )
    db.insert(
        id = "in4",
        montoMinor = -300,
        categoriaId = "cat_transporte",
        fecha = "2026-09-11",
    )
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
    db.insert(
        id = "before",
        montoMinor = 9_999,
        categoriaId = "cat_comida",
        fecha = "2026-08-31",
    )
    db.insert(
        id = "after",
        montoMinor = 8_888,
        categoriaId = "cat_comida",
        fecha = "2026-10-01",
    )
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
  fun `sumMovimientosPorCategoria totals add up to the totals per tipo`() {
    val db = inMemoryDatabase()
    db.insert(id = "comida", montoMinor = 1_000, categoriaId = "cat_comida")
    db.insert(id = "social", montoMinor = 500, categoriaId = "cat_social")

    val perCategoria =
        db.movimientoQueries
            .sumMovimientosPorCategoria("COP", "2026-09-01", "2026-09-30")
            .executeAsList()
            .associate { (it.categoria_id to it.tipo) to it.total_minor }

    assertEquals(
        mapOf(("cat_comida" to "GASTO") to 1_000L, ("cat_social" to "GASTO") to 500L),
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
