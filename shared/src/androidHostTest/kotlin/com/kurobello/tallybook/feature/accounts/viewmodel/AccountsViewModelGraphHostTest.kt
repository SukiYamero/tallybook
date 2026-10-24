package com.kurobello.tallybook.feature.accounts.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.CuentaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.SaldoRepository
import com.kurobello.tallybook.core.data.SaldoRepositoryImpl
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.cashCuenta
import com.kurobello.tallybook.core.data.insertCuentaRow
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.di.viewModelModule
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class AccountsViewModelGraphHostTest {
  @Test
  fun `production graph creates a credit card whose SQL balance estimate and in use rule show`() =
      runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val jdbc =
            Class.forName("org.sqlite.JDBC")
                .asSubclass(Driver::class.java)
                .getDeclaredConstructor()
                .newInstance()
        DriverManager.registerDriver(jdbc)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(driver)
        val database =
            TallybookDatabase(driver).apply {
              insertCuentaRow(cashCuenta(Moneda.COP), ChangeStamp.SEED)
            }
        val directory = createTempDirectory("tallybook-accounts-vm-")
        val factory =
            PreferencesDataStoreFactory(
                object : PreferencesPathProvider {
                  override fun path(file: PreferencesFile) =
                      directory.resolve(file.fileName).toString()
                },
                dispatcher,
            )
        val preferences = AppPreferencesRepositoryImpl(factory.create())
        val clock = MutableClock(Instant.parse("2026-10-24T12:00:00Z"))
        val cuentas =
            CuentaRepositoryImpl(
                database,
                dispatcher,
                IdGenerator { "nu" },
                clock,
                TestChangeClock(),
            )
        val movimientos = MovimientoRepositoryImpl(database, dispatcher, TestChangeClock())
        val context = koinApplication {
          modules(
              viewModelModule,
              module {
                single<AppPreferencesRepository> { preferences }
                single<CuentaRepository> { cuentas }
                single<SaldoRepository> { SaldoRepositoryImpl(database, dispatcher) }
                single<Clock> { clock }
                single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
              },
          )
        }
        val owner =
            object : ViewModelStoreOwner {
              override val viewModelStore = ViewModelStore()
            }
        try {
          assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.COP))
          assertEquals(
              DataOutcome.Success(Unit),
              CategoriaRepositoryImpl(
                      database,
                      dispatcher,
                      IdGenerator { "unused" },
                      TestChangeClock(),
                  )
                  .ensureSeeded(
                      CategorySeedCatalog.categories.map {
                        Categoria(it.id, it.id, it.padreId, it.icono, it.color)
                      }
                  ),
          )
          val vm =
              resolveViewModel(
                  vmClass = AccountsViewModel::class,
                  viewModelStore = owner.viewModelStore,
                  extras = defaultExtras(owner),
                  scope = context.koin.scopeRegistry.rootScope,
              )
          runCurrent()
          assertEquals(listOf(Money(0, Moneda.COP)), vm.ready().totals)

          vm.onSearchChanged("NU")
          vm.onCreate()
          vm.onDraftChanged { onTipo(TipoCuenta.CREDITO).onSaldo("500").onDiaCorte("15") }
          vm.onSave()
          runCurrent()
          vm.onSheetHidden()
          vm.onSearchChanged("")
          assertEquals(
              DataOutcome.Success(Unit),
              movimientos.insert(compra(LocalDate(2026, 10, 20), Money(12_000, Moneda.COP))),
          )
          runCurrent()

          val nu = vm.ready().groups.single { it.tipo == TipoCuenta.CREDITO }.rows.single()
          assertEquals(Money(-62_000, Moneda.COP), nu.saldo)
          assertEquals(Money(62_000, Moneda.COP), nu.credit?.deuda)
          assertEquals(Money(12_000, Moneda.COP), nu.credit?.cycleSpending)
          assertEquals(listOf(Money(-62_000, Moneda.COP)), vm.ready().totals)

          vm.onOpenCuenta("nu")
          vm.onRequestDelete()
          vm.onConfirmDelete()
          runCurrent()
          assertEquals(AccountConfirmation.DeleteInUse(1L), vm.uiState.value.sheet?.confirmation)
          vm.onConfirmArchive()
          runCurrent()
          vm.onSheetHidden()
          assertEquals(listOf("nu"), vm.ready().archived.map { it.cuenta.id })
          assertEquals(listOf(Money(-62_000, Moneda.COP)), vm.ready().totals)
        } finally {
          owner.viewModelStore.clear()
          runCurrent()
          context.close()
          factory.close()
          driver.close()
          DriverManager.deregisterDriver(jdbc)
          directory.toFile().deleteRecursively()
          Dispatchers.resetMain()
        }
      }
}

private fun AccountsViewModel.ready(): AccountsContent.Ready =
    assertIs<AccountsContent.Ready>(uiState.value.content)

private fun compra(fecha: LocalDate, monto: Money) =
    Movimiento(
        "cine",
        TipoMovimiento.GASTO,
        monto,
        fecha,
        "cat_otros",
        null,
        null,
        Instant.parse("2026-10-20T00:00:00Z"),
        "nu",
        MedioPago.CREDITO,
    )
