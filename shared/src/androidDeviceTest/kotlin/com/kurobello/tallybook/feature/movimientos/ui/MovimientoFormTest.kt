package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class MovimientoFormTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun typeAndFieldChangesReachActions() {
    val actions = RecordingActions()

    composeRule.setContent { TallybookTheme { MovimientoForm(state(), actions) } }

    composeRule.onNodeWithText("Ingreso").performClick()
    composeRule.onNodeWithTag("movimiento_amount").performTextInput("12,50")

    composeRule.runOnIdle {
      assertEquals(TipoMovimiento.INGRESO, actions.tipo)
      assertEquals("12,50", actions.amount)
    }
  }

  @Test
  fun emptySubmitShowsAmountAndCategoryErrorsWithoutDismissing() {
    val actions = RecordingActions()
    val invalidState = state(submitAttempted = true)

    composeRule.setContent { TallybookTheme { MovimientoForm(invalidState, actions) } }

    composeRule.onNodeWithText("Ingresa un monto").assertIsDisplayed()
    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Elige una categoría",
            )
        )
        .assertIsDisplayed()
  }

  @Test
  fun invalidAmountReceivesFocusAndAnnouncesItsSpecificError() {
    composeRule.setContent {
      TallybookTheme { MovimientoForm(state(submitAttempted = true), RecordingActions()) }
    }

    composeRule
        .onNodeWithTag("movimiento_amount")
        .assertIsFocused()
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Ingresa un monto",
            )
        )
  }

  @Test
  fun descriptionDisclosureLimitsInputTo180Characters() {
    val actions = RecordingActions()

    composeRule.setContent { TallybookTheme { MovimientoForm(state(), actions) } }

    composeRule.onNodeWithText("Más detalles").performClick()
    composeRule.onNodeWithTag("movimiento_description").performTextInput("x".repeat(181))

    composeRule.runOnIdle { assertEquals(180, actions.descripcion.length) }
  }

  @Test
  fun overflowShowsTheAmountError() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            state(amountInput = "999999999999999999999", submitAttempted = true),
            RecordingActions(),
        )
      }
    }

    composeRule.onNodeWithText("El monto es demasiado grande").assertIsDisplayed()
  }

  @Test
  fun loadAndSaveErrorsKeepTheExistingValuesAndOfferRetry() {
    val actions = RecordingActions()
    val errorState =
        state(
            amountInput = "10",
            selectedCategoriaId = category.id,
            error = MovimientoFormError.PREFERENCES_UNAVAILABLE,
        )

    composeRule.setContent { TallybookTheme { MovimientoForm(errorState, actions) } }

    composeRule
        .onNodeWithText(
            "No pudimos cargar tu configuración. Lo que escribiste sigue aquí. Intenta cargarla de nuevo."
        )
        .assertIsDisplayed()
    composeRule.onNodeWithText("Reintentar").performClick()
    composeRule.onNodeWithText("10").assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(1, actions.retryCount) }
  }

  private companion object {
    val category =
        Categoria(
            id = "food",
            nombre = "Comida",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )

    fun state(
        tipo: TipoMovimiento = TipoMovimiento.GASTO,
        amountInput: String = "",
        selectedCategoriaId: String? = null,
        submitAttempted: Boolean = false,
        error: MovimientoFormError? = null,
    ) =
        AddMovimientoUiState(
            tipo = tipo,
            amountInput = amountInput,
            fecha = LocalDate(2026, 9, 15),
            moneda = Moneda.COP,
            categorias = listOf(category),
            selectedCategoriaId = selectedCategoriaId,
            descripcion = "",
            isLoading = false,
            isSubmitting = false,
            submitAttempted = submitAttempted,
            error = error,
        )
  }

  private class RecordingActions : AddMovimientoActions {
    var tipo: TipoMovimiento? = null
    var amount = ""
    var descripcion = ""
    var retryCount = 0

    override fun onTipoChanged(tipo: TipoMovimiento) {
      this.tipo = tipo
    }

    override fun onAmountChanged(value: String) {
      amount = value
    }

    override fun onFechaChanged(value: LocalDate) = Unit

    override fun onCategoriaSelected(id: String) = Unit

    override fun onDescripcionChanged(value: String) {
      descripcion = value
    }

    override fun onRetryLoad() {
      retryCount += 1
    }

    override fun onSubmit() = Unit
  }
}
