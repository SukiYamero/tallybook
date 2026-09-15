package com.kurobello.tallybook.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_add_first
import tallybook.shared.generated.resources.home_recent
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

class HomeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun loadingExposesLocalizedProgressAndAddAction() {
    val loadingDescription = localizedString(Res.string.loading)
    val addMovementLabel = localizedString(Res.string.home_add_first)
    composeRule.setContent { TallybookTheme { HomeScreen(HomeUiState.loading(), {}, {}) } }

    composeRule.onNodeWithContentDescription(loadingDescription).assertIsDisplayed()
    composeRule.onNodeWithContentDescription(addMovementLabel).assertIsDisplayed()
  }

  @Test
  fun emptyCtaAndFabInvokeTheSameAddCallback() {
    var additions = 0
    val addMovementLabel = localizedString(Res.string.home_add_first)
    composeRule.setContent { TallybookTheme { HomeScreen(emptyState, { additions += 1 }, {}) } }

    composeRule.onNodeWithText(addMovementLabel).performClick()
    composeRule.onNodeWithContentDescription(addMovementLabel).performClick()

    composeRule.runOnIdle { assertEquals(2, additions) }
  }

  @Test
  fun errorRendersRetryThatInvokesTheRetryCallback() {
    var retries = 0
    val retryLabel = localizedString(Res.string.retry)
    composeRule.setContent {
      TallybookTheme {
        HomeScreen(
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
  fun populatedStateShowsRecentMovimientoAndItsCategory() {
    val recentMovementsLabel = localizedString(Res.string.home_recent)
    composeRule.setContent { TallybookTheme { HomeScreen(populatedState, {}, {}) } }

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
        HomeUiState(
            isLoading = false,
            summary = HomeUiState.emptySummary(Moneda.COP),
            recent = emptyList(),
            categoriasById = emptyMap(),
            error = null,
        )
    val populatedState =
        HomeUiState(
            isLoading = false,
            summary = MovimientoSummary(Money(100, Moneda.COP), Money(100, Moneda.COP)),
            recent = listOf(movimiento),
            categoriasById = mapOf(categoria.id to categoria),
            error = null,
        )
  }
}
