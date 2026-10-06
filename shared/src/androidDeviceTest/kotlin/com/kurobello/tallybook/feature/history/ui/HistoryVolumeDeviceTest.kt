package com.kurobello.tallybook.feature.history.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.ui.layout.TallybookAppShell
import com.kurobello.tallybook.core.ui.navigation.History
import com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HISTORY_SAVED_INPUT_KEY
import com.kurobello.tallybook.feature.history.viewmodel.HistoryDateProvider
import com.kurobello.tallybook.feature.history.viewmodel.HistoryLoadState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryRepositories
import com.kurobello.tallybook.feature.history.viewmodel.HistorySavedInput
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import com.kurobello.tallybook.feature.history.viewmodel.projectHistory
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.history_overall_balance
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.unknown_category

@OptIn(ExperimentalResourceApi::class)
class HistoryVolumeDeviceTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun selectedMonthIsCompleteAndScrollableInTestOwnedStorage() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    check(context.packageName == "com.kurobello.tallybook.shared.test") {
      "Volume storage must belong to the self-targeted test APK"
    }
    setResourceReaderAndroidContext(context)
    val before = memoryPssKb()
    val fixture = VolumeFixture(context)
    val visible = mutableStateOf(true)
    var composed = false
    var metrics: String? = null
    try {
      metrics = exercise(fixture, visible) { composed = true }
    } finally {
      if (composed) {
        composeRule.runOnIdle { visible.value = false }
        composeRule.waitForIdle()
      }
      fixture.close()
      println(
          "History volume target=${Build.MODEL} sdk=${Build.VERSION.SDK_INT} testDebug=${context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0} selected=10000 excludedCurrency=4000 excludedDates=4000 beforePssKb=$before afterClosePssKb=${memoryPssKb()} $metrics"
      )
    }
  }

  private fun exercise(
      fixture: VolumeFixture,
      visible: androidx.compose.runtime.MutableState<Boolean>,
      onComposed: () -> Unit,
  ): String {
    runBlocking { withTimeout(30_000) { fixture.startAndSeed() } }
    val afterReady = memoryPssKb()
    val measurement = runBlocking { withTimeout(30_000) { fixture.measureSnapshot() } }
    val historyLabel = runBlocking { getString(Res.string.navigation_history) }
    val overallLabel = runBlocking { getString(Res.string.history_overall_balance) }
    var clearancePixels = 0f
    composeRule.setContent {
      if (visible.value) {
        val viewModel = requireNotNull(fixture.viewModel)
        val state by viewModel.uiState.collectAsState()
        TallybookTheme {
          TallybookAppShell(
              modifier = Modifier.fillMaxSize(),
              bottomBar = { TallybookBottomNavigation({}, selectedDestination = History) },
          ) { clearance ->
            clearancePixels = with(LocalDensity.current) { clearance.toPx() }
            HistoryScreen(state, viewModel, {}, clearance)
          }
        }
      }
    }
    onComposed()
    val list = composeRule.onNode(hasContentDescription(historyLabel) and hasScrollAction())
    list.performScrollToNode(hasText(measurement.first))
    composeRule.onNodeWithText(measurement.first).assertIsDisplayed()
    list.performScrollToNode(hasText(measurement.middle))
    composeRule.onNodeWithText(measurement.middle).assertIsDisplayed()
    list.performScrollToNode(hasText(measurement.last)).performTouchInput { swipeUp() }
    composeRule.onNodeWithText(measurement.last).assertIsDisplayed()
    composeRule.onNodeWithText(overallLabel).assertIsDisplayed()
    assertTrue(clearancePixels > 0f)
    val rowBottom =
        composeRule.onNodeWithText(measurement.last).fetchSemanticsNode().boundsInRoot.bottom
    assertTrue(list.fetchSemanticsNode().boundsInRoot.bottom - rowBottom >= clearancePixels)
    return "read=${measurement.read} projection=${measurement.projection} plan=${measurement.plan} afterReadyPssKb=$afterReady afterScrollPssKb=${memoryPssKb()}"
  }

  private class VolumeFixture(private val context: Context) {
    private val name = "history-volume-${UUID.randomUUID()}.db"
    private val directory =
        context.cacheDir.resolve(name.removeSuffix(".db")).also { check(it.mkdirs()) }
    private val driver =
        RecordingDriver(AndroidSqliteDriver(TallybookDatabase.Schema, context, name))
    private val database = TallybookDatabase(driver)
    private val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path() = directory.resolve("preferences.preferences_pb").absolutePath
            },
            Dispatchers.IO,
        )
    private val preferences = AppPreferencesRepositoryImpl(factory.create())
    private val movimientos = MovimientoRepositoryImpl(database, Dispatchers.IO)
    private val categorias = CategoriaRepositoryImpl(database, Dispatchers.IO)
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private var collector: Job? = null
    var viewModel: HistoryViewModel? = null
      private set

    suspend fun startAndSeed() {
      assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.COP))
      assertEquals(
          DataOutcome.Success(Unit),
          categorias.ensureSeeded(CategorySeedProvider().getCategories()),
      )
      val fallback = getString(Res.string.unknown_category)
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val handle = SavedStateHandle()
        handle[HISTORY_SAVED_INPUT_KEY] =
            encodeToSavedState(
                HistorySavedInput.serializer(),
                HistorySavedInput(1, "Month", "2026-09-15"),
            )
        viewModel =
            HistoryViewModel(
                HistoryRepositories(preferences, movimientos, categorias),
                handle,
                fallback,
                HistoryDateProvider(
                    object : Clock {
                      override fun now() = Instant.parse("2026-10-05T12:00:00Z")
                    },
                    TimeZoneProvider { TimeZone.UTC },
                ),
                Dispatchers.Default,
                scope,
            )
        store.put("volume", requireNotNull(viewModel))
        collector = scope.launch { requireNotNull(viewModel).uiState.collect {} }
      }
      requireNotNull(viewModel).uiState.first { it.period is HistoryLoadState.Ready }
      withContext(Dispatchers.IO) {
        database.transaction {
          repeat(10000) { index ->
            val id = "cop-${index.toString().padStart(5, '0')}"
            insert(
                id,
                "COP",
                "2026-09-${(index % 30 + 1).toString().padStart(2, '0')}",
                "Volume $id",
            )
          }
          repeat(4000) { index ->
            insert("usd-$index", "USD", "2026-09-15", null)
            insert(
                "outside-$index",
                "COP",
                if (index % 2 == 0) "2026-08-31" else "2026-10-01",
                null,
            )
          }
        }
      }
      val state =
          requireNotNull(viewModel).uiState.first {
            (it.period as? HistoryLoadState.Ready)?.value?.movementCount == 10000 &&
                (it.overall as? HistoryLoadState.Ready)?.value?.gastos?.minorUnits == 1_400_000L
          }
      assertEquals(
          1_000_000L,
          assertIs<HistoryLoadState.Ready<HistoryProjection.Content>>(state.period)
              .value
              .summary
              .gastos
              .minorUnits,
      )
    }

    private fun insert(id: String, currency: String, date: String, description: String?) {
      database.movimientoQueries.insertMovimiento(
          id,
          "GASTO",
          100,
          currency,
          date,
          "cat_comida",
          description,
          "2026-09-15T12:00:00Z",
      )
    }

    suspend fun measureSnapshot(): Measurement {
      val range = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
      val started = TimeSource.Monotonic.markNow()
      val rows =
          assertIs<DataOutcome.Success<List<com.kurobello.tallybook.core.model.Movimiento>>>(
                  movimientos.observeRange(Moneda.COP, range).first()
              )
              .data
      val readTime = started.elapsedNow().toString()
      val sql = requireNotNull(driver.rangeSql)
      assertEquals(10000, rows.size)
      assertEquals(10000, rows.map { it.id }.toSet().size)
      assertEquals("cop-09989", rows.first().id)
      assertEquals("cop-00000", rows.last().id)
      assertTrue(rows.all { it.monto.moneda == Moneda.COP && it.fecha in range.start..range.end })
      val categorySnapshot =
          assertIs<DataOutcome.Success<List<com.kurobello.tallybook.core.model.Categoria>>>(
                  categorias.getAll()
              )
              .data
              .associateBy { it.id }
      val fallback = getString(Res.string.unknown_category)
      val projectionStart = TimeSource.Monotonic.markNow()
      val projection =
          assertIs<HistoryProjection.Content>(
              projectHistory(Moneda.COP, range, rows, categorySnapshot, fallback)
          )
      val projectionTime = projectionStart.elapsedNow().toString()
      assertEquals(10000, projection.movementCount)
      assertEquals(1_000_000L, projection.summary.gastos.minorUnits)
      val plan =
          withContext(Dispatchers.IO) {
            driver
                .executeQuery(
                    null,
                    "EXPLAIN QUERY PLAN $sql",
                    { cursor ->
                      val details = mutableListOf<String>()
                      while (cursor.next().value) details += requireNotNull(cursor.getString(3))
                      QueryResult.Value(details)
                    },
                    3,
                ) {
                  bindString(0, "COP")
                  bindString(1, "2026-09-01")
                  bindString(2, "2026-09-30")
                }
                .value
          }
      return Measurement(
          requireNotNull(rows.first().descripcion),
          requireNotNull(rows[5000].descripcion),
          requireNotNull(rows.last().descripcion),
          readTime,
          projectionTime,
          plan,
      )
    }

    fun close() {
      runBlocking {
        withTimeout(30_000) {
          collector?.cancelAndJoin()
          scopeJob.cancelAndJoin()
        }
      }
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        store.clear()
        viewModel = null
      }
      factory.close()
      driver.close()
      check(name.startsWith("history-volume-"))
      if (context.getDatabasePath(name).exists()) check(context.deleteDatabase(name))
      check(directory.deleteRecursively())
    }
  }

  private data class Measurement(
      val first: String,
      val middle: String,
      val last: String,
      val read: String,
      val projection: String,
      val plan: List<String>,
  )

  private class RecordingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    @Volatile
    var rangeSql: String? = null
      private set

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (sql.trimStart().startsWith("SELECT") && sql.contains("fecha >=")) rangeSql = sql
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }
  }

  private companion object {
    fun memoryPssKb(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss
  }
}
