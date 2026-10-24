package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.ui.HistoryMovimientoRowUi
import com.kurobello.tallybook.feature.history.ui.HistoryRow
import com.kurobello.tallybook.feature.search.ui.SearchResultRow
import com.kurobello.tallybook.feature.search.ui.SearchResultRowUi
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test

class MovimientoRowMedioTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun homeRowAnnouncesTheMedioOnceAtTheEndOfItsDescription() {
    assertMedioAnnouncedOnce { movimiento -> MovimientoRow(movimiento, comida) }
  }

  @Test
  fun historyRowAnnouncesTheMedioOnceAtTheEndOfItsDescription() {
    assertMedioAnnouncedOnce { movimiento ->
      HistoryRow(HistoryMovimientoRowUi(movimiento, comida, "Almuerzo", comida.nombre))
    }
  }

  @Test
  fun searchRowAnnouncesTheMedioOnceAtTheEndOfItsDescription() {
    assertMedioAnnouncedOnce { movimiento ->
      SearchResultRow(SearchResultRowUi(movimiento, comida, "Almuerzo", comida.nombre))
    }
  }

  private fun assertMedioAnnouncedOnce(row: @Composable (Movimiento) -> Unit) {
    val movimiento = movimiento(MedioPago.CREDITO)
    val label = runBlocking { getString(MedioPago.CREDITO.labelResource) }
    composeRule.setContent { TallybookTheme { row(movimiento) } }

    composeRule
        .onAllNodes(
            SemanticsMatcher("description ends with the medio") { node ->
              node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any {
                it.startsWith("Almuerzo, ") && it.endsWith(", $label")
              }
            }
        )
        .assertCountEquals(1)
    composeRule
        .onAllNodesWithContentDescription(label, substring = true, useUnmergedTree = true)
        .assertCountEquals(1)
  }

  private fun movimiento(medio: MedioPago) =
      Movimiento(
          id = "mov-credito",
          tipo = TipoMovimiento.GASTO,
          monto = Money(5_000_000, Moneda.COP),
          fecha = LocalDate(2026, 9, 15),
          categoriaId = comida.id,
          titulo = "Almuerzo",
          descripcion = null,
          createdAt = Instant.fromEpochMilliseconds(1_757_894_400_000),
          cuentaId = Cuenta.defaultCashId(Moneda.COP),
          medio = medio,
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
