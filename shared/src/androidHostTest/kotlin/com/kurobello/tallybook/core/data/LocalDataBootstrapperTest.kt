package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class LocalDataBootstrapperTest {

  @Test
  fun `initialize resolves locale once then initializes preferences and categories`() = runTest {
    val localeProvider = FakeDeviceLocaleProvider("en-US")
    val preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    var resolvedResourceCount = 0
    val bootstrapper =
        bootstrapper(
            localeProvider = localeProvider,
            preferencesRepository = preferencesRepository,
            categoriaRepository = categoriaRepository,
            categorySeedProvider =
                CategorySeedProvider { resource ->
                  resolvedResourceCount += 1
                  resource.key
                },
        )

    assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
    assertEquals(1, localeProvider.calls)
    assertEquals(listOf(Moneda.USD), preferencesRepository.initializedWith)
    assertEquals(1, categoriaRepository.seedCalls)
    assertEquals(59, categoriaRepository.seededCategorias.single().size)
    assertEquals(59, resolvedResourceCount)
  }

  @Test
  fun `initialize stops before category seeding when preferences initialization fails`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("preferences failed")))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    val bootstrapper =
        bootstrapper(
            preferencesRepository = FakePreferencesRepository(failure),
            categoriaRepository = categoriaRepository,
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(0, categoriaRepository.seedCalls)
  }

  @Test
  fun `initialize returns a category seed failure`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("categories failed")))
    val categoriaRepository = FakeCategoriaRepository(failure)
    val changeClock = TestChangeClock()
    val seededCuentas = mutableListOf<Cuenta>()
    val bootstrapper =
        bootstrapper(
            categoriaRepository = categoriaRepository,
            changeClock = changeClock,
            seedDefaultCuenta = { cuenta ->
              seededCuentas += cuenta
              DataOutcome.Success(Unit)
            },
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(1, categoriaRepository.seedCalls)
    assertEquals(emptyList(), seededCuentas)
    assertEquals(0, changeClock.prepareCalls)
  }

  @Test
  fun `initialize returns a cash cuenta seed failure before preparing the clock`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("cuenta failed")))
    val changeClock = TestChangeClock()
    val bootstrapper = bootstrapper(changeClock = changeClock, seedDefaultCuenta = { failure })

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(0, changeClock.prepareCalls)
  }

  @Test
  fun `a fresh install seeds one cash cuenta in the stored primary currency`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val preferences = FakePreferencesRepository(DataOutcome.Success(Unit), stored = Moneda.USD)

      assertEquals(
          DataOutcome.Success(Unit),
          databaseBootstrapper(database, preferences).initialize(),
      )

      assertEquals(listOf(Cuenta.defaultCashId(Moneda.USD)), database.liveCuentaIds())
      assertEquals(
          Cuenta(
              id = Cuenta.defaultCashId(Moneda.USD),
              nombre = "account_type_cash",
              tipo = TipoCuenta.EFECTIVO,
              saldoInicial = Money(0, Moneda.USD),
              diaCorte = null,
              archivada = false,
              createdAt = NOW,
          ),
          database.cuentaQueries
              .selectCuentaById(Cuenta.defaultCashId(Moneda.USD))
              .executeAsOne()
              .toDomain(),
      )
      assertEquals(
          ChangeStamp.SEED.value to null,
          driver.storedStamps("cuenta", Cuenta.defaultCashId(Moneda.USD)),
      )
    }
  }

  @Test
  fun `a tombstoned default cash id makes the fresh install seed take a new id`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val cashId = Cuenta.defaultCashId(Moneda.COP)
      database.insertCuentaRow(cashCuenta(Moneda.COP), ChangeStamp.SEED)
      driver.tombstone("cuenta", cashId)
      val tombstone = driver.storedStamps("cuenta", cashId)
      val preferences = FakePreferencesRepository(DataOutcome.Success(Unit), stored = Moneda.COP)

      assertEquals(
          DataOutcome.Success(Unit),
          databaseBootstrapper(database, preferences).initialize(),
      )

      val live = database.cuentaQueries.selectAllCuentas().executeAsList().map { it.toDomain() }
      assertEquals(listOf(SEEDED_CASH_ID), live.map { it.id })
      assertEquals(Moneda.COP, live.single().moneda)
      assertEquals(tombstone, driver.storedStamps("cuenta", cashId))
    }
  }

  @Test
  fun `a restart or a primary currency change seeds no further cuenta`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val preferences = FakePreferencesRepository(DataOutcome.Success(Unit), stored = Moneda.COP)
      val bootstrapper = databaseBootstrapper(database, preferences)

      bootstrapper.initialize()
      assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
      preferences.setMonedaPrincipal(Moneda.MXN)
      assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())

      assertEquals(listOf(Cuenta.defaultCashId(Moneda.COP)), database.liveCuentaIds())
    }
  }

  @Test
  fun `a migrated cuenta in another currency prevents the fresh install seed`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      database.insertCuentaRow(cashCuenta(Moneda.USD), ChangeStamp.SEED)
      val preferences = FakePreferencesRepository(DataOutcome.Success(Unit), stored = Moneda.COP)

      assertEquals(
          DataOutcome.Success(Unit),
          databaseBootstrapper(database, preferences).initialize(),
      )

      assertEquals(listOf(Cuenta.defaultCashId(Moneda.USD)), database.liveCuentaIds())
    }
  }

  @Test
  fun `initialize prepares the clock with the stored maximum after seeding`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver, etiquetaIds = listOf("tag"))
      val tombstoneStamp = ChangeStamp.of(epochMillis = 9_000, counter = 2, node = NODE)
      database.insertLegacy(
          movimiento("live"),
          stamp = ChangeStamp.of(epochMillis = 9_000, counter = 1, node = NODE),
      )
      database.insertLegacy(movimiento("deleted"), stamp = tombstoneStamp)
      driver.tombstone("movimiento", "deleted")
      val events = mutableListOf<String>()
      val changeClock = RecordingChangeClock(events)
      val bootstrapper =
          bootstrapper(
              preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit), events),
              categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit), events),
              changeClock = changeClock,
              seedDefaultCuenta = {
                events += "cuenta"
                DataOutcome.Success(Unit)
              },
              storedMaxChangeStamp = {
                database.maxStoredChangeStamp(UnconfinedTestDispatcher(testScheduler))
              },
          )

      assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
      assertEquals(listOf("preferences", "seed", "cuenta", "prepare"), events)
      assertEquals(
          listOf<DataOutcome<ChangeStamp?>>(DataOutcome.Success(tombstoneStamp)),
          changeClock.storedMaxima,
      )
    }
  }

  @Test
  fun `the stored maximum covers cuenta and transferencia stamps`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver)
      database.insertLegacy(
          movimiento("live"),
          stamp = ChangeStamp.of(epochMillis = 9_000, counter = 0, node = NODE),
      )
      val cuentaStamp = ChangeStamp.of(epochMillis = 9_001, counter = 0, node = NODE)
      database.insertCuentaRow(
          cashCuenta(Moneda.COP).copy(id = "nequi", tipo = TipoCuenta.BANCO),
          cuentaStamp,
      )
      val read: suspend () -> DataOutcome<ChangeStamp?> = {
        database.maxStoredChangeStamp(UnconfinedTestDispatcher(testScheduler))
      }

      assertEquals(DataOutcome.Success(cuentaStamp), read())

      val transferenciaStamp = ChangeStamp.of(epochMillis = 9_002, counter = 0, node = NODE)
      database.insertTransferenciaRow(
          Transferencia(
              id = "move",
              cuentaOrigenId = "nequi",
              cuentaDestinoId = Cuenta.defaultCashId(Moneda.COP),
              montoOrigen = Money(100, Moneda.COP),
              montoDestino = Money(100, Moneda.COP),
              fecha = LocalDate(2026, 10, 1),
              descripcion = null,
              createdAt = NOW,
          ),
          transferenciaStamp,
      )
      driver.tombstone("transferencia", "move")

      assertEquals(DataOutcome.Success(transferenciaStamp), read())
    }
  }

  @Test
  fun `initialize fails when the stored maximum is not a change stamp`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver)
      driver.execute(null, "UPDATE categoria SET updated_at = 'not a stamp'", 0)
      val changeClock = TestChangeClock()
      val bootstrapper =
          bootstrapper(
              changeClock = changeClock,
              storedMaxChangeStamp = {
                database.maxStoredChangeStamp(UnconfinedTestDispatcher(testScheduler))
              },
          )

      val outcome = bootstrapper.initialize()

      assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(outcome).error)
      assertEquals(1, changeClock.prepareCalls)
    }
  }

  @Suppress("LongParameterList")
  private fun bootstrapper(
      localeProvider: DeviceLocaleProvider = FakeDeviceLocaleProvider("es-CO"),
      preferencesRepository: AppPreferencesRepository =
          FakePreferencesRepository(DataOutcome.Success(Unit)),
      categoriaRepository: CategoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit)),
      categorySeedProvider: CategorySeedProvider = CategorySeedProvider { resource ->
        resource.key
      },
      changeClock: ChangeClock = TestChangeClock(),
      seedDefaultCuenta: suspend (Cuenta) -> DataOutcome<Unit> = { DataOutcome.Success(Unit) },
      storedMaxChangeStamp: suspend () -> DataOutcome<ChangeStamp?> = { DataOutcome.Success(null) },
  ) =
      LocalDataBootstrapper(
          localeProvider = localeProvider,
          preferencesRepository = preferencesRepository,
          categoriaRepository = categoriaRepository,
          categorySeedProvider = categorySeedProvider,
          cuentaSeedProvider = CuentaSeedProvider(FixedClock) { resource -> resource.key },
          seedDefaultCuenta = seedDefaultCuenta,
          changeClock = changeClock,
          storedMaxChangeStamp = storedMaxChangeStamp,
      )

  private fun TestScope.databaseBootstrapper(
      database: TallybookDatabase,
      preferences: AppPreferencesRepository,
  ): LocalDataBootstrapper {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    return bootstrapper(
        preferencesRepository = preferences,
        seedDefaultCuenta = {
          database.seedDefaultCuenta(it, IdGenerator { SEEDED_CASH_ID }, dispatcher)
        },
        storedMaxChangeStamp = { database.maxStoredChangeStamp(dispatcher) },
    )
  }

  private fun TallybookDatabase.liveCuentaIds(): List<String> =
      Moneda.entries
          .map(Cuenta::defaultCashId)
          .filter { cuentaQueries.selectCuentaById(it).executeAsOneOrNull() != null }
          .also { check(it.size.toLong() == cuentaQueries.countLiveCuentas().executeAsOne()) }

  private fun movimiento(id: String) =
      Movimiento(
          id = id,
          tipo = TipoMovimiento.GASTO,
          monto = Money(100, Moneda.COP),
          fecha = LocalDate(2026, 10, 1),
          categoriaId = CATEGORIA_OTROS_ID,
          titulo = null,
          descripcion = null,
          createdAt = Instant.parse("2026-10-01T12:00:00Z"),
          cuentaId = Cuenta.defaultCashId(Moneda.COP),
          medio = MedioPago.EFECTIVO,
          etiquetaIds = listOf("tag"),
      )

  private class RecordingChangeClock(private val events: MutableList<String>) : ChangeClock {
    val storedMaxima = mutableListOf<DataOutcome<ChangeStamp?>>()

    override suspend fun prepare(
        storedMax: suspend () -> DataOutcome<ChangeStamp?>
    ): DataOutcome<Unit> {
      events += "prepare"
      val outcome = storedMax()
      storedMaxima += outcome
      return outcome.map {}
    }

    override fun next(): ChangeStamp = error("Bootstrap never issues a stamp")
  }

  private object FixedClock : Clock {
    override fun now(): Instant = NOW
  }

  private companion object {
    const val NODE = "0123456789abcdef"
    val NOW: Instant = Instant.parse("2026-10-24T09:00:00Z")
    const val SEEDED_CASH_ID: String = "cuenta-generated"
  }

  private class FakeDeviceLocaleProvider(
      private val languageTag: String,
  ) : DeviceLocaleProvider {
    var calls = 0

    override fun currentLanguageTag(): String {
      calls += 1
      return languageTag
    }
  }

  private class FakePreferencesRepository(
      private val initializeResult: DataOutcome<Unit>,
      private val events: MutableList<String> = mutableListOf(),
      private var stored: Moneda? = null,
  ) : AppPreferencesRepository {
    override val preferences: Flow<DataOutcome<AppPreferencesState>> = flow {
      val moneda = stored
      emit(
          DataOutcome.Success(
              if (moneda == null) AppPreferencesState.Uninitialized
              else AppPreferencesState.Ready(AppPreferences(moneda))
          )
      )
    }
    val initializedWith = mutableListOf<Moneda>()

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> {
      events += "preferences"
      initializedWith += monedaPrincipal
      if (initializeResult is DataOutcome.Success && stored == null) stored = monedaPrincipal
      return initializeResult
    }

    override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> {
      stored = moneda
      return DataOutcome.Success(Unit)
    }
  }

  private class FakeCategoriaRepository(
      private val seedResult: DataOutcome<Unit>,
      private val events: MutableList<String> = mutableListOf(),
  ) : CategoriaRepository by UnsupportedCategoriaRepository {
    var seedCalls = 0
    val seededCategorias = mutableListOf<List<Categoria>>()

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
        flowOf(DataOutcome.Success(emptyList()))

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

    override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> {
      events += "seed"
      seedCalls += 1
      seededCategorias += categorias
      return seedResult
    }
  }
}
