package com.kurobello.tallybook.feature.categories.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.di.entryModule
import com.kurobello.tallybook.di.viewModelModule
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CategoriesViewModelGraphHostTest {
  @Test
  fun `production graph creates a suggested category and counts it live from SQL`() = runTest {
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
    val database = TallybookDatabase(driver)
    val directory = createTempDirectory("tallybook-categories-vm-")
    val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path(file: PreferencesFile) = directory.resolve(file.fileName).toString()
            },
            dispatcher,
        )
    val preferences = AppPreferencesRepositoryImpl(factory.create())
    val movimientos = MovimientoRepositoryImpl(database, dispatcher)
    val categorias = CategoriaRepositoryImpl(database, dispatcher, IdGenerator { "custom-gym" })
    val context = koinApplication {
      modules(
          viewModelModule,
          entryModule,
          module {
            single<AppPreferencesRepository> { preferences }
            single<EtiquetaRepository> { FakeEtiquetaRepository() }
            single<CategoriaRepository> { categorias }
          },
      )
    }
    val owner =
        object : ViewModelStoreOwner {
          override val viewModelStore = ViewModelStore()
        }
    try {
      assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.COP))
      val vm =
          resolveViewModel(
              vmClass = CategoriesViewModel::class,
              viewModelStore = owner.viewModelStore,
              extras = defaultExtras(owner),
              scope = context.koin.scopeRegistry.rootScope,
          )
      runCurrent()
      assertEquals(
          CategoriesContent.Browse(emptyList(), emptyList()),
          vm.uiState.value.content,
      )

      vm.onOpenSheet(CategorySheetRequest.New("Gimnasio"))
      vm.onSaveEditor()
      runCurrent()
      val created = assertIs<CategoriesContent.Browse>(vm.uiState.value.content).custom.single()
      assertEquals("custom-gym", created.categoria.id)
      assertEquals(CategoryIcon.DUMBBELL, created.categoria.icono)
      assertEquals(0L, created.count)
      vm.onSheetHidden()

      movimientos.insert(movimiento("cop", Moneda.COP))
      movimientos.insert(movimiento("usd", Moneda.USD))
      runCurrent()
      val counted = assertIs<CategoriesContent.Browse>(vm.uiState.value.content).custom.single()
      assertEquals(1L, counted.count)

      vm.onOpenSheet(CategorySheetRequest.Open("custom-gym"))
      vm.onRequestDelete()
      runCurrent()
      assertEquals(CategoryDeletion.InUse(2L), vm.uiState.value.editor?.deletion)
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

private fun movimiento(id: String, moneda: Moneda) =
    Movimiento(
        id,
        TipoMovimiento.GASTO,
        Money(125, moneda),
        LocalDate(2026, 10, 5),
        "custom-gym",
        null,
        null,
        Instant.parse("2026-10-05T00:00:00Z"),
    )
