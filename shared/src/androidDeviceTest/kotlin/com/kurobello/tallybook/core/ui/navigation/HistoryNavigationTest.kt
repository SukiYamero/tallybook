package com.kurobello.tallybook.core.ui.navigation

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import java.io.Serializable as JavaSerializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.history_scope_week
import tallybook.shared.generated.resources.history_year_description
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.ui_catalog_title

internal class HistoryNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun actualShellRetainsTypedSelectionScrollAndRestoresIntoFreshOwners() {
    withShell { fixture ->
      historyTab().performClick()
      composeRule.onNodeWithText(text(Res.string.history_scope_week)).performClick()
      val list = historyList()
      list.performScrollToNode(hasText("Item 35"))
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      historyTab().performClick()
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      val starts = fixture.movimientos.starts.get()
      restoreShell(fixture)
      historyTab().assertIsSelected()
      composeRule.waitUntil(5_000) { fixture.movimientos.starts.get() > starts }
      composeRule.onNodeWithText("Item 35").assertIsDisplayed()
      list.performScrollToNode(hasText(text(Res.string.history_scope_week)))
      composeRule.onNodeWithText(text(Res.string.history_scope_week)).assertIsSelected()
    }
  }

  @Test
  fun onlyPrimariesNavigateAndCreationBlocksUnderlyingSelectionUntilHidden() {
    withShell {
      listOf(Res.string.navigation_search, Res.string.navigation_profile).forEach { label ->
        composeRule
            .onNodeWithContentDescription(text(label))
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
      }
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_home))
          .assertHasClickAction()
      historyTab().performClick()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      historyTab().assertIsSelected()
      composeRule.onNodeWithText("Food").assertIsDisplayed()
      systemBack()
      historyTab().assertIsSelected()
      composeRule.onNodeWithText("Food").assertDoesNotExist()
      composeRule
          .onNodeWithContentDescription(text(Res.string.movimiento_amount_expense_prompt))
          .assertIsDisplayed()
      val pane =
          SemanticsMatcher.expectValue(
              SemanticsProperties.PaneTitle,
              text(Res.string.add_movimiento_pane_title),
          )
      repeat(3) {
        if (composeRule.onAllNodes(pane).fetchSemanticsNodes().isNotEmpty()) {
          var imeVisible = false
          InstrumentationRegistry.getInstrumentation().runOnMainSync {
            imeVisible =
                WindowInspector.getGlobalWindowViews().any { view ->
                  view.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
                }
          }
          InstrumentationRegistry.getInstrumentation()
              .sendStatus(
                  2,
                  Bundle().apply {
                    putString(
                        "history_diagnostic",
                        JSONObject()
                            .put("phase", "form-dismiss")
                            .put("imeVisible", imeVisible)
                            .toString(),
                    )
                  },
              )
          systemBack()
          historyTab().assertIsSelected()
        }
      }
      composeRule.onNode(pane).assertDoesNotExist()
      historyTab().assertIsSelected()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  @Test
  fun freshShellDoesNotRestoreChangedCreationDraftOrAnOpenSession() {
    withShell { fixture ->
      historyTab().performClick()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithText("Food").performClick()
      val prompt = text(Res.string.movimiento_amount_expense_prompt)
      composeRule.onNodeWithContentDescription(prompt).performTextInput("1234")
      restoreShell(fixture)
      historyTab().assertIsSelected()
      composeRule.onNodeWithContentDescription(prompt).assertDoesNotExist()
      composeRule
          .onNodeWithContentDescription(text(Res.string.navigation_add_movement))
          .performClick()
      composeRule.onNodeWithText("Food").performClick()
      composeRule
          .onNodeWithContentDescription(prompt)
          .assert(
              SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
          )
    }
  }

  @Test
  fun outgoingPrimaryReadsStopAfterSettledTransitionAndSharingTimeout() {
    withShell { fixture ->
      composeRule.waitUntil(5_000) { fixture.movimientos.activeRecent.get() == 1 }
      historyTab().performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(7_000) { fixture.movimientos.activeRecent.get() == 0 }
      assertEquals(1, fixture.movimientos.activeRanges.get())
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(7_000) { fixture.movimientos.activeRanges.get() == 0 }
      assertEquals(1, fixture.movimientos.activeRecent.get())
    }
  }

  @Test
  fun focusedYearPopupConsumesBackBeforeHistoryRootAndCatalogReturnsHome() {
    withShell { fixture ->
      composeRule.runOnIdle { fixture.movimientos.addPriorYear() }
      historyTab().performClick()
      val year = runBlocking { getString(Res.string.history_year_description, 2026) }
      composeRule.onNodeWithContentDescription(year).performClick()
      recordPopupWindows("before-popup-focus")
      composeRule.onNode(isPopup()).assertExists()
      composeRule.onNodeWithText("2025").assertIsDisplayed()
      try {
        composeRule.waitUntil(5_000) { popupWindowState().isReady }
      } finally {
        recordPopupWindows("after-popup-focus-wait")
      }
      systemBack()
      composeRule.onNodeWithText("2025").assertDoesNotExist()
      composeRule.onNode(isPopup()).assertDoesNotExist()
      composeRule.waitUntil(5_000) { popupWindowState().count == 0 }
      historyTab().assertIsSelected()
      systemBack()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  @Test
  fun explicitCatalogLaunchHasSupportedPrimaryReturnWithoutBlankRoot() {
    withShell(start = UiCatalog) {
      composeRule.onNodeWithText(text(Res.string.ui_catalog_title)).assertExists()
      systemBack()
      composeRule.onNodeWithContentDescription(text(Res.string.navigation_home)).assertIsSelected()
    }
  }

  private fun historyTab() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_history)) and hasClickAction()
      )

  private fun historyList() =
      composeRule.onNode(
          hasContentDescription(text(Res.string.navigation_history)) and
              SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
      )

  private fun systemBack() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
  }

  private fun popupWindowState(): PopupWindowState {
    var state = PopupWindowState(0, false)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val popups =
          WindowInspector.getGlobalWindowViews().filter {
            it.javaClass.name == "androidx.compose.ui.window.PopupLayout"
          }
      val popup = popups.singleOrNull()
      state =
          PopupWindowState(
              popups.size,
              popup != null && popup.isAttachedToWindow && popup.isShown && popup.hasWindowFocus(),
          )
    }
    return state
  }

  private fun recordPopupWindows(phase: String) {
    val nativeWindows = JSONArray()
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      WindowInspector.getGlobalWindowViews().forEach { view ->
        val params = view.layoutParams as? WindowManager.LayoutParams
        nativeWindows.put(
            JSONObject()
                .put("class", view.javaClass.name)
                .put("attached", view.isAttachedToWindow)
                .put("shown", view.isShown)
                .put("focused", view.hasWindowFocus())
                .put("width", view.width)
                .put("height", view.height)
                .put("flags", params?.flags)
                .put("type", params?.type)
        )
      }
    }
    val diagnostic = JSONObject().put("phase", phase).put("windows", nativeWindows)
    InstrumentationRegistry.getInstrumentation()
        .sendStatus(2, Bundle().apply { putString("history_diagnostic", diagnostic.toString()) })
  }

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }

  private fun restoreShell(fixture: DeviceShell) {
    lateinit var snapshot: NavigationSnapshot
    composeRule.runOnIdle { snapshot = fixture.saveAndUnmount() }
    composeRule.waitForIdle()
    composeRule.runOnIdle { fixture.restoreNewOwners(snapshot) }
    composeRule.waitForIdle()
  }

  private fun withShell(start: Destination = Home, block: (DeviceShell) -> Unit) {
    val fixture = DeviceShell()
    try {
      composeRule.setContent {
        if (fixture.mounted.value) {
          val owner = fixture.owner.value
          KoinIsolatedContext(fixture.context) {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalViewModelStoreOwner provides owner.viewModelOwner,
                LocalSavedStateRegistryOwner provides owner,
                LocalSaveableStateRegistry provides fixture.registry.value,
            ) {
              TallybookTheme {
                TallybookViewportOverlayHost { TallybookNavHost(startDestination = start) }
              }
            }
          }
        }
      }
      composeRule.waitForIdle()
      block(fixture)
    } finally {
      composeRule.runOnIdle { fixture.close() }
    }
  }
}

private data class PopupWindowState(val count: Int, val isReady: Boolean)

private class DeviceShell {
  val movimientos = NavigationMovimientos()
  val mounted = mutableStateOf(true)
  val owner = mutableStateOf(NavigationParent(null))
  val registry =
      mutableStateOf(SaveableStateRegistry(null) { it is Parcelable || it is JavaSerializable })
  private val ids = AtomicInteger()
  val context = koinApplication {
    modules(
        viewModelModule,
        module {
          single<AppPreferencesRepository> { NavigationPreferences() }
          single<MovimientoRepository> { movimientos }
          single<CategoriaRepository> { NavigationCategories() }
          single<IdGenerator> { IdGenerator { "new-${ids.incrementAndGet()}" } }
          single<Clock> {
            object : Clock {
              override fun now() = Instant.parse("2026-10-05T12:00:00Z")
            }
          }
          single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
          single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { Dispatchers.Default }
        },
    )
  }

  fun saveAndUnmount(): NavigationSnapshot {
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(owner.value.save())
      parcel.writeValue(registry.value.performSave())
      parcel.setDataPosition(0)
      val saved = requireNotNull(parcel.readBundle(javaClass.classLoader))
      val raw = parcel.readValue(javaClass.classLoader)
      require(raw is Map<*, *>)
      val compose =
          raw.entries.associate { (key, value) ->
            require(key is String && value is List<*>)
            key to value.toList()
          }
      mounted.value = false
      return NavigationSnapshot(saved, compose)
    } finally {
      parcel.recycle()
    }
  }

  fun restoreNewOwners(snapshot: NavigationSnapshot) {
    check(!mounted.value)
    val original = owner.value
    original.close()
    val restored = NavigationParent(snapshot.owner)
    assertNotSame(original.viewModelStore, restored.viewModelStore)
    owner.value = restored
    registry.value =
        SaveableStateRegistry(snapshot.compose) { it is Parcelable || it is JavaSerializable }
    mounted.value = true
  }

  fun close() {
    owner.value.close()
    context.close()
  }
}

private data class NavigationSnapshot(
    val owner: SavedState,
    val compose: Map<String, List<Any?>>,
)

private class NavigationParent(restored: SavedState?) :
    SavedStateRegistryOwner, ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner

  init {
    controller.performRestore(restored)
    viewModelOwner = androidx.lifecycle.viewmodel.ViewModelStoreOwner(viewModelStore, this)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun save(): SavedState = savedState().also(controller::performSave)

  fun close() {
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    viewModelStore.clear()
  }
}

private class NavigationMovimientos : MovimientoRepository {
  private val rows =
      MutableStateFlow(
          (1..60).map { index ->
            Movimiento(
                "row-$index",
                TipoMovimiento.GASTO,
                Money(125, Moneda.USD),
                LocalDate(2026, 10, 5),
                "food",
                "Item $index",
                Instant.parse("2026-10-05T12:00:00Z"),
            )
          }
      )
  val starts = AtomicInteger()
  val activeRanges = AtomicInteger()
  val activeRecent = AtomicInteger()

  fun addPriorYear() {
    rows.value +=
        rows.value
            .first()
            .copy(
                id = "prior-year",
                fecha = LocalDate(2025, 10, 5),
                createdAt = Instant.parse("2025-10-05T12:00:00Z"),
            )
  }

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = flow {
    starts.incrementAndGet()
    activeRanges.incrementAndGet()
    try {
      emitAll(
          rows.map { values ->
            DataOutcome.Success(
                values
                    .filter { it.fecha in range.start..range.end && it.monto.moneda == moneda }
                    .sortedWith(navigationStorageOrder)
            )
          }
      )
    } finally {
      activeRanges.decrementAndGet()
    }
  }

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      flow {
        activeRecent.incrementAndGet()
        try {
          emitAll(
              rows.map { values ->
                DataOutcome.Success(
                    values
                        .filter { it.monto.moneda == moneda }
                        .sortedWith(navigationStorageOrder)
                        .take(limit)
                )
              }
          )
        } finally {
          activeRecent.decrementAndGet()
        }
      }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = flow {
    activeRecent.incrementAndGet()
    try {
      emitAll(rows.map { DataOutcome.Success(it.sortedWith(navigationStorageOrder).take(limit)) })
    } finally {
      activeRecent.decrementAndGet()
    }
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = rows.map {
    DataOutcome.Success(it.map { row -> row.fecha.year }.distinct())
  }

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = rows.map {
    DataOutcome.Success(
        MovimientoSummary(Money(0, moneda), Money(it.sumOf { row -> row.monto.minorUnits }, moneda))
    )
  }

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    rows.value += movimiento
    return DataOutcome.Success(Unit)
  }

  override suspend fun delete(id: String): DataOutcome<Unit> {
    rows.value = rows.value.filter { it.id != id }
    return DataOutcome.Success(Unit)
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(rows.value)
}

private val navigationStorageOrder =
    compareByDescending<Movimiento> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

private class NavigationPreferences : AppPreferencesRepository {
  override val preferences: Flow<DataOutcome<AppPreferencesState>> =
      MutableStateFlow(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD))))

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

private class NavigationCategories : CategoriaRepository {
  private val category = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      MutableStateFlow(DataOutcome.Success(listOf(category)))

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      DataOutcome.Success(listOf(category))

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}
