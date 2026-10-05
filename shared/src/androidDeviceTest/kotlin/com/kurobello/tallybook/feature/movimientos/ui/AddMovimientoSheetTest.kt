package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
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
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.more_fields
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_category_expense_prompt
import tallybook.shared.generated.resources.save_expense

class AddMovimientoSheetTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun initialFormUsesContentHeightSheet() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SubmitCountingActions(),
            onDismiss = {},
        )
      }
    }

    val sheet =
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.PaneTitle,
                resourceString(Res.string.add_movimiento_pane_title),
            )
        )
    val sheetNode = sheet.fetchSemanticsNode()
    val rootHeight = sheetNode.layoutInfo.coordinates.findRootCoordinates().size.height.toFloat()

    composeRule.runOnIdle {
      assertTrue(
          sheetNode.boundsInRoot.height < rootHeight,
          "Expected the compact form sheet to remain shorter than the window",
      )
    }
  }

  @Test
  fun openingDetailsKeepsTheSheetAtItsCompactPositionUntilMotionAdvances() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SubmitCountingActions(),
            onDismiss = {},
        )
      }
    }

    val paneTitle = resourceString(Res.string.add_movimiento_pane_title)
    val compactTop =
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, paneTitle))
            .fetchSemanticsNode()
            .boundsInRoot
            .top
    composeRule.mainClock.autoAdvance = false

    composeRule.onNodeWithText(resourceString(Res.string.more_fields)).performClick()

    val openingTop =
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, paneTitle))
            .fetchSemanticsNode()
            .boundsInRoot
            .top

    assertEquals(
        compactTop,
        openingTop,
        "Expected sheet position to animate from the compact detent instead of jumping",
    )
  }

  @Test
  fun dateAppearsAboveAmount() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SubmitCountingActions(),
            onDismiss = {},
        )
      }
    }

    val dateBounds =
        composeRule
            .onNodeWithContentDescription(resourceString(Res.string.date))
            .fetchSemanticsNode()
            .boundsInRoot
    val amountBounds =
        composeRule
            .onNodeWithContentDescription(
                resourceString(Res.string.movimiento_amount_expense_prompt)
            )
            .fetchSemanticsNode()
            .boundsInRoot

    assertTrue(
        dateBounds.bottom <= amountBounds.top,
        "Expected date to be above amount: date=$dateBounds, amount=$amountBounds",
    )
  }

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

    composeRule
        .onNodeWithContentDescription(resourceString(Res.string.save_expense))
        .assertIsNotEnabled()
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

    composeRule.onNodeWithText(resourceString(Res.string.save_expense)).performClick()
    composeRule.runOnIdle { assertEquals(1, actions.submitCount) }
  }

  @Test
  fun newEntryStartsWithCategoryWithoutSubmittingOrDismissing() {
    var dismissCount = 0
    val actions = SubmitCountingActions()

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState =
                state()
                    .copy(
                        amountInput = "",
                        selectedCategoriaId = null,
                    ),
            actions = actions,
            onDismiss = { dismissCount += 1 },
        )
      }
    }

    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_category_expense_prompt))
        .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(0, actions.submitCount)
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
            .onNodeWithContentDescription(resourceString(Res.string.save_expense))
            .fetchSemanticsNode()
            .boundsInRoot
            .height
    composeRule.runOnIdle { isSubmitting = true }
    composeRule.waitForIdle()

    assertEquals(
        idleHeight,
        composeRule
            .onNodeWithContentDescription(resourceString(Res.string.save_expense))
            .fetchSemanticsNode()
            .boundsInRoot
            .height,
    )
  }

  @Test
  fun closedKeyboardCtaIsFullyReachable() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SubmitCountingActions(),
            onDismiss = {},
        )
      }
    }

    val cta = composeRule.onNodeWithContentDescription(resourceString(Res.string.save_expense))
    val ctaNode = cta.fetchSemanticsNode()
    val ctaBounds = ctaNode.boundsInRoot
    val rootHeight = ctaNode.layoutInfo.coordinates.findRootCoordinates().size.height.toFloat()
    cta.assertIsDisplayed()
    composeRule.runOnIdle {
      assertTrue(ctaBounds.height >= with(composeRule.density) { 48.dp.toPx() })
      assertTrue(
          ctaBounds.bottom <= rootHeight,
          "Expected CTA bottom ${ctaBounds.bottom} to be within root height $rootHeight",
      )
    }
  }

  @Test
  fun expandedDetailsScrollWithoutMovingTheSaveAction() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(descripcion = "Details ".repeat(100)),
            actions = SubmitCountingActions(),
            onDismiss = {},
            modifier = Modifier.height(400.dp),
        )
      }
    }

    composeRule.onNodeWithText(resourceString(Res.string.more_fields)).performClick()
    val cta = composeRule.onNodeWithContentDescription(resourceString(Res.string.save_expense))
    val initialBounds = cta.fetchSemanticsNode().boundsInRoot

    composeRule.onNodeWithTag("movimiento_details").performTouchInput { swipeUp() }
    composeRule.waitForIdle()

    val ctaNode = cta.fetchSemanticsNode()
    val ctaBounds = ctaNode.boundsInRoot
    val rootHeight = ctaNode.layoutInfo.coordinates.findRootCoordinates().size.height.toFloat()
    cta.assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(
          initialBounds,
          ctaBounds,
          "Expected the fixed CTA to keep its bounds while details scroll",
      )
      assertTrue(ctaBounds.height >= with(composeRule.density) { 48.dp.toPx() })
      assertTrue(
          ctaBounds.bottom <= rootHeight,
          "Expected CTA bottom ${ctaBounds.bottom} to be within root height $rootHeight",
      )
    }
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

  private fun state(isSubmitting: Boolean = false, descripcion: String = "") =
      AddMovimientoUiState(
          tipo = TipoMovimiento.GASTO,
          amountInput = "1",
          fecha = LocalDate(2026, 9, 15),
          moneda = Moneda.COP,
          categorias = listOf(category),
          selectedCategoriaId = category.id,
          descripcion = descripcion,
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

  private fun resourceString(resource: StringResource): String = runBlocking { getString(resource) }

  private class SubmitCountingActions(private val onSubmitRequested: () -> Unit = {}) :
      AddMovimientoActions {
    var submitCount = 0

    override fun onTipoChanged(tipo: TipoMovimiento) = Unit

    override fun onAmountChanged(value: String) = Unit

    override fun onFechaChanged(value: LocalDate) = Unit

    override fun onCategoriaSelected(id: String) = Unit

    override fun onDescripcionChanged(value: String) = Unit

    override fun onDiscardChanges() = Unit

    override fun onRetryLoad() = Unit

    override fun onSubmit() {
      submitCount += 1
      onSubmitRequested()
    }
  }
}
