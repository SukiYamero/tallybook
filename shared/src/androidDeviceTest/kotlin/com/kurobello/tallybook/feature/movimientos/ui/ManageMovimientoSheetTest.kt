package com.kurobello.tallybook.feature.movimientos.ui

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.testing.awaitSoftKeyboard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoDraftState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoSheetActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoDiscardTarget
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementPhase
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementSession
import com.kurobello.tallybook.feature.movimientos.viewmodel.USD_CASH
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementCategoria
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.toDraft
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
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_cancel
import tallybook.shared.generated.resources.movimiento_close
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_edit_title
import tallybook.shared.generated.resources.movimiento_save_changes
import tallybook.shared.generated.resources.tag_add_description
import tallybook.shared.generated.resources.tag_remove

class ManageMovimientoSheetTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun editorShowsPrefilledFieldsWithHeaderDeleteAndOneFooterRow() {
    val actions = ManagementSheetActions()
    composeRule.setContent { TallybookTheme { ManageMovimientoContent(editSession(), actions) } }
    composeRule.onNodeWithText("12.34").assertIsNotFocused()
    composeRule.onNodeWithText("Lunch").performScrollTo().assertIsDisplayed()
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
  fun editorKeepsItsCategoryAndShowsOnlyTheTagsTheCatalogStillHas() {
    val actions = ManagementSheetActions()
    val trip = Etiqueta("trip", "Viaje Chile", Instant.parse("2026-01-01T00:00:00Z"))
    val session = editSession(etiquetaIds = listOf("deleted", trip.id))
    composeRule.setContent {
      TallybookTheme {
        ManageMovimientoContent(
            session.copy(etiquetas = listOf(trip), etiquetasLoad = EtiquetasLoad.READY),
            actions,
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(managementString(Res.string.category_tag_remove, "Food"))
        .assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.category_picker_single_title))
        .assertExists()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.tag_remove, trip.nombre))
        .performScrollTo()
        .performClick()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.tag_add_description))
        .assertExists()
    composeRule.onNodeWithText("deleted").assertDoesNotExist()
    composeRule.runOnIdle { assertEquals(listOf(trip.id), actions.removedEtiquetas) }
  }

  @Test
  fun editorSubmissionAndPendingDiscardDisableHeaderAndFooterActions() {
    val ready = editSession()
    var session by mutableStateOf(ready)
    composeRule.setContent {
      TallybookTheme { ManageMovimientoContent(session, ManagementSheetActions()) }
    }
    // The sheet composes its content over its opening frames; only the state changes are frozen.
    composeRule.waitForIdle()
    composeRule.mainClock.autoAdvance = false
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
                cuentas = listOf(USD_CASH),
                isCuentasLoading = false,
            ),
            actions,
        )
      }
    }
    composeRule.onNodeWithText("12.34").assertIsNotFocused()
    composeRule.onNodeWithText("Lunch").performScrollTo().assertIsDisplayed()
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
    composeRule.setContent {
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
                cuentas = listOf(USD_CASH),
                isCuentasLoading = false,
            ),
            actions,
        )
      }
    }
    val save = managementString(Res.string.movimiento_save_changes)
    val cancel = managementString(Res.string.movimiento_cancel)
    val amount = managementString(Res.string.movimiento_amount_expense_prompt)
    composeRule.onNodeWithContentDescription(amount).performScrollTo().performClick()
    composeRule.awaitSoftKeyboard(visible = true)
    composeRule.onNodeWithText(save).assertDoesNotExist()
    composeRule.onNodeWithText(cancel).assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(managementString(Res.string.movimiento_delete))
        .assertIsDisplayed()
        .assertIsEnabled()
    composeRule.onNodeWithContentDescription(amount).performImeAction()
    composeRule.awaitSoftKeyboard(visible = false)
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
                cuentas = listOf(USD_CASH),
                isCuentasLoading = false,
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
                cuentas = listOf(USD_CASH),
                isCuentasLoading = false,
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
  val removedEtiquetas = mutableListOf<String>()

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

  override fun onCategoriaSelected(id: String) = Unit

  override fun onEtiquetasChanged(ids: List<String>) = Unit

  override fun onEtiquetaRemoved(id: String) {
    removedEtiquetas += id
  }

  override fun onCreateEtiqueta(nombre: String) = Unit

  override fun onRetryEtiquetas() = Unit

  override fun onTituloChanged(value: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onRetryLoad() = Unit

  override fun onMedioSelected(medio: MedioPago) = Unit

  override fun onCuentaSelected(id: String) = Unit
}

private fun managementString(resource: StringResource, vararg args: Any) = runBlocking {
  getString(resource, *args)
}

private fun editSession(etiquetaIds: List<String> = emptyList()): MovimientoManagementSession {
  val movimiento = managementMovimiento().copy(etiquetaIds = etiquetaIds)
  return MovimientoManagementSession(
      token = 1,
      movimientoId = movimiento.id,
      phase = MovimientoManagementPhase.Edit,
      movimiento = movimiento,
      categorias = listOf(managementCategoria()),
      isCategoriesLoading = false,
      edit = EditMovimientoDraftState(movimiento, movimiento.toDraft(), LocalDate(2026, 9, 15)),
      cuentas = listOf(USD_CASH),
      isCuentasLoading = false,
  )
}
