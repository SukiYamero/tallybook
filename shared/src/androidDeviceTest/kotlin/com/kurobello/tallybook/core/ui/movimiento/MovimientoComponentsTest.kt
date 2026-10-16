package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
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
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_row_extra_categories
import tallybook.shared.generated.resources.unknown_category

class MovimientoComponentsTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun categoryChipExposesLabelSelectionAndOneCallbackPerTap() {
    var clickCount = 0
    composeRule.setContent {
      TallybookTheme {
        CategoryChip(
            categoria = comida,
            selected = true,
            onClick = { clickCount += 1 },
        )
      }
    }

    composeRule
        .onNodeWithText("Comida")
        .assertIsSelected()
        .assertHasClickAction()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .performClick()

    composeRule.runOnIdle { assertEquals(1, clickCount) }
  }

  @Test
  fun movimientoRowDerivesIncomeAndExpenseSignsFromTipo() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(
            movimiento = movimiento(TipoMovimiento.INGRESO, minorUnits = 123_400),
            categoria = comida,
        )
        MovimientoRow(
            movimiento = movimiento(TipoMovimiento.GASTO, minorUnits = 56_700),
            categoria = comida,
        )
      }
    }

    composeRule.onNodeWithText("$\u00A0+1.234,00").assertExists()
    composeRule.onNodeWithText("$\u00A0-567,00").assertExists()
  }

  @Test
  fun movimientoRowUsesLocalizedFallbackWhenCategoryIsMissing() {
    val unknownCategoryLabel = runBlocking { getString(Res.string.unknown_category) }
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(
            movimiento = movimiento(TipoMovimiento.GASTO, minorUnits = 10_000),
            categoria = null,
        )
      }
    }

    composeRule.onNodeWithText(unknownCategoryLabel).assertExists()
    composeRule.onNodeWithText("cat_missing").assertDoesNotExist()
  }

  @Test
  fun balanceCardDisplaysTheThreeSummaryAmounts() {
    val summary =
        MovimientoSummary(
            ingresos = Money(1_000_000, Moneda.COP),
            gastos = Money(250_000, Moneda.COP),
        )
    composeRule.setContent { TallybookTheme { BalanceCard(summary) } }

    composeRule.onNodeWithText("$\u00A010.000,00").assertExists()
    composeRule.onNodeWithText("$\u00A02.500,00").assertExists()
    composeRule.onNodeWithText("$\u00A07.500,00").assertExists()
  }

  @Test
  fun homeRowKeepsCategoryFirstAndFullDescriptionWithoutAnAction() {
    val description = "A very long meal description that remains available to accessibility"
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(movimiento(TipoMovimiento.GASTO, 100).copy(descripcion = description), comida)
      }
    }
    composeRule.onNodeWithText("Comida").assertExists().assertHasNoClickAction()
    composeRule
        .onNodeWithContentDescription("Comida, $description", substring = true)
        .assertHasNoClickAction()
  }

  @Test
  fun largeTextAndMaximumPositiveAmountKeepTitleAndAmountApart() {
    val description = "Long description that must remain fully available to accessibility"
    val amount =
        signedMovimientoAmountText(Money(Long.MAX_VALUE, Moneda.COP), TipoMovimiento.INGRESO)
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme {
          MovimientoRow(
              movimiento(TipoMovimiento.INGRESO, Long.MAX_VALUE).copy(descripcion = description),
              comida,
              Modifier.width(240.dp),
          )
        }
      }
    }
    val titleBounds =
        composeRule
            .onNodeWithText("Comida", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
    val amountBounds =
        composeRule.onNodeWithText(amount, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    assertTrue(!titleBounds.overlaps(amountBounds))
    composeRule.onNodeWithContentDescription(description, substring = true).assertHasNoClickAction()
  }

  @Test
  fun rowWithSeveralCategoriesShowsTheFirstOneAndACount() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(
            movimiento(TipoMovimiento.GASTO, 100)
                .copy(categoriaIds = listOf("cat_comida", "cat_cine", "cat_salud")),
            comida,
        )
      }
    }

    composeRule.onNodeWithText("Comida").assertExists()
    composeRule.onNodeWithText("+2", useUnmergedTree = true).assertExists()
    val extraLabel = runBlocking {
      getPluralString(Res.plurals.movimiento_row_extra_categories, 2, 2)
    }
    composeRule.onNodeWithContentDescription(extraLabel, substring = true).assertExists()
  }

  @Test
  fun rowWithOneCategoryShowsNoCount() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(
            movimiento(TipoMovimiento.GASTO, 100).copy(categoriaIds = listOf("cat_comida")),
            comida,
        )
      }
    }

    composeRule.onNodeWithText("+0", useUnmergedTree = true).assertDoesNotExist()
    composeRule.onNodeWithText("+1", useUnmergedTree = true).assertDoesNotExist()
  }

  private fun movimiento(tipo: TipoMovimiento, minorUnits: Long) =
      Movimiento(
          id = "mov-1",
          tipo = tipo,
          monto = Money(minorUnits, Moneda.COP),
          fecha = LocalDate(2026, 9, 15),
          categoriaIds = listOf("cat_missing"),
          descripcion = null,
          createdAt = Instant.fromEpochMilliseconds(1_757_894_400_000),
      )

  private companion object {
    val comida =
        Categoria(
            id = "cat_comida",
            nombre = "Comida",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
  }
}
