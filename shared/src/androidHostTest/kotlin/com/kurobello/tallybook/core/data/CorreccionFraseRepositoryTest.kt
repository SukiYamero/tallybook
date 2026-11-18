package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.OrigenFrase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class CorreccionFraseRepositoryTest {

  @Test
  fun `insertAll then observeSince returns them newest first`() = runTest {
    withStorage { _, repository, _ ->
      val old = correccion("old", "2026-10-01T10:00:00Z")
      val recent = correccion("recent", "2026-10-02T10:00:00Z", leido = null, guardado = "5000")

      assertEquals(DataOutcome.Success(Unit), repository.insertAll(listOf(old, recent)))

      assertEquals(
          DataOutcome.Success(listOf(recent, old)),
          repository.observeSince(Instant.parse("2026-09-01T00:00:00Z")).first(),
      )
    }
  }

  @Test
  fun `observeSince excludes older records`() = runTest {
    withStorage { _, repository, _ ->
      val old = correccion("old", "2026-10-01T10:00:00Z")
      val boundary = correccion("boundary", "2026-10-02T10:00:00Z")
      repository.insertAll(listOf(old, boundary))

      assertEquals(
          DataOutcome.Success(listOf(boundary)),
          repository.observeSince(boundary.createdAt).first(),
      )
    }
  }

  @Test
  fun `tombstoned records are not observed`() = runTest {
    withStorage { driver, repository, _ ->
      val kept = correccion("kept", "2026-10-02T10:00:00Z")
      val gone = correccion("gone", "2026-10-03T10:00:00Z")
      repository.insertAll(listOf(kept, gone))
      driver.tombstone("correccion_frase", "gone")

      assertEquals(
          DataOutcome.Success(listOf(kept)),
          repository.observeSince(Instant.parse("2026-09-01T00:00:00Z")).first(),
      )
    }
  }

  @Test
  fun `one insertAll issues one stamp shared by its rows and seen by the max stamp query`() =
      runTest {
        val changeClock = TestChangeClock()
        withStorage(changeClock) { driver, repository, database ->
          repository.insertAll(
              listOf(
                  correccion("a", "2026-10-01T10:00:00Z"),
                  correccion("b", "2026-10-01T11:00:00Z"),
              )
          )

          val stamp = changeClock.issuedStamps.single().value
          assertEquals(stamp, driver.storedStamps("correccion_frase", "a")?.first)
          assertEquals(stamp, driver.storedStamps("correccion_frase", "b")?.first)
          assertEquals(
              stamp,
              database.changeStampQueries.selectMaxChangeStamp().executeAsOne().max_stamp,
          )
        }
      }

  @Test
  fun `an empty insertAll issues no stamp`() = runTest {
    val changeClock = TestChangeClock()
    withStorage(changeClock) { _, repository, _ ->
      repository.insertAll(emptyList())

      assertEquals(0, changeClock.issuedStamps.size)
    }
  }

  private fun correccion(
      id: String,
      createdAt: String,
      leido: String? = "1000",
      guardado: String? = "2000",
  ) =
      CorreccionFrase(
          id = id,
          origen = OrigenFrase.DICTADO,
          palabras = listOf("gaste", "mil", "en", "pan"),
          campo = CampoCorreccion.MONTO,
          leido = leido,
          propuesto = "1000",
          guardado = guardado,
          createdAt = Instant.parse(createdAt),
      )

  private suspend fun TestScope.withStorage(
      changeClock: ChangeClock = TestChangeClock(),
      block: suspend (SqlDriver, CorreccionFraseRepository, TallybookDatabase) -> Unit,
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    try {
      val database = TallybookDatabase(driver)
      val repository =
          CorreccionFraseRepositoryImpl(
              database = database,
              dispatcher = StandardTestDispatcher(testScheduler),
              changeClock = changeClock,
          )
      block(driver, repository, database)
    } finally {
      driver.close()
    }
  }
}
