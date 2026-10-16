package com.kurobello.tallybook.feature.movimientos.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoDraftState
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoSheetActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoDiscardTarget
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementPhase
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementSession
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementCategoria
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.toDraft
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_cancel
import tallybook.shared.generated.resources.movimiento_close
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_edit_title
import tallybook.shared.generated.resources.movimiento_save_changes

class ManageMovimientoSheetTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun editorShowsPrefilledFieldsWithHeaderDeleteAndOneFooterRow() {
    val actions = ManagementSheetActions()
    composeRule.setContent { TallybookTheme { ManageMovimientoContent(editSession(), actions) } }
    composeRule.onNodeWithText("12.34").assertIsNotFocused()
    composeRule.onNodeWithText("Lunch").assertIsDisplayed()
    composeRule.onNodeWithText("Food").assertIsDisplayed()
    assertEditorActionPlacement()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_save_changes)).performClick()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
        .performClick()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_cancel)).performClick()
    composeRule.runOnIdle {
      assertEquals(1, actions.saves)
      assertEquals(1, actions.deleteRequests)
      assertEquals(1, actions.cancels)
    }
  }

  @Test
  fun editorSubmissionAndPendingDiscardDisableHeaderAndFooterActions() {
    composeRule.mainClock.autoAdvance = false
    val ready = editSession()
    var session by mutableStateOf(ready)
    composeRule.setContent {
      TallybookTheme { ManageMovimientoContent(session, ManagementSheetActions()) }
    }
    composeRule.runOnIdle { session = ready.copy(isSubmitting = true) }
    composeRule.mainClock.advanceTimeByFrame()
    assertEditorActionsDisabled()
    composeRule.runOnIdle { session = ready.copy(discardTarget = MovimientoDiscardTarget.Close) }
    composeRule.mainClock.advanceTimeByFrame()
    assertEditorActionsDisabled()
  }

  @Test
  fun editPrefillDoesNotFocusAmountAndDescriptionStartsExpanded() {
    val actions = ManagementSheetActions()
    val movimiento = managementMovimiento()
    val form =
        EditMovimientoDraftState(
            movimiento,
            movimiento.toDraft(),
            LocalDate(2026, 9, 15),
        )
    composeRule.setContent {
      TallybookTheme {
        ManageMovimientoContent(
            MovimientoManagementSession(
                1,
                movimiento.id,
                MovimientoManagementPhase.Edit,
                movimiento,
                listOf(managementCategoria()),
                edit = form,
                isCategoriesLoading = false,
            ),
            actions,
        )
      }
    }
    composeRule.onNodeWithText("12.34").assertIsNotFocused()
    composeRule.onNodeWithText("Lunch").assertIsDisplayed()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_save_changes)).performClick()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_cancel)).performClick()
    composeRule.runOnIdle {
      assertEquals(1, actions.saves)
      assertEquals(1, actions.cancels)
    }
  }

  @Test
  fun keyboardRemovesEditFooterSemanticsAndDismissalRestoresReachableActions() {
    val actions = ManagementSheetActions()
    val movimiento = managementMovimiento()
    val edit = EditMovimientoDraftState(movimiento, movimiento.toDraft(), LocalDate(2026, 9, 15))
    var imeBottom = 0
    composeRule.setContent {
      val bottom = WindowInsets.ime.getBottom(LocalDensity.current)
      SideEffect { imeBottom = bottom }
      TallybookTheme {
        ManageMovimientoContent(
            MovimientoManagementSession(
                7,
                movimiento.id,
                MovimientoManagementPhase.Edit,
                movimiento,
                listOf(managementCategoria()),
                edit = edit,
                isCategoriesLoading = false,
            ),
            actions,
        )
      }
    }
    val save = managementString(Res.string.movimiento_save_changes)
    val cancel = managementString(Res.string.movimiento_cancel)
    val amount = managementString(Res.string.movimiento_amount_expense_prompt)
    composeRule.onNodeWithContentDescription(amount).performClick()
    composeRule.waitUntil(5_000) { imeBottom > 0 }
    composeRule.onNodeWithText(save).assertDoesNotExist()
    composeRule.onNodeWithText(cancel).assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
        .assertIsDisplayed()
        .assertIsEnabled()
    composeRule.onNodeWithContentDescription(amount).performImeAction()
    composeRule.waitUntil(5_000) { imeBottom == 0 }
    composeRule.onNodeWithText(save).assertIsDisplayed().assertIsEnabled().performClick()
    composeRule.onNodeWithText(cancel).assertIsDisplayed().assertIsEnabled().performClick()
    composeRule.runOnIdle {
      assertEquals(1, actions.saves)
      assertEquals(1, actions.cancels)
    }
  }

  @Test
  fun closingRetainsTheDraftButDisablesFormAndCancelInteractions() {
    val actions = ManagementSheetActions()
    val movimiento = managementMovimiento()
    val form =
        EditMovimientoDraftState(
            movimiento,
            movimiento.toDraft(),
            LocalDate(2026, 9, 15),
        )
    var phase by androidx.compose.runtime.mutableStateOf(MovimientoManagementPhase.Edit)
    composeRule.setContent {
      TallybookTheme {
        ManageMovimientoContent(
            MovimientoManagementSession(
                7,
                movimiento.id,
                phase,
                movimiento,
                listOf(managementCategoria()),
                edit = form,
                isCategoriesLoading = false,
            ),
            actions,
        )
      }
    }
    composeRule.mainClock.autoAdvance = false
    composeRule.runOnIdle { phase = MovimientoManagementPhase.Closing }
    composeRule.mainClock.advanceTimeByFrame()
    composeRule
        .onNodeWithText(managementString(Res.string.movimiento_save_changes))
        .assertIsNotEnabled()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_cancel)).assertIsNotEnabled()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
        .assertIsNotEnabled()
  }

  @Test
  fun editorBackDispatchesTheIntentFromTheNativeModalWindow() {
    val actions = ManagementSheetActions()
    composeRule.setContent { TallybookTheme { ManageMovimientoContent(editSession(), actions) } }
    composeRule
        .onNodeWithText(managementString(Res.string.movimiento_save_changes))
        .assertIsDisplayed()
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
    composeRule.runOnIdle { assertEquals(1, actions.backs) }
  }

  @Test
  fun categoryCatalogFailureKeepsDeleteReachableAndMissingRowDisablesMutations() {
    val actions = ManagementSheetActions()
    val ready = editSession().copy(isCategoriesLoading = true, isCategoriesFailed = true)
    var session by mutableStateOf(ready)
    composeRule.setContent { TallybookTheme { ManageMovimientoContent(session, actions) } }
    val delete = managementString(Res.string.movimiento_delete)
    val save = managementString(Res.string.movimiento_save_changes)
    composeRule.onNodeWithContentDescription(delete).assertIsEnabled().performClick()
    composeRule.onNodeWithContentDescription(save).assertIsNotEnabled()
    composeRule.runOnIdle {
      assertEquals(1, actions.deleteRequests)
      session = ready.copy(error = MovimientoManagementError.MissingOnSave)
    }
    composeRule.onNodeWithContentDescription(delete).assertIsNotEnabled()
    composeRule.onNodeWithContentDescription(save).assertIsNotEnabled()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_close)).assertIsEnabled()
  }

  @Test
  fun explicitEditBackCallbackDoesNotAcceptHiddenDetent() {
    val actions = ManagementSheetActions()
    val movimiento = managementMovimiento()
    val form =
        EditMovimientoDraftState(
            movimiento,
            movimiento.toDraft(),
            LocalDate(2026, 9, 15),
        )
    composeRule.setContent {
      TallybookTheme {
        ManageMovimientoContent(
            MovimientoManagementSession(
                7,
                movimiento.id,
                MovimientoManagementPhase.Edit,
                movimiento,
                listOf(managementCategoria()),
                edit = form,
                isCategoriesLoading = false,
            ),
            actions,
        )
      }
    }
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
    composeRule.runOnIdle {
      assertEquals(1, actions.backs)
      assertEquals(0, actions.hidden)
    }
    composeRule
        .onNodeWithText(managementString(Res.string.movimiento_save_changes))
        .assertIsDisplayed()
  }

  private fun assertEditorActionPlacement() {
    val title =
        composeRule
            .onNodeWithText(managementString(Res.string.movimiento_edit_title))
            .getUnclippedBoundsInRoot()
    val delete =
        composeRule
            .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
            .getUnclippedBoundsInRoot()
    val cancel =
        composeRule
            .onNodeWithText(managementString(Res.string.movimiento_cancel))
            .getUnclippedBoundsInRoot()
    val save =
        composeRule
            .onNodeWithText(managementString(Res.string.movimiento_save_changes))
            .getUnclippedBoundsInRoot()
    assertTrue(title.right <= delete.left)
    assertTrue(title.top < delete.bottom && delete.top < title.bottom)
    assertTrue(cancel.right <= save.left)
    assertTrue(cancel.top < save.bottom && save.top < cancel.bottom)
  }

  private fun assertEditorActionsDisabled() {
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
        .assertIsNotEnabled()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_save_changes))
        .assertIsNotEnabled()
    composeRule.onNodeWithText(managementString(Res.string.movimiento_cancel)).assertIsNotEnabled()
  }
}

// The fixture implements the whole route contract while asserting only emitted presentation
// intents.
@Suppress("TooManyFunctions")
private class ManagementSheetActions : ManageMovimientoSheetActions {
  var deleteRequests = 0
  var saves = 0
  var cancels = 0
  var backs = 0
  var hidden = 0

  override fun onRequestDelete() {
    deleteRequests++
  }

  override fun onSubmit() {
    saves++
  }

  override fun onCancelEdit() {
    cancels++
  }

  override fun onBack() {
    backs++
  }

  override fun onSheetHidden(origin: Long) {
    hidden++
  }

  override fun onRequestDismiss() = false

  override fun onConfirmDelete() = Unit

  override fun onCancelDelete() = Unit

  override fun onContinueEditing() = Unit

  override fun onDiscardChanges() = Unit

  override fun onTipoChanged(tipo: TipoMovimiento) = Unit

  override fun onAmountChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onCategoriasChanged(ids: List<String>) = Unit

  override fun onCategoriaRemoved(id: String) = Unit

  override fun onTituloChanged(value: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onRetryLoad() = Unit
}

private fun managementString(resource: StringResource) = runBlocking { getString(resource) }

private fun editSession(): MovimientoManagementSession {
  val movimiento = managementMovimiento()
  return MovimientoManagementSession(
      token = 1,
      movimientoId = movimiento.id,
      phase = MovimientoManagementPhase.Edit,
      movimiento = movimiento,
      categorias = listOf(managementCategoria()),
      isCategoriesLoading = false,
      edit = EditMovimientoDraftState(movimiento, movimiento.toDraft(), LocalDate(2026, 9, 15)),
  )
}
