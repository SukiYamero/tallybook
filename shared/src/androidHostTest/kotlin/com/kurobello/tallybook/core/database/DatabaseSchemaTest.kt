package com.kurobello.tallybook.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.seedCashCuentas
import com.kurobello.tallybook.core.data.toDomain
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import java.io.File
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class DatabaseSchemaTest {

  @Test
  fun `migration from schema 7 assigns one cash account per currency and keeps every row`() =
      withSnapshot(version = 7) { driver ->
        listOf(
                "('cop_gasto', 'GASTO', 1000, 'COP', '2026-01-01T10:00:00.5Z', " +
                    "'1767261600500-0001-0123456789abcdef', NULL)",
                "('cop_first', 'GASTO', 2500, 'COP', '2026-01-01T10:00:00Z', " +
                    "'1767261600000-0000-0000000000000000', NULL)",
                "('cop_ingreso', 'INGRESO', 9000, 'COP', '2026-03-01T08:00:00Z', " +
                    "'1772352000000-0002-0123456789abcdef', NULL)",
                "('usd_gasto', 'GASTO', 1250, 'USD', '2026-02-01T00:00:00Z', " +
                    "'1769904000000-0000-0123456789abcdef', NULL)",
                "('clp_deleted', 'GASTO', 900, 'CLP', '2025-12-31T23:59:59Z', " +
                    "'1772352000000-0003-0123456789abcdef', " +
                    "'1772352000000-0003-0123456789abcdef')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, created_at, " +
                      "updated_at, deleted_at, fecha, categoria_id) " +
                      "SELECT column1, column2, column3, column4, column5, column6, column7, " +
                      "'2026-01-15', 'cat_comida' FROM (VALUES $values)",
                  0,
              )
            }
        driver.execute(
            null,
            "INSERT INTO etiqueta(id, nombre, created_at, updated_at) VALUES " +
                "('tag_a', 'Viaje', '2026-01-01T00:00:00Z', '${ChangeStamp.SEED.value}'), " +
                "('tag_b', 'Casa', '2026-01-01T00:00:00Z', '${ChangeStamp.SEED.value}')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO movimiento_etiqueta(movimiento_id, etiqueta_id, posicion) VALUES " +
                "('cop_gasto', 'tag_b', 0), ('cop_gasto', 'tag_a', 1), ('usd_gasto', 'tag_a', 0)",
            0,
        )
        val sumsBefore =
            driver.queryStrings(
                "SELECT monto_moneda || ' ' || tipo || ' ' || SUM(monto_minor) FROM movimiento " +
                    "WHERE deleted_at IS NULL GROUP BY monto_moneda, tipo ORDER BY 1"
            )
        val stampsBefore = driver.movimientoStamps()

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 7,
            newVersion = TallybookDatabase.Schema.version,
        )

        val seed = ChangeStamp.SEED.value
        assertEquals(
            listOf(
                "cuenta_efectivo_clp Efectivo EFECTIVO CLP 0 - 0 2025-12-31T23:59:59Z $seed $seed",
                "cuenta_efectivo_cop Efectivo EFECTIVO COP 0 - 0 2026-01-01T10:00:00Z $seed -",
                "cuenta_efectivo_usd Efectivo EFECTIVO USD 0 - 0 2026-02-01T00:00:00Z $seed -",
            ),
            driver.queryStrings(
                "SELECT id || ' ' || nombre || ' ' || tipo || ' ' || moneda || ' ' || " +
                    "saldo_inicial_minor || ' ' || COALESCE(dia_corte, '-') || ' ' || " +
                    "archivada || ' ' || created_at || ' ' || updated_at || ' ' || " +
                    "COALESCE(deleted_at, '-') FROM cuenta ORDER BY id"
            ),
        )
        assertEquals(
            listOf(
                "clp_deleted cuenta_efectivo_clp EFECTIVO",
                "cop_first cuenta_efectivo_cop EFECTIVO",
                "cop_gasto cuenta_efectivo_cop EFECTIVO",
                "cop_ingreso cuenta_efectivo_cop EFECTIVO",
                "usd_gasto cuenta_efectivo_usd EFECTIVO",
            ),
            driver.queryStrings(
                "SELECT id || ' ' || cuenta_id || ' ' || medio_pago FROM movimiento ORDER BY id"
            ),
        )
        assertEquals(stampsBefore, driver.movimientoStamps())
        val database = TallybookDatabase(driver)
        assertEquals(
            mapOf(
                "cop_first" to "COP",
                "cop_gasto" to "COP",
                "cop_ingreso" to "COP",
                "usd_gasto" to "USD",
            ),
            database.movimientoQueries.selectAllMovimientos().executeAsList().associate {
              it.id to it.monto_moneda
            },
        )
        assertEquals(
            sumsBefore,
            listOf("CLP", "COP", "USD")
                .flatMap { moneda ->
                  database.movimientoQueries.totalsByTipo(moneda).map { (tipo, total) ->
                    "$moneda $tipo $total"
                  }
                }
                .sorted(),
        )
        assertEquals(
            listOf("cop_gasto tag_a 1", "cop_gasto tag_b 0", "usd_gasto tag_a 0"),
            driver.queryStrings(
                "SELECT movimiento_id || ' ' || etiqueta_id || ' ' || posicion " +
                    "FROM movimiento_etiqueta ORDER BY 1"
            ),
        )
        val clpSnapshot =
            Movimiento(
                id = "clp_deleted",
                tipo = TipoMovimiento.GASTO,
                monto = Money(900, Moneda.CLP),
                fecha = LocalDate(2026, 1, 15),
                categoriaId = "cat_comida",
                titulo = null,
                descripcion = null,
                createdAt = Instant.parse("2025-12-31T23:59:59Z"),
                cuentaId = Cuenta.defaultCashId(Moneda.CLP),
                medio = MedioPago.EFECTIVO,
            )
        assertEquals(
            DataOutcome.Success(RestoreResult.MissingCuenta),
            runBlocking {
              MovimientoRepositoryImpl(database, Dispatchers.Unconfined, TestChangeClock())
                  .restoreDeleted(clpSnapshot)
            },
        )
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 8 adds the live transfer view and keeps every transfer`() =
      withSnapshot(version = 8) { driver ->
        val seed = ChangeStamp.SEED.value
        driver.execute(
            null,
            "INSERT INTO cuenta(id, nombre, tipo, moneda, created_at, updated_at) VALUES " +
                "('cop', 'Efectivo', 'EFECTIVO', 'COP', '2026-01-01T00:00:00Z', '$seed'), " +
                "('usd', 'Chase', 'BANCO', 'USD', '2026-01-01T00:00:00Z', '$seed')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO transferencia(id, cuenta_origen_id, cuenta_destino_id, " +
                "monto_origen_minor, monto_destino_minor, fecha, created_at, updated_at, " +
                "deleted_at) VALUES " +
                "('live', 'cop', 'usd', 400000, 10000, '2026-02-01', '2026-02-01T00:00:00Z', " +
                "'$seed', NULL), " +
                "('gone', 'usd', 'cop', 100, 4000, '2026-02-02', '2026-02-02T00:00:00Z', " +
                "'$seed', '$seed')",
            0,
        )

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 8,
            newVersion = TallybookDatabase.Schema.version,
        )

        assertEquals(
            listOf("live cop 400000 COP usd 10000 USD"),
            driver.queryStrings(
                "SELECT id || ' ' || cuenta_origen_id || ' ' || monto_origen_minor || ' ' || " +
                    "moneda_origen || ' ' || cuenta_destino_id || ' ' || " +
                    "monto_destino_minor || ' ' || moneda_destino FROM transferencia_con_monedas"
            ),
        )
        assertEquals(listOf("gone", "live"), driver.queryStrings("SELECT id FROM transferencia"))
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 9 colors every cuenta by its type and keeps its other fields`() =
      withSnapshot(version = 9) { driver ->
        val seed = ChangeStamp.SEED.value
        val stamp = "1772352000000-0003-0123456789abcdef"
        driver.execute(
            null,
            "INSERT INTO cuenta(id, nombre, tipo, moneda, saldo_inicial_minor, dia_corte, " +
                "archivada, created_at, updated_at, deleted_at) VALUES " +
                "('cash', 'Efectivo', 'EFECTIVO', 'COP', 0, NULL, 0, " +
                "'2026-01-01T00:00:00Z', '$seed', NULL), " +
                "('nequi', 'Nequi', 'BANCO', 'COP', 150000, NULL, 1, " +
                "'2026-01-02T00:00:00Z', '$stamp', NULL), " +
                "('nu', 'NU', 'CREDITO', 'COP', -50000, 15, 0, " +
                "'2026-01-03T00:00:00Z', '$stamp', NULL), " +
                "('gone', 'Chase', 'BANCO', 'USD', 0, NULL, 0, " +
                "'2026-01-04T00:00:00Z', '$stamp', '$stamp')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO movimiento(id, tipo, monto_minor, fecha, created_at, categoria_id, " +
                "cuenta_id, medio_pago, updated_at) VALUES ('mov', 'GASTO', 1000, " +
                "'2026-02-01', '2026-02-01T00:00:00Z', 'cat_comida', 'nu', 'CREDITO', '$seed')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO transferencia(id, cuenta_origen_id, cuenta_destino_id, " +
                "monto_origen_minor, monto_destino_minor, fecha, created_at, updated_at) " +
                "VALUES ('tr', 'nequi', 'cash', 500, 500, '2026-02-02', " +
                "'2026-02-02T00:00:00Z', '$seed')",
            0,
        )
        val fieldsBefore = driver.cuentaFields()

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 9,
            newVersion = TallybookDatabase.Schema.version,
        )

        assertEquals(fieldsBefore, driver.cuentaFields())
        assertEquals(
            listOf("cash emerald", "gone blue", "nequi blue", "nu purple"),
            driver.queryStrings("SELECT id || ' ' || color FROM cuenta ORDER BY id"),
        )
        assertEquals(
            listOf("mov COP"),
            driver.queryStrings("SELECT id || ' ' || monto_moneda FROM movimiento_con_etiquetas"),
        )
        assertEquals(
            listOf("tr COP COP"),
            driver.queryStrings(
                "SELECT id || ' ' || moneda_origen || ' ' || moneda_destino " +
                    "FROM transferencia_con_monedas"
            ),
        )
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 10 adds recurrencias and keeps every movimiento unlinked`() =
      withSnapshot(version = 10) { driver ->
        val seed = ChangeStamp.SEED.value
        val stamp = "1772352000000-0003-0123456789abcdef"
        driver.execute(
            null,
            "INSERT INTO cuenta(id, nombre, tipo, moneda, color, created_at, updated_at) VALUES " +
                "('cash', 'Efectivo', 'EFECTIVO', 'COP', 'emerald', '2026-01-01T00:00:00Z', " +
                "'$seed')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO movimiento(id, tipo, monto_minor, fecha, descripcion, created_at, " +
                "titulo, categoria_id, cuenta_id, medio_pago, updated_at, deleted_at) VALUES " +
                "('live', 'GASTO', 1000, '2026-02-01', 'nota', '2026-02-01T00:00:00Z', 'pan', " +
                "'cat_comida', 'cash', 'EFECTIVO', '$stamp', NULL), " +
                "('gone', 'INGRESO', 500, '2026-02-02', NULL, '2026-02-02T00:00:00Z', NULL, " +
                "'cat_ingresos', 'cash', 'EFECTIVO', '$stamp', '$stamp')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO movimiento_etiqueta(movimiento_id, etiqueta_id, posicion) " +
                "VALUES ('live', 'tag', 0)",
            0,
        )
        val fieldsBefore = driver.movimientoFields()

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 10,
            newVersion = TallybookDatabase.Schema.version,
        )

        assertEquals(fieldsBefore, driver.movimientoFields())
        assertEquals(
            listOf("gone -", "live -"),
            driver.queryStrings(
                "SELECT id || ' ' || COALESCE(recurrencia_id, '-') FROM movimiento ORDER BY id"
            ),
        )
        assertEquals(
            listOf("live COP 0\u001Ftag"),
            driver.queryStrings(
                "SELECT id || ' ' || monto_moneda || ' ' || etiqueta_ids " +
                    "FROM movimiento_con_etiquetas"
            ),
        )
        assertEquals(
            listOf("0"),
            driver.queryStrings(
                "SELECT (SELECT COUNT(*) FROM recurrencia) + " +
                    "(SELECT COUNT(*) FROM recurrencia_etiqueta)"
            ),
        )
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 11 adds the empty correccion_frase table`() =
      withSnapshot(version = 11) { driver ->
        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 11,
            newVersion = TallybookDatabase.Schema.version,
        )

        assertEquals(listOf("0"), driver.queryStrings("SELECT COUNT(*) FROM correccion_frase"))
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `migration from schema 12 adds the empty presupuesto table`() =
      withSnapshot(version = 12) { driver ->
        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 12,
            newVersion = TallybookDatabase.Schema.version,
        )

        assertEquals(listOf("0"), driver.queryStrings("SELECT COUNT(*) FROM presupuesto"))
        assertEquals(freshSchema(), driver.schemaObjects())
      }

  @Test
  fun `fresh schema rejects invalid cuenta and transferencia rows`() {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val seed = ChangeStamp.SEED.value
      fun insertCuenta(id: String, tipo: String, diaCorte: String) =
          driver.execute(
              null,
              "INSERT INTO cuenta(id, nombre, tipo, moneda, dia_corte, color, created_at, " +
                  "updated_at) VALUES ('$id', '$id', '$tipo', 'COP', $diaCorte, 'blue', " +
                  "'2026-01-01T00:00:00Z', '$seed')",
              0,
          )
      fun insertTransferencia(id: String, destino: String, origenMinor: Long, destinoMinor: Long) =
          driver.execute(
              null,
              "INSERT INTO transferencia(id, cuenta_origen_id, cuenta_destino_id, " +
                  "monto_origen_minor, monto_destino_minor, fecha, created_at, updated_at) " +
                  "VALUES ('$id', 'nequi', '$destino', $origenMinor, $destinoMinor, " +
                  "'2026-01-02', '2026-01-02T00:00:00Z', '$seed')",
              0,
          )

      insertCuenta("nequi", "BANCO", "NULL")
      insertCuenta("nu", "CREDITO", "31")
      insertCuenta("nu_first_day", "CREDITO", "1")
      insertTransferencia("valid", destino = "nu", origenMinor = 100, destinoMinor = 100)

      assertFailsWith<SQLException> { insertCuenta("unknown_tipo", "AHORRO", "NULL") }
      assertFailsWith<SQLException> { insertCuenta("dia_zero", "CREDITO", "0") }
      assertFailsWith<SQLException> { insertCuenta("dia_32", "CREDITO", "32") }
      assertFailsWith<SQLException> {
        driver.execute(
            null,
            "INSERT INTO cuenta(id, nombre, tipo, moneda, created_at, updated_at) VALUES " +
                "('no_color', 'no_color', 'BANCO', 'COP', '2026-01-01T00:00:00Z', '$seed')",
            0,
        )
      }
      assertFailsWith<SQLException> {
        insertTransferencia("zero_origen", destino = "nu", origenMinor = 0, destinoMinor = 100)
      }
      assertFailsWith<SQLException> {
        insertTransferencia(
            "negative_destino",
            destino = "nu",
            origenMinor = 100,
            destinoMinor = -1,
        )
      }
      assertFailsWith<SQLException> {
        insertTransferencia(
            "same_account",
            destino = "nequi",
            origenMinor = 100,
            destinoMinor = 100,
        )
      }
      assertEquals(
          listOf("nequi", "nu", "nu_first_day"),
          driver.queryStrings("SELECT id FROM cuenta ORDER BY id"),
      )
      assertEquals(listOf("valid"), driver.queryStrings("SELECT id FROM transferencia"))
    }
  }

  @Test
  fun `migration from schema 6 backfills change stamps and keeps every row`() =
      withSnapshot(version = 6) { driver ->
        listOf(
                "('plain', '2026-10-02T12:00:00Z')",
                "('nanos', '2026-10-02T12:00:00.123456789Z')",
                "('pre_1970', '1969-07-20T20:17:40Z')",
                "('unparseable_year', '+10000-01-01T00:00:00Z')",
                "('beyond_stamp_range', '2500-01-01T00:00:00Z')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO movimiento(id, created_at, tipo, monto_minor, monto_moneda, fecha, " +
                      "titulo, categoria_id) SELECT column1, column2, 'GASTO', 1000, 'COP', " +
                      "'2026-10-01', 'almuerzo', 'cat_comida' FROM (VALUES $values)",
                  0,
              )
            }
        driver.execute(
            null,
            "INSERT INTO movimiento(id, tipo, monto_minor, monto_moneda, fecha, categoria_id) " +
                "VALUES ('legacy_default', 'INGRESO', 500, 'USD', '2026-09-30', 'cat_sueldo')",
            0,
        )
        listOf(
                "('tag_a', 'Viaje', 'con Ana', '2026-09-01T08:30:00.5Z')",
                "('tag_b', 'Casa', NULL, '1970-01-01T00:00:00Z')",
            )
            .forEach { values ->
              driver.execute(
                  null,
                  "INSERT INTO etiqueta(id, nombre, descripcion, created_at) VALUES $values",
                  0,
              )
            }
        listOf("('plain', 'tag_b', 0)", "('plain', 'tag_a', 1)", "('nanos', 'tag_a', 0)").forEach {
            values ->
          driver.execute(
              null,
              "INSERT INTO movimiento_etiqueta(movimiento_id, etiqueta_id, posicion) " +
                  "VALUES $values",
              0,
          )
        }
        driver.execute(
            null,
            "INSERT INTO categoria(id, nombre, padre_id, icono, color) " +
                "VALUES ('cat_comida', 'Comida', NULL, 'utensils', 'amber')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO categoria(id, nombre, padre_id, icono, color, descripcion, origen, " +
                "oculta) VALUES ('custom', 'Mascotas', NULL, 'tag', 'neutral', 'gato', " +
                "'usuario', 1)",
            0,
        )

        TallybookDatabase.Schema.migrate(
            driver,
            oldVersion = 6,
            newVersion = TallybookDatabase.Schema.version,
        )

        val seed = ChangeStamp.SEED.value
        assertEquals(
            listOf(
                "beyond_stamp_range $seed",
                "legacy_default $seed",
                "nanos 1790942400123-0000-0000000000000000",
                "plain 1790942400000-0000-0000000000000000",
                "pre_1970 $seed",
                "unparseable_year $seed",
            ),
            driver.stampsOf("movimiento"),
        )
        assertEquals(
            listOf("tag_a 1788251400500-0000-0000000000000000", "tag_b $seed"),
            driver.stampsOf("etiqueta"),
        )
        assertEquals(listOf("cat_comida $seed", "custom $seed"), driver.stampsOf("categoria"))
        listOf("movimiento", "etiqueta", "categoria").forEach { table ->
          driver.queryStrings("SELECT updated_at FROM $table").forEach(ChangeStamp::parse)
          assertEquals(
              emptyList(),
              driver.queryStrings("SELECT id FROM $table WHERE deleted_at IS NOT NULL"),
          )
        }
        val database = TallybookDatabase(driver)
        val migrated =
            database.movimientoQueries.selectAllMovimientos().executeAsList().associateBy { it.id }
        assertEquals(6, migrated.size)
        assertEquals(listOf("tag_b", "tag_a"), migrated.getValue("plain").toDomain().etiquetaIds)
        assertEquals(listOf("tag_a"), migrated.getValue("nanos").toDomain().etiquetaIds)
        assertEquals(
            Instant.parse("2026-10-02T12:00:00.123456789Z"),
            migrated.getValue("nanos").toDomain().createdAt,
        )
        val legacy = migrated.getValue("legacy_default").toDomain()
        assertEquals(Money(500, Moneda.USD), legacy.monto)
        assertEquals(Instant.parse("1970-01-01T00:00:00Z"), legacy.createdAt)
        assertEquals(
            listOf("tag_a Viaje con Ana", "tag_b Casa -"),
            driver.queryStrings(
                "SELECT id || ' ' || nombre || ' ' || COALESCE(descripcion, '-') FROM etiqueta " +
                    "ORDER BY id"
            ),
        )
        assertEquals(
            listOf("cat_comida sistema 0 -", "custom usuario 1 gato"),
            driver.queryStrings(
                "SELECT id || ' ' || origen || ' ' || oculta || ' ' || COALESCE(descripcion, '-') " +
                    "FROM categoria ORDER BY id"
            ),
        )
        assertEquals(freshSchema(), driver.schemaObjects())
      }

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
            listOf("id", "nombre", "descripcion", "created_at", "updated_at", "deleted_at"),
            driver.queryStrings("SELECT name FROM pragma_table_info('etiqueta')"),
        )
        assertEquals(
            listOf("descripcion", "deleted_at"),
            driver.queryStrings(
                "SELECT name FROM pragma_table_info('etiqueta') WHERE \"notnull\" = 0"
            ),
        )
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
        val seed = ChangeStamp.SEED.value
        assertEquals(
            listOf(
                Categoria(
                    "cat_comida",
                    "Comida",
                    null,
                    "utensils",
                    "amber",
                    null,
                    "sistema",
                    0,
                    seed,
                    null,
                ),
                Categoria(
                    "cat_cafe",
                    "Café",
                    "cat_comida",
                    "coffee",
                    "amber",
                    null,
                    "sistema",
                    0,
                    seed,
                    null,
                ),
                Categoria(
                    "cat_otros",
                    "Otros",
                    null,
                    "circle-dashed",
                    "neutral",
                    null,
                    "sistema",
                    0,
                    seed,
                    null,
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
                "fecha",
                "descripcion",
                "created_at",
                "titulo",
                "categoria_id",
                "cuenta_id",
                "medio_pago",
                "recurrencia_id",
                "updated_at",
                "deleted_at",
            ),
            driver.queryStrings("SELECT name FROM pragma_table_info('movimiento')"),
        )
        assertEquals(
            listOf(
                "correccion_frase_recent",
                "movimiento_by_categoria",
                "movimiento_by_cuenta",
                "movimiento_etiqueta_etiqueta",
                "movimiento_recent",
                "presupuesto_categoria",
                "transferencia_by_destino",
                "transferencia_by_origen",
                "transferencia_recent",
            ),
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
            listOf(
                "movimiento_con_etiquetas",
                "recurrencia_con_etiquetas",
                "transferencia_con_monedas",
            ),
            driver.queryStrings("SELECT name FROM sqlite_master WHERE type = 'view' ORDER BY name"),
        )
        assertEquals(
            listOf(
                "categoria",
                "correccion_frase",
                "cuenta",
                "etiqueta",
                "movimiento",
                "movimiento_etiqueta",
                "presupuesto",
                "recurrencia",
                "recurrencia_etiqueta",
                "transferencia",
            ),
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

  private fun MovimientoQueries.totalsByTipo(moneda: String): Map<String, Long?> =
      sumMovimientosByTipoInRange(moneda, "0000-01-01", "9999-12-31").executeAsList().associate {
        it.tipo to it.total_minor
      }

  private fun SqlDriver.stampsOf(table: String): List<String> =
      queryStrings("SELECT id || ' ' || updated_at FROM $table ORDER BY id")

  private fun SqlDriver.cuentaFields(): List<String> =
      queryStrings(
          "SELECT id || ' ' || nombre || ' ' || tipo || ' ' || moneda || ' ' || " +
              "saldo_inicial_minor || ' ' || COALESCE(dia_corte, '-') || ' ' || archivada || ' ' || " +
              "created_at || ' ' || updated_at || ' ' || COALESCE(deleted_at, '-') FROM cuenta " +
              "ORDER BY id"
      )

  private fun SqlDriver.movimientoFields(): List<String> =
      queryStrings(
          "SELECT id || ' ' || tipo || ' ' || monto_minor || ' ' || fecha || ' ' || " +
              "COALESCE(descripcion, '-') || ' ' || created_at || ' ' || COALESCE(titulo, '-') || " +
              "' ' || categoria_id || ' ' || cuenta_id || ' ' || medio_pago || ' ' || updated_at || " +
              "' ' || COALESCE(deleted_at, '-') FROM movimiento ORDER BY id"
      )

  private fun SqlDriver.movimientoStamps(): List<String> =
      queryStrings(
          "SELECT id || ' ' || updated_at || ' ' || COALESCE(deleted_at, '-') FROM movimiento " +
              "ORDER BY id"
      )

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
    return TallybookDatabase(driver).apply { seedCashCuentas() }
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
          fecha = fecha,
          titulo = titulo,
          descripcion = descripcion,
          created_at = createdAt,
          categoria_id = categoriaId,
          cuenta_id = Cuenta.defaultCashId(Moneda.valueOf(moneda)),
          medio_pago = MedioPago.EFECTIVO.name,
          updated_at = ChangeStamp.SEED.value,
          recurrencia_id = null,
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
  fun `selectRecentMovimientosByMoneda uses fecha then created at then id and obeys limit`() {
    val db = inMemoryDatabase()
    db.insert(id = "a", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T12:00:00Z")
    db.insert(id = "c", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T12:00:00Z")
    db.insert(id = "b", montoMinor = 100, fecha = "2026-09-03", createdAt = "2026-09-03T11:00:00Z")
    db.insert(id = "d", montoMinor = 100, fecha = "2026-09-02", createdAt = "2026-09-04T12:00:00Z")

    assertEquals(
        listOf("c", "a", "b"),
        db.movimientoQueries.selectRecentMovimientosByMoneda("COP", 3).executeAsList().map {
          it.id
        },
    )
  }

  @Test
  fun `sumMovimientosByTipoInRange filters by moneda before summing`() {
    val db = inMemoryDatabase()
    db.insert(id = "gasto", montoMinor = 500, moneda = "COP")
    db.insert(id = "ingreso", tipo = "INGRESO", montoMinor = 2_000, moneda = "COP")
    db.insert(id = "other", tipo = "INGRESO", montoMinor = 9_999, moneda = "USD")

    assertEquals(
        mapOf("GASTO" to 500L, "INGRESO" to 2_000L),
        db.movimientoQueries.totalsByTipo("COP"),
    )
  }

  @Test
  fun `tombstoneMovimiento hides the row and keeps it stored with the stamp`() {
    val db = inMemoryDatabase()
    db.insert(id = "mov1", montoMinor = 100)
    val stamp = ChangeStamp.of(epochMillis = 1, counter = 0, node = "00000000000000aa").value

    assertEquals(1L, db.movimientoQueries.tombstoneMovimiento(stamp, "mov1").value)
    assertEquals(0L, db.movimientoQueries.tombstoneMovimiento(stamp, "mov1").value)

    assertNull(db.movimientoQueries.selectMovimientoById("mov1").executeAsOneOrNull())
    val stored = db.movimientoQueries.selectStoredMovimientoById("mov1").executeAsOne()
    assertEquals(stamp to stamp, stored.updated_at to stored.deleted_at)
    assertEquals(100L, stored.monto_minor)
  }

  @Test
  fun `sumGastoPorCategoriaRaiz totals gasto per root categoria inside the range`() {
    val db = inMemoryDatabase()
    db.categoriaQueries.insertCategoria(
        "cat_cafe",
        "Cafe",
        "cat_comida",
        "coffee",
        "amber",
        ChangeStamp.SEED.value,
    )
    db.insert(
        id = "child",
        montoMinor = 600,
        categoriaId = "cat_cafe",
        fecha = "2026-09-12",
    )
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
            .sumGastoPorCategoriaRaiz("COP", "2026-09-01", "2026-09-30")
            .executeAsList()
            .associate { it.root_id to it.total_minor }

    assertEquals(mapOf("cat_comida" to 4_600L, "cat_transporte" to 2_700L), totals)
  }

  @Test
  fun `sumGastoPorCategoriaRaiz totals add up to the gasto total`() {
    val db = inMemoryDatabase()
    db.categoriaQueries.insertCategoria(
        "cat_comida",
        "Comida",
        null,
        "utensils",
        "amber",
        ChangeStamp.SEED.value,
    )
    db.insert(id = "comida", montoMinor = 1_000, categoriaId = "cat_comida")
    db.insert(id = "unknown", montoMinor = 500, categoriaId = "cat_missing")
    db.insert(id = "ingreso", tipo = "INGRESO", montoMinor = 9_000, categoriaId = "cat_comida")

    val perRoot =
        db.movimientoQueries
            .sumGastoPorCategoriaRaiz("COP", "2026-09-01", "2026-09-30")
            .executeAsList()
            .associate { it.root_id to it.total_minor }

    assertEquals(mapOf("cat_comida" to 1_000L, "cat_missing" to 500L), perRoot)
    assertEquals(
        db.movimientoQueries.totalsByTipo("COP").getValue("GASTO"),
        perRoot.values.sumOf { checkNotNull(it) },
    )
  }

  @Test
  fun `a categoria hija keeps its padre_id while a categoria padre has none`() {
    val db = inMemoryDatabase()

    db.categoriaQueries.insertCategoria(
        "cat_ingresos",
        "Ingresos",
        null,
        "trending-up",
        "success",
        ChangeStamp.SEED.value,
    )
    db.categoriaQueries.insertCategoria(
        "cat_sueldo",
        "Sueldo",
        "cat_ingresos",
        "wallet",
        "emerald",
        ChangeStamp.SEED.value,
    )

    val byId = db.categoriaQueries.selectAllCategorias().executeAsList().associateBy { it.id }
    assertNull(byId.getValue("cat_ingresos").padre_id)
    assertEquals("cat_ingresos", byId.getValue("cat_sueldo").padre_id)
    assertEquals("trending-up", byId.getValue("cat_ingresos").icono)
    assertEquals("emerald", byId.getValue("cat_sueldo").color)
  }

  @Test
  fun `reads through the movimiento view skip tombstones`() {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val db = TallybookDatabase(driver).apply { seedCashCuentas() }
      db.insert(id = "live", montoMinor = 100)
      db.insert(id = "tombstone", montoMinor = 200)

      driver.execute(
          null,
          "UPDATE movimiento SET deleted_at = updated_at WHERE id = 'tombstone'",
          0,
      )

      assertEquals(
          listOf("live"),
          db.movimientoQueries.selectAllMovimientos().executeAsList().map { it.id },
      )
      assertNull(db.movimientoQueries.selectMovimientoById("tombstone").executeAsOneOrNull())
    }
  }

  @Test
  fun `direct table reads counts and aggregates skip tombstones`() {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val db = TallybookDatabase(driver).apply { seedCashCuentas() }
      for (id in listOf("cat_live", "cat_gone")) {
        db.categoriaQueries.insertCategoria(id, id, null, "tag", "neutral", ChangeStamp.SEED.value)
      }
      for (id in listOf("tag_live", "tag_gone")) {
        db.etiquetaQueries.insertEtiqueta(
            id,
            id,
            null,
            "2026-01-01T00:00:00Z",
            ChangeStamp.SEED.value,
        )
      }
      db.insert(
          id = "live",
          montoMinor = 100,
          categoriaId = "cat_live",
          etiquetaIds = listOf("tag_live"),
          titulo = "cafe",
      )
      db.insert(
          id = "gone",
          tipo = "INGRESO",
          montoMinor = 900,
          fecha = "2024-09-03",
          categoriaId = "cat_live",
          etiquetaIds = listOf("tag_live"),
          titulo = "taxi",
          createdAt = "2026-09-04T12:00:00Z",
      )
      for ((table, id) in
          listOf("movimiento" to "gone", "categoria" to "cat_gone", "etiqueta" to "tag_gone")) {
        driver.execute(null, "UPDATE $table SET deleted_at = updated_at WHERE id = '$id'", 0)
      }
      val movimientos = db.movimientoQueries
      val categorias = db.categoriaQueries
      val etiquetas = db.etiquetaQueries

      assertEquals(
          listOf("2026"),
          db.transferenciaQueries.selectActividadYears("COP").executeAsList(),
      )
      assertEquals(
          listOf("cafe"),
          movimientos.selectCategoryHistory(10).executeAsList().map { it.titulo },
      )
      assertEquals(
          mapOf("GASTO" to 100L),
          movimientos.totalsByTipo("COP"),
      )
      db.insert(
          id = "gone_gasto",
          montoMinor = 700,
          categoriaId = "cat_live",
          etiquetaIds = listOf("tag_live"),
      )
      db.insert(id = "gone_usd", montoMinor = 300, moneda = "USD")
      driver.execute(
          null,
          "UPDATE movimiento SET deleted_at = updated_at WHERE id IN ('gone_gasto', 'gone_usd')",
          0,
      )
      assertTrue(movimientos.existsMovimientoEnMoneda("COP").executeAsOne())
      assertFalse(movimientos.existsMovimientoEnMoneda("USD").executeAsOne())
      assertEquals(
          listOf("cat_live" to 100L),
          movimientos
              .sumGastoPorCategoriaRaiz("COP", "2024-01-01", "2026-12-31")
              .executeAsList()
              .map { it.root_id to it.total_minor },
      )
      assertEquals(
          listOf("tag_live" to 100L),
          movimientos.sumGastoPorEtiqueta("COP", "2024-01-01", "2026-12-31").executeAsList().map {
            it.etiqueta_id to it.total_minor
          },
      )
      assertEquals(
          1L,
          movimientos
              .countGastoCategoria("COP", "2024-01-01", "2026-12-31", "cat_live")
              .executeAsOne(),
      )
      assertEquals(1L, categorias.countMovimientosForCategoria("cat_live").executeAsOne())
      assertEquals(
          mapOf("cat_live" to 1L),
          categorias.countMovimientosPorCategoria("COP").executeAsList().associate {
            it.categoria_id to it.movimiento_count
          },
      )
      assertEquals(1L, etiquetas.countMovimientosForEtiqueta("tag_live").executeAsOne())
      assertEquals(
          mapOf("tag_live" to 1L),
          etiquetas.countMovimientosPorEtiqueta("COP").executeAsList().associate {
            it.etiqueta_id to it.movimiento_count
          },
      )
      assertEquals(
          listOf("cat_live"),
          categorias.selectAllCategorias().executeAsList().map { it.id },
      )
      assertNull(categorias.selectCategoriaById("cat_gone").executeAsOneOrNull())
      assertEquals(listOf("tag_live"), etiquetas.selectAllEtiquetas().executeAsList().map { it.id })
      assertNull(etiquetas.selectEtiquetaById("tag_gone").executeAsOneOrNull())
      assertEquals(
          listOf("tag_live"),
          etiquetas.selectExistingEtiquetaIds(listOf("tag_live", "tag_gone")).executeAsList(),
      )
    }
  }

  @Test
  fun `live filtered counts and aggregates search the partial indexes`() {
    val driver = PlanRecordingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      TallybookDatabase.Schema.create(driver)
      val db = TallybookDatabase(driver)
      val expected =
          mapOf<String, Pair<() -> Any, String>>(
              "sumMovimientosByTipoInRange" to
                  ({
                    db.movimientoQueries
                        .sumMovimientosByTipoInRange("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorCategoriaRaiz" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorCategoriaRaiz("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorSubcategoria" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorSubcategoria("COP", "2026-09-01", "2026-09-30", "cat")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoCategoriaPorMes" to
                  ({
                    db.movimientoQueries
                        .sumGastoCategoriaPorMes("COP", "2026-09-01", "2026-09-30", "cat")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "countGastoCategoria" to
                  ({
                    db.movimientoQueries
                        .countGastoCategoria("COP", "2026-09-01", "2026-09-30", "cat")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "selectGastosMasGrandesCategoria" to
                  ({
                    db.movimientoQueries
                        .selectGastosMasGrandesCategoria(
                            "COP",
                            "2026-09-01",
                            "2026-09-30",
                            "cat",
                            3,
                        )
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorEtiqueta" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorEtiqueta("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorCuenta" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorCuenta("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorMedio" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorMedio("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumPorMes" to
                  ({
                    db.movimientoQueries
                        .sumPorMes("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "sumGastoPorDia" to
                  ({
                    db.movimientoQueries
                        .sumGastoPorDia("COP", "2026-09-01", "2026-09-30")
                        .executeAsList()
                  } to "SEARCH movimiento USING INDEX movimiento_recent (fecha>? AND fecha<?)"),
              "countMovimientosForCategoria" to
                  ({
                    db.categoriaQueries.countMovimientosForCategoria("cat").executeAsOne()
                  } to "SEARCH movimiento USING INDEX movimiento_by_categoria (categoria_id=?)"),
              "countMovimientosForEtiqueta" to
                  ({
                    db.etiquetaQueries.countMovimientosForEtiqueta("tag").executeAsOne()
                  } to
                      "SEARCH movimiento_etiqueta USING INDEX movimiento_etiqueta_etiqueta " +
                          "(etiqueta_id=?)"),
          )

      expected.forEach { (name, readAndStep) ->
        val (read, step) = readAndStep
        val plan = driver.planOf(read)
        assertTrue(step in plan, "$name: $plan")
        assertTrue(plan.none { it == "SCAN movimiento" }, "$name: $plan")
      }
    } finally {
      driver.close()
    }
  }

  @Test
  fun `recent and filtered reads scan movimiento_recent without a temporary sort`() {
    val driver = PlanRecordingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      TallybookDatabase.Schema.create(driver)
      val queries = TallybookDatabase(driver).movimientoQueries
      val reads =
          mapOf(
              "selectRecentMovimientosByMoneda" to
                  {
                    queries.selectRecentMovimientosByMoneda("COP", 20).executeAsList()
                  },
              "selectFilteredMovimientos without filters" to
                  {
                    queries
                        .selectFilteredMovimientos(
                            "COP",
                            null,
                            null,
                            null,
                            0,
                            emptyList(),
                            0,
                            emptyList(),
                        )
                        .executeAsList()
                  },
              "selectFilteredMovimientos with range and tipo" to
                  {
                    queries
                        .selectFilteredMovimientos(
                            "COP",
                            "2026-01-01",
                            "2026-12-31",
                            "GASTO",
                            0,
                            emptyList(),
                            0,
                            emptyList(),
                        )
                        .executeAsList()
                  },
              "selectFilteredMovimientos with cuenta and medio" to
                  {
                    queries
                        .selectFilteredMovimientos(
                            "COP",
                            null,
                            null,
                            null,
                            1,
                            listOf("nequi"),
                            1,
                            listOf("DEBITO"),
                        )
                        .executeAsList()
                  },
              "selectFilteredMovimientosByCategory" to
                  {
                    queries
                        .selectFilteredMovimientosByCategory(
                            "COP",
                            null,
                            null,
                            null,
                            0,
                            emptyList(),
                            0,
                            emptyList(),
                            listOf("cat_comida"),
                        )
                        .executeAsList()
                  },
          )

      reads.forEach { (name, read) ->
        val plan = driver.planOf(read)
        assertTrue(
            plan.any { it.contains("movimiento USING INDEX movimiento_recent") },
            "$name: $plan",
        )
        assertTrue(plan.none { it.contains("TEMP B-TREE") }, "$name: $plan")
      }
    } finally {
      driver.close()
    }
  }

  /**
   * Records each read's SQL and binders so the test can ask SQLite for that exact statement's plan.
   */
  private class PlanRecordingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    private var lastSql: String? = null
    private var lastParameters = 0
    private var lastBinders: (SqlPreparedStatement.() -> Unit)? = null

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      lastSql = sql
      lastParameters = parameters
      lastBinders = binders
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    fun planOf(read: () -> Any): List<String> {
      read()
      return delegate
          .executeQuery(
              null,
              "EXPLAIN QUERY PLAN ${checkNotNull(lastSql)}",
              { cursor ->
                val details = mutableListOf<String>()
                while (cursor.next().value) details += requireNotNull(cursor.getString(3))
                QueryResult.Value(details)
              },
              lastParameters,
              lastBinders,
          )
          .value
    }
  }
}
