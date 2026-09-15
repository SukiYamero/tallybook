package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

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
    composeRule.setContent {
      TallybookTheme {
        MovimientoRow(
            movimiento = movimiento(TipoMovimiento.GASTO, minorUnits = 10_000),
            categoria = null,
        )
      }
    }

    composeRule.onNodeWithText("Sin categoría").assertExists()
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

  private fun movimiento(tipo: TipoMovimiento, minorUnits: Long) =
      Movimiento(
          id = "mov-1",
          tipo = tipo,
          monto = Money(minorUnits, Moneda.COP),
          fecha = LocalDate(2026, 9, 15),
          categoriaId = "cat_missing",
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
