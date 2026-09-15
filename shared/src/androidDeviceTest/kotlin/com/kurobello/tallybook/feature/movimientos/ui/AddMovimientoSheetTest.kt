package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class AddMovimientoSheetTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun submittingDisablesTheCtaSoItCannotSubmitTwice() {
    val actions = SubmitCountingActions()
    val submittingState = state(isSubmitting = true)

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = submittingState,
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Guardar gasto").assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, actions.submitCount) }
  }

  @Test
  fun validCtaSubmitsOnce() {
    val actions = SubmitCountingActions()

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithText("Guardar gasto").performClick()
    composeRule.runOnIdle { assertEquals(1, actions.submitCount) }
  }

  @Test
  fun invalidSubmitShowsErrorsAndDoesNotDismissTheSheet() {
    var submitAttempted by mutableStateOf(false)
    var dismissCount = 0
    val actions = SubmitCountingActions { submitAttempted = true }

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState =
                state()
                    .copy(
                        amountInput = "",
                        selectedCategoriaId = null,
                        submitAttempted = submitAttempted,
                    ),
            actions = actions,
            onDismiss = { dismissCount += 1 },
        )
      }
    }

    composeRule.onNodeWithContentDescription("Guardar gasto").performClick()

    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Ingresa un monto",
            )
        )
        .assertIsDisplayed()
    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Elige una categoría",
            )
        )
        .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, actions.submitCount)
      assertEquals(0, dismissCount)
    }
  }

  @Test
  fun submittingCtaKeepsTheIdleCtaHeight() {
    var isSubmitting by mutableStateOf(false)

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(isSubmitting = isSubmitting),
            actions = SubmitCountingActions(),
            onDismiss = {},
        )
      }
    }

    val idleHeight =
        composeRule
            .onNodeWithContentDescription("Guardar gasto")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
    composeRule.runOnIdle { isSubmitting = true }
    composeRule.waitForIdle()

    assertEquals(
        idleHeight,
        composeRule
            .onNodeWithContentDescription("Guardar gasto")
            .fetchSemanticsNode()
            .boundsInRoot
            .height,
    )
  }

  @Test
  fun savedEffectUsesTheLatestDismissCallbackExactlyOnce() {
    val effects = MutableSharedFlow<AddMovimientoEffect>(extraBufferCapacity = 1)
    var firstDismisses = 0
    var latestDismisses = 0
    var useLatestCallback by mutableStateOf(false)

    composeRule.setContent {
      AddMovimientoSavedEffect(effects) {
        if (useLatestCallback) latestDismisses += 1 else firstDismisses += 1
      }
    }
    composeRule.runOnIdle { useLatestCallback = true }
    composeRule.runOnIdle { effects.tryEmit(AddMovimientoEffect.Saved) }
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals(0, firstDismisses)
      assertEquals(1, latestDismisses)
    }
  }

  private fun state(isSubmitting: Boolean = false) =
      AddMovimientoUiState(
          tipo = TipoMovimiento.GASTO,
          amountInput = "1",
          fecha = LocalDate(2026, 9, 15),
          moneda = Moneda.COP,
          categorias = listOf(category),
          selectedCategoriaId = category.id,
          descripcion = "",
          isLoading = false,
          isSubmitting = isSubmitting,
          submitAttempted = false,
          error = null,
      )

  private val category =
      Categoria(
          id = "food",
          nombre = "Comida",
          padreId = null,
          icono = CategoryIcon.UTENSILS,
          color = CategoryTint.AMBER,
      )

  private class SubmitCountingActions(private val onSubmitRequested: () -> Unit = {}) :
      AddMovimientoActions {
    var submitCount = 0

    override fun onTipoChanged(tipo: TipoMovimiento) = Unit

    override fun onAmountChanged(value: String) = Unit

    override fun onFechaChanged(value: LocalDate) = Unit

    override fun onCategoriaSelected(id: String) = Unit

    override fun onDescripcionChanged(value: String) = Unit

    override fun onRetryLoad() = Unit

    override fun onSubmit() {
      submitCount += 1
      onSubmitRequested()
    }
  }
}
