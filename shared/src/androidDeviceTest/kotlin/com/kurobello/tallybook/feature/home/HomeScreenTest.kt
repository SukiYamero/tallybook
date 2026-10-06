package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.app_name
import tallybook.shared.generated.resources.home_add_first
import tallybook.shared.generated.resources.home_empty_title
import tallybook.shared.generated.resources.home_load_error_title
import tallybook.shared.generated.resources.home_recent
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.ui_catalog_title

class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun outerShellKeepsBrandAndSearchVisibleWhileLoading() {
    val appName = localizedString(Res.string.app_name)
    val searchPlaceholder = localizedString(Res.string.home_search_placeholder)
    composeRule.setContent { TallybookTheme { HostedHomeScreen(HomeUiState.loading(), {}, {}) } }

    composeRule
        .onNodeWithText(appName)
        .assertIsDisplayed()
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    composeRule.onNodeWithText(searchPlaceholder).assertIsDisplayed()
  }

  @Test
  fun outerShellBaselineHeightsGrowForLargeText() {
    val appName = localizedString(Res.string.app_name)
    val searchPlaceholder = localizedString(Res.string.home_search_placeholder)
    var deviceDensity: Density? = null
    composeRule.setContent {
      val currentDensity = LocalDensity.current
      deviceDensity = currentDensity
      CompositionLocalProvider(
          LocalDensity provides Density(currentDensity.density, fontScale = 3f)
      ) {
        TallybookTheme { HostedHomeScreen(HomeUiState.loading(), {}, {}) }
      }
    }

    val density = checkNotNull(deviceDensity)
    val headerHeight =
        with(density) {
          composeRule.onNodeWithText(appName).fetchSemanticsNode().size.height.toDp()
        }
    val searchHeight =
        with(density) {
          composeRule.onNodeWithText(searchPlaceholder).fetchSemanticsNode().size.height.toDp()
        }

    assertTrue(headerHeight > 44.dp)
    assertTrue(searchHeight > 46.dp)
  }

  @Test
  fun loadingExposesLocalizedProgressAndAddAction() {
    val loadingDescription = localizedString(Res.string.loading)
    val addMovementLabel = localizedString(Res.string.navigation_add_movement)
    composeRule.setContent { TallybookTheme { HostedHomeScreen(HomeUiState.loading(), {}, {}) } }

    composeRule.mainClock.advanceTimeBy(240)
    composeRule.onNodeWithContentDescription(loadingDescription).assertIsDisplayed()
    composeRule.onNodeWithText("$ 000.000").assertDoesNotExist()
    composeRule.onNodeWithContentDescription(addMovementLabel).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationShowsMonetaActionsWithHomeSelected() {
    val homeLabel = localizedString(Res.string.navigation_home)
    val navigationLabels =
        listOf(
            homeLabel,
            localizedString(Res.string.navigation_history),
            localizedString(Res.string.navigation_add_movement),
            localizedString(Res.string.navigation_search),
            localizedString(Res.string.navigation_profile),
        )
    composeRule.setContent { TallybookTheme { HostedHomeScreen(HomeUiState.loading(), {}, {}) } }

    navigationLabels.forEach { label ->
      composeRule.onNodeWithContentDescription(label).assertIsDisplayed()
    }
    composeRule.onNodeWithContentDescription(homeLabel).assertIsSelected()
  }

  @Test
  fun reservedNavigationDestinationsHaveNoClickAction() {
    val unavailableLabels =
        listOf(
            Res.string.navigation_search,
            Res.string.navigation_profile,
        )
    composeRule.setContent { TallybookTheme { HostedHomeScreen(HomeUiState.loading(), {}, {}) } }

    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.navigation_home))
        .assertHasClickAction()
    unavailableLabels.forEach { label ->
      composeRule
          .onNodeWithContentDescription(localizedString(label))
          .assertHasNoClickAction()
          .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.navigation_add_movement))
        .assertHasClickAction()
  }

  @Test
  fun emptyStateTitleIsAHeading() {
    val emptyTitle = localizedString(Res.string.home_empty_title)
    composeRule.setContent { TallybookTheme { HostedHomeScreen(emptyState, {}, {}) } }

    composeRule
        .onNodeWithText(emptyTitle)
        .assertIsDisplayed()
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
  }

  @Test
  fun emptyCtaAndBottomNavigationInvokeTheSameAddCallback() {
    var additions = 0
    val emptyCtaLabel = localizedString(Res.string.home_add_first)
    val navigationAddLabel = localizedString(Res.string.navigation_add_movement)
    composeRule.setContent {
      TallybookTheme { HostedHomeScreen(emptyState, { additions += 1 }, {}) }
    }

    composeRule.onNodeWithText(emptyCtaLabel).performClick()
    composeRule.onNodeWithContentDescription(navigationAddLabel).performClick()

    composeRule.runOnIdle { assertEquals(2, additions) }
  }

  @Test
  fun errorRendersRetryThatInvokesTheRetryCallback() {
    var retries = 0
    val retryLabel = localizedString(Res.string.retry)
    composeRule.setContent {
      TallybookTheme {
        HostedHomeScreen(
            HomeUiState.error(DataError.Local(IllegalStateException())),
            {},
            { retries += 1 },
        )
      }
    }

    composeRule.onNodeWithText(retryLabel).performClick()

    composeRule.runOnIdle { assertEquals(1, retries) }
  }

  @Test
  fun debugCatalogActionInvokesTheCatalogCallback() {
    var catalogOpenings = 0
    val catalogLabel = localizedString(Res.string.ui_catalog_title)
    composeRule.setContent {
      TallybookTheme {
        HostedHomeScreen(
            uiState = HomeUiState.loading(),
            onAddMovimiento = {},
            onRetry = {},
            onOpenCatalog = { catalogOpenings += 1 },
        )
      }
    }

    composeRule.onNodeWithText(catalogLabel).performClick()

    composeRule.runOnIdle { assertEquals(1, catalogOpenings) }
  }

  @Test
  fun errorStateBecomesAPoliteLiveRegion() {
    val errorTitle = localizedString(Res.string.home_load_error_title)
    var uiState by mutableStateOf(HomeUiState.loading())
    composeRule.setContent { TallybookTheme { HostedHomeScreen(uiState, {}, {}) } }

    composeRule.runOnIdle { uiState = HomeUiState.error(DataError.Local(IllegalStateException())) }

    composeRule
        .onNodeWithText(errorTitle)
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            )
        )
  }

  @Test
  fun populatedStateShowsRecentMovimientoAndItsCategory() {
    val recentMovementsLabel = localizedString(Res.string.home_recent)
    composeRule.setContent { TallybookTheme { HostedHomeScreen(populatedState, {}, {}) } }

    composeRule.onNodeWithText("Comida").assertIsDisplayed()
    composeRule.onNodeWithText(recentMovementsLabel).assertIsDisplayed()
    composeRule.onNodeWithText("$\u00A0-1,00").assertIsDisplayed()
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }

  private companion object {
    val categoria =
        Categoria(
            id = "food",
            nombre = "Comida",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
    val movimiento =
        Movimiento(
            id = "movement",
            tipo = TipoMovimiento.GASTO,
            monto = Money(100, Moneda.COP),
            fecha = LocalDate(2026, 9, 15),
            categoriaId = categoria.id,
            descripcion = null,
            createdAt = Instant.parse("2026-09-15T00:00:00Z"),
        )
    val emptyState =
        HomeUiState.ready(
            HomeContent(
                summary = HomeUiState.emptySummary(Moneda.COP),
                recent = emptyList(),
                categoriasById = emptyMap(),
            )
        )
    val populatedState =
        HomeUiState.ready(
            HomeContent(
                summary = MovimientoSummary(Money(100, Moneda.COP), Money(100, Moneda.COP)),
                recent = listOf(movimiento),
                categoriasById = mapOf(categoria.id to categoria),
            )
        )
  }
}

@androidx.compose.runtime.Composable
private fun HostedHomeScreen(
    uiState: HomeUiState,
    onAddMovimiento: () -> Unit,
    onRetry: () -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
  com.kurobello.tallybook.core.ui.layout.TallybookAppShell(
      modifier = modifier.fillMaxSize(),
      bottomBar = {
        com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation(
            onAddMovement = onAddMovimiento,
            onSelectDestination = {},
        )
      },
  ) { clearance ->
    HomeScreen(uiState, onAddMovimiento, onRetry, onOpenCatalog, bottomBarClearance = clearance)
  }
}
