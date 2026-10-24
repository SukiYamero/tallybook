package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaBalance
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountConfirmation
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountDraftState
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountGroup
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSheet
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSheetError
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsActions
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsContent
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsUiState
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_archive_instead
import tallybook.shared.generated.resources.account_create
import tallybook.shared.generated.resources.account_currency_locked
import tallybook.shared.generated.resources.account_cycle_spending
import tallybook.shared.generated.resources.account_debt_estimated
import tallybook.shared.generated.resources.account_duplicate
import tallybook.shared.generated.resources.account_in_use_message
import tallybook.shared.generated.resources.account_name_label
import tallybook.shared.generated.resources.account_pay_card
import tallybook.shared.generated.resources.account_transfer
import tallybook.shared.generated.resources.account_type_bank
import tallybook.shared.generated.resources.account_type_credit_card
import tallybook.shared.generated.resources.accounts_archived_show
import tallybook.shared.generated.resources.accounts_load_error
import tallybook.shared.generated.resources.accounts_new
import tallybook.shared.generated.resources.accounts_totals
import tallybook.shared.generated.resources.retry

@OptIn(ExperimentalResourceApi::class)
class AccountsScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun groupsShowTheirTotalsAndCreditEstimatesAndRowsOpenTheirSheet() {
    val actions = RecordingActions()
    show(mutableStateOf(listed), actions)

    composeRule.onNodeWithText(text(Res.string.accounts_totals)).assertIsDisplayed()
    composeRule.onNodeWithText(Money(10_000_000, Moneda.COP).format()).assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.account_type_bank)).assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.account_type_credit_card)).assertIsDisplayed()
    composeRule
        .onNode(
            hasClickAction() and
                hasText("NU") and
                hasText(text(Res.string.account_debt_estimated)) and
                hasText(text(Res.string.account_cycle_spending))
        )
        .assertIsDisplayed()
    composeRule.onNode(hasClickAction() and hasText("Nequi")).performClick()
    composeRule.onNode(hasClickAction() and hasText("NU")).performClick()

    composeRule.runOnIdle { assertEquals(listOf("open:nequi", "open:nu"), actions.calls) }
  }

  @Test
  fun archivedAccountsStayCollapsedUntilShown() {
    val actions = RecordingActions()
    show(mutableStateOf(listed.copy(content = ready(archived = listOf(balance(VIEJA))))), actions)

    composeRule.onNodeWithText("Vieja").assertDoesNotExist()
    composeRule.onNodeWithText(text(Res.string.accounts_archived_show)).performClick()
    composeRule.runOnIdle { assertEquals(listOf("toggle-archived"), actions.calls) }
  }

  @Test
  fun aLoadFailureOffersRetry() {
    val actions = RecordingActions()
    show(mutableStateOf(AccountsUiState(content = AccountsContent.Failed)), actions)

    composeRule.onNodeWithText(text(Res.string.accounts_load_error)).assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.retry)).performClick()
    composeRule.runOnIdle { assertEquals(listOf("retry"), actions.calls) }
  }

  @Test
  fun anAccountIsCreatedFromTheSheet() {
    val state = mutableStateOf(listed)
    val actions = RecordingActions(state)
    show(state, actions)

    composeRule.onNodeWithContentDescription(text(Res.string.accounts_new)).performClick()
    composeRule.runOnIdle {
      state.value = state.value.copy(sheet = AccountSheet.forNew("", TipoCuenta.BANCO, Moneda.COP))
    }
    composeRule.onNodeWithText(text(Res.string.account_create)).assertIsNotEnabled()
    composeRule
        .onNode(hasSetTextAction() and hasContentDescription(text(Res.string.account_name_label)))
        .performTextReplacement("Daviplata")
    composeRule.onNodeWithText(text(Res.string.account_create)).assertIsEnabled().performClick()

    composeRule.runOnIdle {
      assertEquals("Daviplata", state.value.sheet?.draft?.nombre)
      assertEquals(listOf("create", "save"), actions.calls.filterNot { it == "draft" })
    }
  }

  @Test
  fun repositoryRejectionsShowOnTheirField() {
    val sheet =
        AccountSheet.forEdit(NEQUI, credit = null).let {
          it.copy(draft = it.draft.copy(nombre = "Ahorros"))
        }
    val state =
        mutableStateOf(
            listed.copy(sheet = sheet.copy(error = AccountSheetError.Duplicate(AHORROS)))
        )
    show(state, RecordingActions())

    composeRule
        .onNodeWithText(text(Res.string.account_duplicate, "Ahorros", "COP"))
        .assertIsDisplayed()
    composeRule.runOnIdle {
      state.value = state.value.copy(sheet = sheet.copy(error = AccountSheetError.CurrencyLocked))
    }
    composeRule.onNodeWithText(text(Res.string.account_currency_locked)).assertIsDisplayed()
  }

  @Test
  fun anAccountInUseOffersArchivingInstead() {
    val actions = RecordingActions()
    val sheet =
        AccountSheet.forEdit(NEQUI, credit = null)
            .copy(confirmation = AccountConfirmation.DeleteInUse(3L))
    show(mutableStateOf(listed.copy(sheet = sheet)), actions)

    composeRule
        .onNodeWithText(runBlocking { getPluralString(Res.plurals.account_in_use_message, 3, 3) })
        .assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.account_archive_instead)).performClick()
    composeRule.runOnIdle { assertEquals(listOf("confirm-archive"), actions.calls) }
  }

  @Test
  fun transferActionsAppearOnlyOnceATransferCanOpen() {
    val actions = RecordingActions()
    val state = mutableStateOf(listed.copy(sheet = AccountSheet.forEdit(NU, CREDIT)))
    val canTransfer = mutableStateOf(false)
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost {
          AccountsScreen(state.value, actions, onBack = {}, canTransfer = canTransfer.value)
        }
      }
    }

    composeRule.onNodeWithText(text(Res.string.account_transfer)).assertDoesNotExist()
    canTransfer.value = true
    composeRule.onNodeWithText(text(Res.string.account_transfer)).performClick()
    composeRule.onNodeWithText(text(Res.string.account_pay_card)).performClick()
    composeRule.runOnIdle { assertEquals(listOf("transfer", "pay-card"), actions.calls) }

    state.value = listed.copy(sheet = AccountSheet.forEdit(NEQUI, null))
    composeRule.onNodeWithText(text(Res.string.account_pay_card)).assertDoesNotExist()
  }

  private fun show(
      state: androidx.compose.runtime.MutableState<AccountsUiState>,
      actions: RecordingActions,
  ) {
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost { AccountsScreen(state.value, actions, onBack = {}) }
      }
    }
  }

  private fun text(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  @Suppress("TooManyFunctions")
  private class RecordingActions(
      private val state: androidx.compose.runtime.MutableState<AccountsUiState>? = null
  ) : AccountsActions {
    val calls = mutableListOf<String>()

    override fun onSearchChanged(query: String) {
      calls += "search:$query"
    }

    override fun onRetry() {
      calls += "retry"
    }

    override fun onToggleArchived() {
      calls += "toggle-archived"
    }

    override fun onCreate() {
      calls += "create"
    }

    override fun onOpenCuenta(id: String) {
      calls += "open:$id"
    }

    override fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState) {
      calls += "draft"
      val current = state?.value ?: return
      val sheet = current.sheet ?: return
      state.value = current.copy(sheet = sheet.copy(draft = sheet.draft.transform()))
    }

    override fun onSave() {
      calls += "save"
    }

    override fun onRequestArchive(archived: Boolean) {
      calls += "archive:$archived"
    }

    override fun onConfirmArchive() {
      calls += "confirm-archive"
    }

    override fun onRequestDelete() {
      calls += "delete"
    }

    override fun onConfirmDelete() {
      calls += "confirm-delete"
    }

    override fun onCancelConfirmation() {
      calls += "cancel"
    }

    override fun onDismissSheet(): Boolean {
      calls += "dismiss"
      return false
    }

    override fun onConfirmDiscard() {
      calls += "discard"
    }

    override fun onContinueEditing() {
      calls += "continue"
    }

    override fun onSheetHidden() {
      calls += "hidden"
    }

    override fun onTransfer() {
      calls += "transfer"
    }

    override fun onPayCard() {
      calls += "pay-card"
    }
  }

  private companion object {
    val CREATED_AT: Instant = Instant.parse("2026-01-01T00:00:00Z")

    fun cuenta(id: String, nombre: String, tipo: TipoCuenta, diaCorte: Int? = null) =
        Cuenta(id, nombre, tipo, Money(0, Moneda.COP), diaCorte, archivada = false, CREATED_AT)

    val NEQUI = cuenta("nequi", "Nequi", TipoCuenta.BANCO)
    val AHORROS = cuenta("ahorros", "Ahorros", TipoCuenta.BANCO)
    val NU = cuenta("nu", "NU", TipoCuenta.CREDITO, diaCorte = 15)
    val VIEJA = cuenta("vieja", "Vieja", TipoCuenta.BANCO).copy(archivada = true)
    val CREDIT =
        CreditEstimate(
            deuda = Money(5_000_000, Moneda.COP),
            cycleSpending = Money(1_200_000, Moneda.COP),
            cycle = MovimientoDateRange(LocalDate(2026, 10, 16), LocalDate(2026, 11, 15)),
        )

    fun balance(cuenta: Cuenta, credit: CreditEstimate? = null) =
        CuentaBalance(
            cuenta,
            Money(if (credit == null) 15_000_000 else -5_000_000, Moneda.COP),
            credit,
        )

    fun ready(archived: List<CuentaBalance> = emptyList()) =
        AccountsContent.Ready(
            totals = listOf(Money(10_000_000, Moneda.COP)),
            groups =
                listOf(
                    AccountGroup(TipoCuenta.BANCO, listOf(balance(NEQUI))),
                    AccountGroup(TipoCuenta.CREDITO, listOf(balance(NU, CREDIT))),
                ),
            archived = archived,
        )

    val listed = AccountsUiState(content = ready())
  }
}
