package com.kurobello.tallybook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.ActividadRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.EtiquetaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.seedCashCuentas
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class CategoryTagsIntegrationTest {

  @Test
  fun `one category and two tags filter by tag and parent and survive a tag deletion`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver).apply { seedCashCuentas() }
      val dispatcher = StandardTestDispatcher(testScheduler)
      var nextId = 0
      val ids = IdGenerator { "id-${++nextId}" }
      val categorias = CategoriaRepositoryImpl(database, dispatcher, ids, TestChangeClock())
      val etiquetas =
          EtiquetaRepositoryImpl(database, dispatcher, ids, FixedClock, TestChangeClock())
      val movimientos = MovimientoRepositoryImpl(database, dispatcher, TestChangeClock())
      val actividades = ActividadRepositoryImpl(database, dispatcher)
      assertEquals(
          DataOutcome.Success(Unit),
          categorias.ensureSeeded(
              CategorySeedCatalog.categories.map {
                Categoria(it.id, it.id, it.padreId, it.icono, it.color)
              }
          ),
      )
      val empresa = etiquetas.create("Empresa").saved()
      val viaje = etiquetas.create("Viaje Chile").saved()
      val movimiento =
          Movimiento(
              id = "cena",
              tipo = TipoMovimiento.GASTO,
              monto = Money(45_000, Moneda.COP),
              fecha = LocalDate(2026, 10, 10),
              categoriaId = "cat_restaurante",
              titulo = "Cena con clientes",
              descripcion = null,
              createdAt = Instant.parse("2026-10-10T21:00:00Z"),
              cuentaId = Cuenta.defaultCashId(Moneda.COP),
              medio = MedioPago.EFECTIVO,
              etiquetaIds = listOf(viaje.id, empresa.id),
          )
      assertEquals(DataOutcome.Success(Unit), movimientos.insert(movimiento))

      val byTag = MovimientoReadCriteria(Moneda.COP, etiquetaIds = setOf(empresa.id))
      val byParent = MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("cat_comida"))
      assertEquals(listOf(movimiento), actividades.read(byTag))
      assertEquals(listOf(movimiento), actividades.read(byParent))
      assertEquals(
          emptyList(),
          actividades.read(MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("cat_cafe"))),
      )
      assertEquals(
          DataOutcome.Success(mapOf(empresa.id to 1L, viaje.id to 1L)),
          etiquetas.observeMovimientoCounts(Moneda.COP).first(),
      )

      val observed = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
      backgroundScope.launch {
        actividades.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect(observed::add)
      }
      testScheduler.runCurrent()
      assertEquals(DataOutcome.Success(Unit), etiquetas.delete(empresa.id))
      testScheduler.runCurrent()

      val remaining = movimiento.copy(etiquetaIds = listOf(viaje.id))
      assertEquals(
          listOf(remaining),
          assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(observed.last()).data.movimientos,
      )
      assertEquals(emptyList(), actividades.read(byTag))
      assertEquals(DataOutcome.Success(listOf(viaje)), etiquetas.getAll())
      assertEquals(DataOutcome.Success(1L), categorias.countMovimientos("cat_restaurante"))
    }
  }

  private suspend fun ActividadRepositoryImpl.read(
      criteria: MovimientoReadCriteria
  ): List<Movimiento> =
      assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(observeFiltered(criteria).first())
          .data
          .movimientos

  private fun DataOutcome<EtiquetaWrite>.saved(): Etiqueta =
      assertIs<EtiquetaWrite.Saved>(assertIs<DataOutcome.Success<EtiquetaWrite>>(this).data)
          .etiqueta

  private object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-10-16T12:00:00Z")
  }
}
