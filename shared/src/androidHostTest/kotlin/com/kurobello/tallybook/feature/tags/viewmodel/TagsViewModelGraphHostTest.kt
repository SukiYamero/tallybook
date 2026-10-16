package com.kurobello.tallybook.feature.tags.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
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
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class TagsViewModelGraphHostTest {
  @Test
  fun `production graph creates a tag counts it live from SQL and deletes it keeping movimientos`() =
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
        val database = TallybookDatabase(driver)
        val directory = createTempDirectory("tallybook-tags-vm-")
        val factory =
            PreferencesDataStoreFactory(
                object : PreferencesPathProvider {
                  override fun path() = directory.resolve("preferences.preferences_pb").toString()
                },
                dispatcher,
            )
        val preferences = AppPreferencesRepositoryImpl(factory.create())
        val movimientos = MovimientoRepositoryImpl(database, dispatcher)
        val etiquetas =
            EtiquetaRepositoryImpl(
                database,
                dispatcher,
                IdGenerator { "tag-empresa" },
                object : Clock {
                  override fun now() = Instant.parse("2026-10-05T00:00:00Z")
                },
            )
        val context = koinApplication {
          modules(
              viewModelModule,
              module {
                single<AppPreferencesRepository> { preferences }
                single<EtiquetaRepository> { etiquetas }
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
              CategoriaRepositoryImpl(database, dispatcher, IdGenerator { "unused" })
                  .ensureSeeded(
                      CategorySeedCatalog.categories.map {
                        Categoria(it.id, it.id, it.padreId, it.icono, it.color)
                      }
                  ),
          )
          val vm =
              resolveViewModel(
                  vmClass = TagsViewModel::class,
                  viewModelStore = owner.viewModelStore,
                  extras = defaultExtras(owner),
                  scope = context.koin.scopeRegistry.rootScope,
              )
          runCurrent()
          assertEquals(TagsContent.Browse(emptyList()), vm.uiState.value.content)

          vm.onOpenSheet(TagSheetRequest.New("Empresa"))
          vm.onSaveSheet()
          runCurrent()
          vm.onSheetHidden()
          val created = assertIs<TagsContent.Browse>(vm.uiState.value.content).rows.single()
          assertEquals("tag-empresa", created.etiqueta.id)
          assertEquals(0L, created.count)

          assertEquals(DataOutcome.Success(Unit), movimientos.insert(movimiento("cop", Moneda.COP)))
          assertEquals(DataOutcome.Success(Unit), movimientos.insert(movimiento("usd", Moneda.USD)))
          runCurrent()
          assertEquals(
              1L,
              assertIs<TagsContent.Browse>(vm.uiState.value.content).rows.single().count,
          )

          vm.onOpenSheet(TagSheetRequest.Open("tag-empresa"))
          vm.onRequestDelete()
          runCurrent()
          assertEquals(TagDeletion.Confirm(2L), vm.uiState.value.sheet?.deletion)
          vm.onConfirmDelete()
          runCurrent()
          assertEquals(TagsContent.Browse(emptyList()), vm.uiState.value.content)
          val remaining = assertIs<DataOutcome.Success<List<Movimiento>>>(movimientos.getAll())
          assertEquals(
              listOf("cop" to emptyList(), "usd" to emptyList<String>()),
              remaining.data.sortedBy { it.id }.map { it.id to it.etiquetaIds },
          )
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
        "cat_otros",
        null,
        null,
        Instant.parse("2026-10-05T00:00:00Z"),
        etiquetaIds = listOf("tag-empresa"),
    )
