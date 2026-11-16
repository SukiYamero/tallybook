package com.kurobello.tallybook.feature.transfers.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.navigation.PrimaryDeviceShell
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.testing.closeSoftKeyboard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferDraftState
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferFormActions
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_transfer
import tallybook.shared.generated.resources.accounts_recent_transfers
import tallybook.shared.generated.resources.amount
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_undo
import tallybook.shared.generated.resources.transfer_create
import tallybook.shared.generated.resources.transfer_delete
import tallybook.shared.generated.resources.transfer_received_label
import tallybook.shared.generated.resources.transferencia_deleted
import tallybook.shared.generated.resources.transferencia_restored

internal class TransferFormTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun theReceivedAmountAppearsOnlyBetweenAccountsInDifferentCurrencies() {
    val actions = RecordingActions()
    val form = mutableStateOf(formFor(TransferEntry.New(NEQUI.id, CASH.id, prefill = null)))
    composeRule.setContent { TallybookTheme { TransferForm(form.value, actions, enabled = true) } }
    composeRule
        .onNodeWithText(text(Res.string.transfer_received_label), substring = true)
        .assertDoesNotExist()

    form.value = form.value.withDestino(DOLARES.id)
    composeRule
        .onNodeWithText(text(Res.string.transfer_received_label), substring = true)
        .assertExists()

    composeRule.onAllNodes(hasText("Nequi") and hasClickAction())[1].performClick()
    assertEquals(NEQUI.id, actions.destino)
  }

  @Test
  fun anInvalidSubmitFocusesTheFirstAmountWithAnError() {
    val form = mutableStateOf(formFor(TransferEntry.New(NEQUI.id, CASH.id, prefill = null)))
    composeRule.setContent {
      TallybookTheme { TransferForm(form.value, RecordingActions(), enabled = true) }
    }
    val amount = hasSetTextAction() and hasContentDescription(text(Res.string.amount))
    composeRule.onNode(amount).assertIsNotFocused()

    composeRule.runOnIdle { form.value = form.value.copy(submitAttempted = true) }

    composeRule.onNode(amount).assertIsFocused()
  }

  @Test
  fun aTransferCreatedFromAnAccountCanBeDeletedAndRestoredWithUndo() {
    // The accounts under test are in COP, so the shell's user has COP as primary currency.
    val fixture = PrimaryDeviceShell(Moneda.COP)
    fixture.cuentas.cuentas.value += NEQUI
    composeRule.setContent {
      val owner = fixture.owner.value
      KoinIsolatedContext(fixture.context) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides owner,
            LocalViewModelStoreOwner provides owner.viewModelOwner,
            LocalSavedStateRegistryOwner provides owner,
            LocalSaveableStateRegistry provides fixture.registry.value,
        ) {
          TallybookTheme { TallybookViewportOverlayHost { TallybookNavHost() } }
        }
      }
    }
    try {
      composeRule
          .onNode(hasText(Money(125_000, Moneda.COP).format()) and hasClickAction())
          .performClick()
      composeRule.onNode(hasText("Efectivo") and hasClickAction()).performClick()
      composeRule.onNodeWithText(text(Res.string.account_transfer)).performClick()

      composeRule.waitUntil {
        composeRule.onAllNodesWithText("Nequi").fetchSemanticsNodes().size == 2
      }
      composeRule.onAllNodes(hasText("Nequi") and hasClickAction())[1].performClick()
      composeRule
          .onNode(hasSetTextAction() and hasContentDescription(text(Res.string.amount)))
          .performTextInput("150")
      // The sheet footer leaves while the keyboard is open, as the user sees it.
      composeRule.closeSoftKeyboard()
      composeRule.onNodeWithText(text(Res.string.transfer_create)).performClick()

      val route = "Efectivo → Nequi"
      composeRule.waitUntil {
        composeRule.onAllNodesWithText(route).fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText(text(Res.string.accounts_recent_transfers)).assertExists()
      composeRule.onNodeWithText(route).performClick()
      composeRule.onNodeWithContentDescription(text(Res.string.transfer_delete)).performClick()
      composeRule.onNodeWithText(text(Res.string.movimiento_delete)).performClick()

      composeRule.waitUntil {
        composeRule
            .onAllNodesWithText(text(Res.string.transferencia_deleted))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeRule.onNodeWithText(route).assertDoesNotExist()
      composeRule.onNodeWithText(text(Res.string.movimiento_undo)).performClick()
      composeRule.waitUntil {
        composeRule
            .onAllNodesWithText(text(Res.string.transferencia_restored))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeRule.onNodeWithText(route).assertExists()
    } finally {
      fixture.context.close()
    }
  }

  private fun formFor(entry: TransferEntry.New) =
      TransferDraftState.forNew(listOf(CASH, NEQUI, DOLARES), Moneda.COP, entry, TODAY)

  private fun text(resource: StringResource): String = runBlocking { getString(resource) }
}

private class RecordingActions : TransferFormActions {
  var destino: String? = null

  override fun onOrigenSelected(id: String) = Unit

  override fun onDestinoSelected(id: String) {
    destino = id
  }

  override fun onMontoOrigenChanged(value: String) = Unit

  override fun onMontoDestinoChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onDescripcionChanged(value: String) = Unit
}

private val TODAY = LocalDate(2026, 10, 5)
private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

private fun cuenta(id: String, nombre: String, tipo: TipoCuenta, moneda: Moneda) =
    Cuenta(
        id,
        nombre,
        tipo,
        Money(0, moneda),
        null,
        archivada = false,
        CREATED_AT,
        color = tipo.defaultColor,
    )

private val CASH =
    cuenta(Cuenta.defaultCashId(Moneda.COP), "Efectivo", TipoCuenta.EFECTIVO, Moneda.COP)
private val NEQUI = cuenta("nequi", "Nequi", TipoCuenta.BANCO, Moneda.COP)
private val DOLARES = cuenta("dolares", "Dólares", TipoCuenta.BANCO, Moneda.USD)
